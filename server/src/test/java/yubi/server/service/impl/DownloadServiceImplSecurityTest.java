package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import yubi.core.base.consts.DownloadOwnerType;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.entity.Download;
import yubi.core.entity.User;
import yubi.core.mappers.ext.DownloadMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.service.DownloadFileResolver;
import yubi.server.service.DownloadFileResource;
import yubi.server.service.DownloadFileTarget;
import yubi.server.service.DownloadTaskScheduler;
import yubi.server.service.SharedDownloadContext;

import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DownloadServiceImplSecurityTest {

    private DownloadMapperExt mapper;
    private YuBiSecurityManager securityManager;
    private DownloadFileResolver fileResolver;
    private DownloadTaskScheduler scheduler;
    private DownloadServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        mapper = mock(DownloadMapperExt.class);
        securityManager = mock(YuBiSecurityManager.class);
        fileResolver = mock(DownloadFileResolver.class);
        scheduler = mock(DownloadTaskScheduler.class);
        service = new DownloadServiceImpl(mapper, securityManager, fileResolver, scheduler);

        user = new User();
        user.setId("user-1");
        user.setUsername("owner");
        when(securityManager.getCurrentUser()).thenReturn(user);
    }

    @Test
    void shouldCreateAndListAuthenticatedTasksOnlyForCurrentUser() {
        DownloadTaskDTO created = service.submitDownloadTask(downloadParam("orders"));

        ArgumentCaptor<Download> inserted = ArgumentCaptor.forClass(Download.class);
        verify(mapper).insert(inserted.capture());
        assertEquals(DownloadOwnerType.AUTHENTICATED.name(), inserted.getValue().getOwnerType());
        assertEquals("user-1", inserted.getValue().getOwnerId());
        assertEquals("user-1", inserted.getValue().getCreateBy());
        assertEquals(null, inserted.getValue().getShareId());
        assertEquals(inserted.getValue().getId(), created.id());
        verify(scheduler).submit(any());

        Download ownTask = task("task-1", "download/task-1/orders.xlsx", (byte) 1);
        when(mapper.selectAuthenticatedTasks(eq("user-1"), any())).thenReturn(List.of(ownTask));
        assertEquals(List.of(DownloadTaskDTO.from(ownTask)), service.listDownloadTasks());
    }

    @Test
    void shouldRejectAnonymousAuthenticatedUseCasesWithoutTouchingPersistenceOrFiles() {
        when(securityManager.getCurrentUser()).thenReturn(null);

        assertThrows(NotAllowedException.class, () -> service.submitDownloadTask(downloadParam("orders")));
        assertThrows(NotAllowedException.class, service::listDownloadTasks);
        assertThrows(NotAllowedException.class, () -> service.downloadFile("task-1"));

        verifyNoInteractions(mapper, fileResolver, scheduler);
    }

    @Test
    void shouldReadOwnFileAndRejectCrossUserBeforeResolvingPath() {
        Download ownTask = task("task-1", "download/task-1/orders.xlsx", (byte) 1);
        DownloadFileResource expected = resource("orders.xlsx");
        when(mapper.selectAuthenticatedTask("task-1", "user-1")).thenReturn(ownTask);
        when(mapper.markAuthenticatedTaskDownloaded(eq("task-1"), eq("user-1"), any())).thenReturn(1);
        when(fileResolver.openForRead("download/task-1/orders.xlsx")).thenReturn(expected);

        assertSame(expected, service.downloadFile("task-1"));

        reset(fileResolver);
        when(mapper.selectAuthenticatedTask("other-task", "user-1")).thenReturn(null);
        assertThrows(NotAllowedException.class, () -> service.downloadFile("other-task"));
        verifyNoInteractions(fileResolver);
    }

    @Test
    void shouldScopeSharedCreateListAndFileReadByShareAndSessionDigest() {
        SharedDownloadContext context = new SharedDownloadContext(
                "share-1", "a".repeat(64), "share-owner", "user-1", "user-1", "org-1"
        );

        service.submitSharedDownloadTask(downloadParam("shared"), context);
        ArgumentCaptor<Download> inserted = ArgumentCaptor.forClass(Download.class);
        verify(mapper).insert(inserted.capture());
        assertEquals(DownloadOwnerType.SHARE.name(), inserted.getValue().getOwnerType());
        assertEquals("share-1", inserted.getValue().getShareId());
        assertEquals("a".repeat(64), inserted.getValue().getOwnerId());
        assertEquals("a".repeat(64).length(), inserted.getValue().getOwnerId().length());

        Download ownTask = task("shared-task", "download/shared-task/shared.xlsx", (byte) 1);
        when(mapper.selectSharedTasks(eq("share-1"), eq("a".repeat(64)), any()))
                .thenReturn(List.of(ownTask));
        assertEquals(1, service.listSharedDownloadTasks(context).size());

        DownloadFileResource resource = resource("shared.xlsx");
        when(mapper.selectSharedTask("shared-task", "share-1", "a".repeat(64))).thenReturn(ownTask);
        when(mapper.markSharedTaskDownloaded(
                eq("shared-task"), eq("share-1"), eq("a".repeat(64)), any()
        )).thenReturn(1);
        when(fileResolver.openForRead("download/shared-task/shared.xlsx")).thenReturn(resource);
        assertSame(resource, service.downloadSharedFile("shared-task", context));
    }

    @Test
    void shouldRejectMigratedLegacyTaskBeforeResolvingFile() {
        when(mapper.selectAuthenticatedTask("legacy-task", "user-1")).thenReturn(null);

        assertThrows(NotAllowedException.class, () -> service.downloadFile("legacy-task"));

        verifyNoInteractions(fileResolver);
        verify(mapper, never()).markAuthenticatedTaskDownloaded(any(), any(), any());
    }

    @Test
    void shouldRejectCrossShareAndCrossSessionBeforeResolvingFile() {
        SharedDownloadContext crossShare = new SharedDownloadContext(
                "share-2", "a".repeat(64), null, "user-1", "user-1", "org-1"
        );
        SharedDownloadContext crossSession = new SharedDownloadContext(
                "share-1", "b".repeat(64), null, "user-1", "user-1", "org-1"
        );
        when(mapper.selectSharedTask("task-1", "share-2", "a".repeat(64))).thenReturn(null);
        when(mapper.selectSharedTask("task-1", "share-1", "b".repeat(64))).thenReturn(null);

        assertThrows(NotAllowedException.class, () -> service.downloadSharedFile("task-1", crossShare));
        assertThrows(NotAllowedException.class, () -> service.downloadSharedFile("task-1", crossSession));

        verifyNoInteractions(fileResolver);
    }

    @Test
    void shouldRejectMissingSharedTaskBeforeResolvingFile() {
        SharedDownloadContext context = new SharedDownloadContext(
                "share-1", "a".repeat(64), null, "user-1", "user-1", "org-1"
        );
        when(mapper.selectSharedTask("missing-task", "share-1", "a".repeat(64)))
                .thenReturn(null);

        assertThrows(
                NotAllowedException.class,
                () -> service.downloadSharedFile("missing-task", context)
        );

        verifyNoInteractions(fileResolver);
        verify(mapper, never()).markSharedTaskDownloaded(any(), any(), any(), any());
    }

    @Test
    void shouldRejectNonDigestSharedOwnerBeforePersistence() {
        SharedDownloadContext rawSession = new SharedDownloadContext(
                "share-1", "raw-session-token", null, "user-1", "user-1", "org-1"
        );
        SharedDownloadContext uppercaseDigest = new SharedDownloadContext(
                "share-1", "A".repeat(64), null, "user-1", "user-1", "org-1"
        );

        assertThrows(NotAllowedException.class, () -> service.listSharedDownloadTasks(rawSession));
        assertThrows(NotAllowedException.class, () -> service.listSharedDownloadTasks(uppercaseDigest));

        verifyNoInteractions(mapper, fileResolver, scheduler);
    }

    @Test
    void shouldRejectSharedCreationWithoutTrustedQuerySubject() {
        SharedDownloadContext missingSubject = new SharedDownloadContext(
                "share-1", "a".repeat(64), "share-owner", "user-1", null, "org-1"
        );

        assertThrows(
                NotAllowedException.class,
                () -> service.submitSharedDownloadTask(downloadParam("shared"), missingSubject)
        );

        verifyNoInteractions(mapper, fileResolver, scheduler);
    }

    @Test
    void shouldPersistOnlyLimitedFailureCodeWhenGeneratorThrowsSensitiveException() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(scheduler).submit(any());
        Path taskDirectory = Path.of("/safe/task-id");
        when(fileResolver.createTaskDirectory(any())).thenReturn(taskDirectory);
        when(fileResolver.createTarget(taskDirectory, "template.yubi"))
                .thenReturn(new DownloadFileTarget(
                        new java.io.ByteArrayOutputStream(),
                        "download/task-id/template.yubi",
                        "template.yubi"
                ));

        DownloadTaskDTO result = service.submitGeneratedTask("template.yubi", target -> {
            throw new IllegalStateException("jdbc:mysql://secret/db?password=raw-secret /internal/path");
        });

        ArgumentCaptor<Download> updates = ArgumentCaptor.forClass(Download.class);
        verify(mapper).updateByPrimaryKeySelective(updates.capture());
        assertEquals((byte) -1, updates.getValue().getStatus());
        assertEquals("INTERNAL_FAILURE", updates.getValue().getFailureCode());
        assertEquals(null, updates.getValue().getPath());
        assertEquals((byte) -1, result.status());
        assertTrue(result.toString().contains("template.yubi"));
        assertTrue(!result.toString().contains("raw-secret"));
    }

    @Test
    void shouldNotInvokeGeneratorWhenExclusiveTargetCannotBeCreated() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(scheduler).submit(any());
        Path taskDirectory = Path.of("/safe/task-id");
        when(fileResolver.createTaskDirectory(any())).thenReturn(taskDirectory);
        when(fileResolver.createTarget(taskDirectory, "template.yubi"))
                .thenThrow(new NotAllowedException("下载文件不可用"));
        AtomicBoolean generatorInvoked = new AtomicBoolean();

        DownloadTaskDTO result = service.submitGeneratedTask(
                "template.yubi",
                target -> generatorInvoked.set(true)
        );

        assertFalse(generatorInvoked.get());
        assertEquals((byte) -1, result.status());
    }

    @Test
    void shouldRejectUnfinishedTaskBeforeResolvingPath() {
        when(mapper.selectAuthenticatedTask("task-1", "user-1"))
                .thenReturn(task("task-1", null, (byte) 0));

        assertThrows(NotAllowedException.class, () -> service.downloadFile("task-1"));

        verify(mapper, never()).markAuthenticatedTaskDownloaded(any(), any(), any());
        verifyNoInteractions(fileResolver);
    }

    @Test
    void shouldReplacePersistenceExceptionWithSafeDownloadBoundaryError() {
        when(mapper.selectAuthenticatedTasks(eq("user-1"), any()))
                .thenThrow(new IllegalStateException(
                        "select * from source where password='raw-secret' and path='/internal/path'"
                ));

        NotAllowedException exception = assertThrows(
                NotAllowedException.class,
                service::listDownloadTasks
        );

        assertEquals("下载任务不可用", exception.getMessage());
        assertTrue(!exception.getMessage().contains("raw-secret"));
        assertTrue(!exception.getMessage().contains("/internal/path"));
    }

    @Test
    void shouldCloseOpenedHandleWhenOwnershipStateUpdateFails() throws Exception {
        Download ownTask = task("task-1", "download/task-1/orders.xlsx", (byte) 1);
        InputStream inputStream = mock(InputStream.class);
        DownloadFileResource resource = new DownloadFileResource(inputStream, "orders.xlsx");
        when(mapper.selectAuthenticatedTask("task-1", "user-1")).thenReturn(ownTask);
        when(fileResolver.openForRead("download/task-1/orders.xlsx")).thenReturn(resource);
        when(mapper.markAuthenticatedTaskDownloaded(eq("task-1"), eq("user-1"), any()))
                .thenReturn(0);

        assertThrows(NotAllowedException.class, () -> service.downloadFile("task-1"));

        verify(inputStream).close();
    }

    private DownloadCreateParam downloadParam(String fileName) {
        DownloadQueryRequest query = new DownloadQueryRequest();
        query.setViewId("view-1");
        DownloadCreateParam param = new DownloadCreateParam();
        param.setFileName(fileName);
        param.setDownloadParams(List.of(query));
        return param;
    }

    private Download task(String id, String path, byte status) {
        Download download = new Download();
        download.setId(id);
        download.setName("orders.xlsx");
        download.setPath(path);
        download.setStatus(status);
        return download;
    }

    private DownloadFileResource resource(String fileName) {
        return new DownloadFileResource(
                new ByteArrayInputStream("content".getBytes()),
                fileName
        );
    }
}

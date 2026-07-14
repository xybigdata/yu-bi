package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.base.consts.AttachmentType;
import yubi.core.common.Application;
import yubi.core.entity.Download;
import yubi.core.entity.User;
import yubi.core.mappers.ext.DownloadMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.service.DownloadFileResolver;
import yubi.server.service.DownloadFileResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadTaskPhysicalIsolationTest {

    @TempDir
    Path tempDir;

    private DownloadMapperExt mapper;
    private AtomicReference<User> currentUser;
    private DownloadServiceImpl service;

    @BeforeEach
    void setUp() {
        ApplicationContext context = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.env.file-path")).thenReturn(tempDir.toString());
        new Application().setApplicationContext(context);

        mapper = mock(DownloadMapperExt.class);
        YuBiSecurityManager securityManager = mock(YuBiSecurityManager.class);
        currentUser = new AtomicReference<>(user("user-1", "owner-one"));
        when(securityManager.getCurrentUser()).thenAnswer(ignored -> currentUser.get());
        service = new DownloadServiceImpl(
                mapper,
                securityManager,
                new DownloadFileResolver(),
                Runnable::run
        );
    }

    @Test
    void shouldIsolateSamePhysicalFileNameAcrossOwnersAndEnforceOwnerRead() throws Exception {
        DownloadTaskDTO first = service.submitGeneratedTask(
                "same-name.yubi",
                target -> target.write("first-owner-content".getBytes(StandardCharsets.UTF_8))
        );
        currentUser.set(user("user-2", "owner-two"));
        DownloadTaskDTO second = service.submitGeneratedTask(
                "same-name.yubi",
                target -> target.write("second-owner-content".getBytes(StandardCharsets.UTF_8))
        );

        ArgumentCaptor<Download> inserted = ArgumentCaptor.forClass(Download.class);
        verify(mapper, times(2)).insert(inserted.capture());
        Map<String, Download> tasksByOwner = new HashMap<>();
        inserted.getAllValues().forEach(task -> tasksByOwner.put(task.getOwnerId(), task));
        Download firstTask = tasksByOwner.get("user-1");
        Download secondTask = tasksByOwner.get("user-2");

        assertNotEquals(first.id(), second.id());
        assertNotEquals(firstTask.getPath(), secondTask.getPath());
        assertEquals("download/" + first.id() + "/same-name.yubi", firstTask.getPath());
        assertEquals("download/" + second.id() + "/same-name.yubi", secondTask.getPath());

        DownloadFileResolver resolver = new DownloadFileResolver();
        assertEquals(
                Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE
                ),
                Files.getPosixFilePermissions(resolver.downloadRoot()
                        .resolve(first.id())
                        .resolve("same-name.yubi"))
        );
        assertEquals("first-owner-content", read(resolver.openForRead(firstTask.getPath())));
        assertEquals("second-owner-content", read(resolver.openForRead(secondTask.getPath())));

        when(mapper.selectAuthenticatedTask(first.id(), "user-1")).thenReturn(firstTask);
        when(mapper.markAuthenticatedTaskDownloaded(eq(first.id()), eq("user-1"), any())).thenReturn(1);
        currentUser.set(user("user-1", "owner-one"));
        assertEquals("first-owner-content", read(service.downloadFile(first.id())));
        assertThrows(NotAllowedException.class, () -> service.downloadFile(second.id()));

        when(mapper.selectAuthenticatedTask(second.id(), "user-2")).thenReturn(secondTask);
        when(mapper.markAuthenticatedTaskDownloaded(eq(second.id()), eq("user-2"), any())).thenReturn(1);
        currentUser.set(user("user-2", "owner-two"));
        assertEquals("second-owner-content", read(service.downloadFile(second.id())));
        assertThrows(NotAllowedException.class, () -> service.downloadFile(first.id()));
    }

    @Test
    void shouldCopyAttachmentProducedInTrustedAbsoluteStagingDirectory() throws Exception {
        ApplicationContext context = Application.getContext();
        AttachmentExcelServiceImpl attachment = mock(AttachmentExcelServiceImpl.class);
        AtomicReference<Path> stagingDirectory = new AtomicReference<>();
        when(context.getBean(AttachmentExcelServiceImpl.class)).thenReturn(attachment);
        when(attachment.getFile(any(), any(String.class), any(String.class), any(), any())).thenAnswer(invocation -> {
            Path staging = Path.of(invocation.getArgument(1, String.class));
            assertTrue(staging.isAbsolute());
            assertNotEquals(tempDir.resolve("download").toAbsolutePath(), staging);
            stagingDirectory.set(staging);
            Path generated = Files.writeString(staging.resolve("orders.xlsx"), "attachment-content");
            return generated.toFile();
        });

        DownloadCreateParam param = new DownloadCreateParam();
        param.setDownloadType(AttachmentType.EXCEL);
        param.setFileName("orders");
        param.setDownloadParams(java.util.List.of(new DownloadQueryRequest()));
        DownloadTaskDTO task = service.submitDownloadTask(param);

        ArgumentCaptor<Download> inserted = ArgumentCaptor.forClass(Download.class);
        verify(mapper).insert(inserted.capture());
        Download persisted = inserted.getValue();
        assertEquals((byte) 1, persisted.getStatus());
        assertEquals("download/" + task.id() + "/orders.xlsx", persisted.getPath());
        assertEquals("attachment-content", Files.readString(
                new DownloadFileResolver().downloadRoot().resolve(task.id()).resolve("orders.xlsx")
        ));
        assertFalse(Files.exists(stagingDirectory.get()));
    }

    private User user(String id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private String read(DownloadFileResource resource) throws Exception {
        try (resource) {
            return new String(resource.inputStream().readAllBytes());
        }
    }
}

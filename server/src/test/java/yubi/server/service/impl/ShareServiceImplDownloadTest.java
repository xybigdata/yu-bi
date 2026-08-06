package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.Environment;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.base.consts.ShareRowPermissionBy;
import yubi.core.common.Application;
import yubi.core.common.MessageResolver;
import yubi.core.entity.Share;
import yubi.core.entity.User;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.ResourceType;
import yubi.security.manager.YuBiSecurityManager;
import yubi.security.util.AESUtil;
import yubi.server.artifact.ArtifactAccess;
import yubi.server.artifact.ArtifactContent;
import yubi.server.artifact.ArtifactDescriptor;
import yubi.server.artifact.ArtifactProducer;
import yubi.server.artifact.ArtifactTaskException;
import yubi.server.artifact.ArtifactTaskState;
import yubi.server.artifact.ArtifactTasks;
import yubi.server.artifact.ArtifactWorkContext;
import yubi.server.artifact.TaskBatch;
import yubi.server.artifact.TaskHandle;
import yubi.server.artifact.TaskPage;
import yubi.server.artifact.TaskView;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.base.params.ShareAuthorizedToken;
import yubi.server.base.params.ShareDownloadParam;
import yubi.server.base.params.ShareToken;
import yubi.server.service.AttachmentService;
import yubi.server.service.DataProviderService;
import yubi.server.service.RoleService;
import yubi.server.service.VizService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShareServiceImplDownloadTest {

    private static final String TOKEN_SECRET = "0123456789abcdef0123456789abcdef";

    private ShareMapperExt shareMapper;

    private UserMapperExt userMapper;

    private ArtifactTasks artifactTasks;

    private YuBiSecurityManager securityManager;

    @TempDir
    Path tempDir;

    private ShareServiceImpl shareService;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.security.token.secret"))
                .thenReturn(TOKEN_SECRET);
        when(environment.getProperty("yubi.env.file-path")).thenReturn(tempDir.toString());
        new Application().setApplicationContext(applicationContext);

        MessageSource messageSource = new StaticMessageSource();
        new MessageResolver().setMessageSource(messageSource);

        shareMapper = mock(ShareMapperExt.class);
        userMapper = mock(UserMapperExt.class);
        artifactTasks = mock(ArtifactTasks.class);
        securityManager = mock(YuBiSecurityManager.class);
        shareService = new ShareServiceImpl(mock(DataProviderService.class), mock(VizService.class),
                shareMapper, mock(RoleService.class), userMapper, artifactTasks);
        shareService.setSecurityManager(securityManager);
    }

    @Test
    void 合法分享凭据创建产物并绑定客户端与分享范围() {
        Share share = codeShare("share-1", "share-owner-id", "correct-password");
        User owner = new User();
        owner.setId("share-owner-id");
        owner.setUsername("share-owner");
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(share);
        when(userMapper.selectByPrimaryKey("share-owner-id")).thenReturn(owner);

        DownloadQueryRequest request = new DownloadQueryRequest();
        request.setViewId("view-1");
        request.setConcurrencyControlMode("DIRTYREAD");
        ShareDownloadParam param = new ShareDownloadParam();
        param.setShareToken("share-1");
        param.setFileName("orders.xlsx");
        param.setDownloadParams(List.of(request));
        param.setExecuteToken(Map.of("view-1", tokenFor("view-1")));
        TaskHandle expected = new TaskHandle("task-1",
                new ArtifactDescriptor("orders.xlsx", "application/octet-stream", ".xlsx"),
                ArtifactTaskState.QUEUED, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "trace-1");
        when(artifactTasks.submit(any(), any(), any())).thenReturn(expected);

        TaskHandle result = shareService.createDownload("client-1", "correct-password", param);

        assertSame(expected, result);
        ArgumentCaptor<ArtifactAccess> access = ArgumentCaptor.forClass(ArtifactAccess.class);
        verify(artifactTasks).submit(access.capture(), any(ArtifactDescriptor.class), any(ArtifactProducer.class));
        assertEquals(
                ArtifactAccess.shared("client-1", "share-1", "share-owner"), access.getValue());
    }

    @Test
    void 查询和下载必须重新验证相同的分享客户端与密码() throws Exception {
        Share share1 = codeShare("share-1", "share-owner-id", "correct-password");
        Share share2 = codeShare("share-2", "share-owner-id", "correct-password");
        User owner = new User();
        owner.setId("share-owner-id");
        owner.setUsername("share-owner");
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(share1);
        when(shareMapper.selectByPrimaryKey("share-2")).thenReturn(share2);
        when(userMapper.selectByPrimaryKey("share-owner-id")).thenReturn(owner);

        ArtifactAccess taskOwner = ArtifactAccess.shared("client-1", "share-1", "share-owner");
        TaskView task = new TaskView("task-1",
                new ArtifactDescriptor("orders", "text/plain", ".txt"),
                ArtifactTaskState.READY, Instant.EPOCH, Instant.EPOCH.plusSeconds(60),
                Instant.EPOCH.plusSeconds(1), null, "trace-1");
        ArtifactTasks ownedTasks = ownedTask(taskOwner, task, "artifact-content");
        shareService = new ShareServiceImpl(mock(DataProviderService.class), mock(VizService.class),
                shareMapper, mock(RoleService.class), userMapper, ownedTasks);

        assertEquals("task-1", shareService.getArtifactTask(
                "share-1", "client-1", "correct-password", "task-1").id());
        try (ArtifactContent content = shareService.openArtifact(
                "share-1", "client-1", "correct-password", "task-1")) {
            assertEquals("artifact-content", new String(content.stream().readAllBytes()));
        }

        assertNotFound(() -> shareService.getArtifactTask(
                "share-1", "client-2", "correct-password", "task-1"));
        assertNotFound(() -> shareService.getArtifactTask(
                "share-2", "client-1", "correct-password", "task-1"));
        assertNotFound(() -> shareService.getArtifactTask(
                "share-1", "client-1", "wrong-password", "task-1"));
        assertNotFound(() -> shareService.getArtifactTask(
                "share-1", "client-1", "correct-password", "unknown-task"));
    }

    @Test
    void 有效executeToken不能替代页面shareId创建任务() {
        ShareDownloadParam param = downloadParam();
        param.setShareToken(tokenFor("view-1").getAuthorizedToken());

        assertNotFound(() -> shareService.createDownload("client-1", "correct-password", param));
    }

    @Test
    void producer复制临时附件并在结束后删除文件释放身份() throws Exception {
        Share share = codeShare("share-1", "share-owner-id", "correct-password");
        User owner = new User();
        owner.setId("share-owner-id");
        owner.setUsername("share-owner");
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(share);
        when(userMapper.selectByPrimaryKey("share-owner-id")).thenReturn(owner);
        Path temporaryFile = tempDir.resolve("temporary.xlsx");
        Files.writeString(temporaryFile, "artifact-content");
        AttachmentService attachmentService = mock(AttachmentService.class);
        when(attachmentService.getFile(any(), any(), any())).thenReturn(temporaryFile.toFile());
        shareService = new ShareServiceImpl(mock(DataProviderService.class), mock(VizService.class),
                shareMapper, mock(RoleService.class), userMapper, artifactTasks,
                ignored -> attachmentService);
        shareService.setSecurityManager(securityManager);
        TaskHandle expected = new TaskHandle("task-1",
                new ArtifactDescriptor("orders", "application/octet-stream", ".xlsx"),
                ArtifactTaskState.QUEUED, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "trace-1");
        when(artifactTasks.submit(any(), any(), any())).thenReturn(expected);
        ShareDownloadParam param = downloadParam();
        param.setShareToken("share-1");

        shareService.createDownload("client-1", "correct-password", param);

        ArgumentCaptor<ArtifactProducer> producer = ArgumentCaptor.forClass(ArtifactProducer.class);
        verify(artifactTasks).submit(any(), any(), producer.capture());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        producer.getValue().produce(new ArtifactWorkContext(
                output, Instant.EPOCH.plusSeconds(60), "share-owner"));
        assertEquals("artifact-content", output.toString(StandardCharsets.UTF_8));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(temporaryFile));
        verify(securityManager).runAs("share-owner");
        verify(securityManager).releaseRunAs();
    }

    @Test
    void producer每次执行前重新校验eexecuteToken() throws Exception {
        Share share = codeShare("share-1", "share-owner-id", "correct-password");
        User owner = new User();
        owner.setId("share-owner-id");
        owner.setUsername("share-owner");
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(share);
        when(userMapper.selectByPrimaryKey("share-owner-id")).thenReturn(owner);
        AttachmentService attachmentService = mock(AttachmentService.class);
        shareService = new ShareServiceImpl(mock(DataProviderService.class), mock(VizService.class),
                shareMapper, mock(RoleService.class), userMapper, artifactTasks,
                ignored -> attachmentService);
        shareService.setSecurityManager(securityManager);
        when(artifactTasks.submit(any(), any(), any())).thenReturn(new TaskHandle(
                "task-1",
                new ArtifactDescriptor("orders", "application/octet-stream", ".xlsx"),
                ArtifactTaskState.QUEUED, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "trace-1"));
        ShareDownloadParam param = downloadParam();
        param.setShareToken("share-1");
        shareService.createDownload("client-1", "correct-password", param);
        ArgumentCaptor<ArtifactProducer> producer = ArgumentCaptor.forClass(ArtifactProducer.class);
        verify(artifactTasks).submit(any(), any(), producer.capture());
        param.setExecuteToken(Map.of("view-1", tokenFor("view-2")));

        assertThrows(RuntimeException.class, () -> producer.getValue().produce(
                new ArtifactWorkContext(
                        new ByteArrayOutputStream(),
                        Instant.EPOCH.plusSeconds(60),
                        "share-owner"
                )
        ));

        verify(attachmentService, never()).getFile(any(), any(), any());
    }

    @Test
    void 登录访客分享鉴权后释放临时身份() {
        Share share = codeShare("share-1", "share-owner-id", null);
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        share.setRowPermissionBy(ShareRowPermissionBy.VISITOR.name());
        share.setOrgId("org-1");
        User visitor = new User();
        visitor.setId("visitor-id");
        visitor.setUsername("visitor");
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(share);
        when(securityManager.getCurrentUser()).thenReturn(visitor);
        when(securityManager.isOrgOwner("org-1")).thenReturn(true);
        TaskHandle expected = new TaskHandle("task-1",
                new ArtifactDescriptor("orders", "application/octet-stream", ".xlsx"),
                ArtifactTaskState.QUEUED, Instant.EPOCH, Instant.EPOCH.plusSeconds(60), "trace-1");
        when(artifactTasks.submit(any(), any(), any())).thenReturn(expected);
        ShareDownloadParam param = downloadParam();
        param.setShareToken("share-1");

        assertSame(expected, shareService.createDownload("client-1", null, param));

        verify(securityManager).runAs("visitor");
        verify(securityManager).releaseRunAs();
        ArgumentCaptor<ArtifactAccess> access = ArgumentCaptor.forClass(ArtifactAccess.class);
        verify(artifactTasks).submit(access.capture(), any(), any());
        assertEquals(ArtifactAccess.shared("client-1", "share-1", "visitor"), access.getValue());
    }

    @Test
    void 登录创建者权限分享仍按实际访问用户隔离任务() {
        Share share = codeShare("share-1", "share-owner-id", null);
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        User owner = new User();
        owner.setId("share-owner-id");
        owner.setUsername("share-owner");
        User visitorA = new User();
        visitorA.setId("visitor-a-id");
        visitorA.setUsername("visitor-a");
        User visitorB = new User();
        visitorB.setId("visitor-b-id");
        visitorB.setUsername("visitor-b");
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(share);
        when(userMapper.selectByPrimaryKey("share-owner-id")).thenReturn(owner);
        when(securityManager.getCurrentUser()).thenReturn(visitorA, visitorB);
        when(artifactTasks.submit(any(), any(), any())).thenReturn(new TaskHandle(
                "task-1",
                new ArtifactDescriptor("orders", "application/octet-stream", ".xlsx"),
                ArtifactTaskState.QUEUED,
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(60),
                "trace-1"
        ));
        ShareDownloadParam first = downloadParam();
        first.setShareToken("share-1");
        ShareDownloadParam second = downloadParam();
        second.setShareToken("share-1");

        shareService.createDownload("shared-browser", null, first);
        shareService.createDownload("shared-browser", null, second);

        ArgumentCaptor<ArtifactAccess> access = ArgumentCaptor.forClass(ArtifactAccess.class);
        verify(artifactTasks, org.mockito.Mockito.times(2)).submit(access.capture(), any(), any());
        assertEquals(
                ArtifactAccess.shared("shared-browser", "share-1", "visitor-a", "share-owner"),
                access.getAllValues().get(0)
        );
        assertEquals(
                ArtifactAccess.shared("shared-browser", "share-1", "visitor-b", "share-owner"),
                access.getAllValues().get(1)
        );
    }

    private Share codeShare(String id, String creatorId, String password) {
        Share share = new Share();
        share.setId(id);
        share.setCreateBy(creatorId);
        share.setVizType("DASHBOARD");
        share.setVizId("dashboard-1");
        share.setAuthenticationMode(ShareAuthenticationMode.CODE.name());
        share.setAuthenticationCode(password);
        share.setRowPermissionBy(ShareRowPermissionBy.CREATOR.name());
        return share;
    }

    private ShareToken tokenFor(String viewId) {
        ShareAuthorizedToken token = new ShareAuthorizedToken();
        token.setVizType(ResourceType.VIEW);
        token.setVizId(viewId);
        token.setCreateBy("share-owner-id");
        token.setPermissionBy("share-owner");
        return ShareToken.create(AESUtil.encrypt(token, TOKEN_SECRET));
    }

    private ShareDownloadParam downloadParam() {
        DownloadQueryRequest request = new DownloadQueryRequest();
        request.setViewId("view-1");
        ShareDownloadParam param = new ShareDownloadParam();
        param.setFileName("orders");
        param.setDownloadParams(List.of(request));
        param.setExecuteToken(Map.of("view-1", tokenFor("view-1")));
        return param;
    }

    private ArtifactTasks ownedTask(ArtifactAccess owner, TaskView task, String content) {
        return new ArtifactTasks() {
            @Override
            public TaskHandle submit(ArtifactAccess access, ArtifactDescriptor descriptor,
                                     ArtifactProducer producer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TaskBatch inspect(ArtifactAccess access, Set<String> ids) {
                if (owner.equals(access) && ids.contains(task.id())) {
                    return new TaskBatch(List.of(task), Set.of());
                }
                return new TaskBatch(List.of(), ids);
            }

            @Override
            public TaskPage list(ArtifactAccess access, int terminalOffset, int terminalLimit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public TaskHandle retry(ArtifactAccess access, String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ArtifactContent open(ArtifactAccess access, String id) {
                if (!owner.equals(access) || !task.id().equals(id)) {
                    throw new ArtifactTaskException(
                            "ARTIFACT_NOT_FOUND", "产物任务不存在或已过期", "trace-not-found");
                }
                byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                return new ArtifactContent(task.descriptor().fileName(), task.descriptor().mediaType(),
                        bytes.length, new ByteArrayInputStream(bytes));
            }

            @Override
            public void confirmDelivery(ArtifactAccess access, String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void delete(ArtifactAccess access, String id) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void assertNotFound(org.junit.jupiter.api.function.Executable executable) {
        ArtifactTaskException error = assertThrows(ArtifactTaskException.class, executable);
        assertEquals("ARTIFACT_NOT_FOUND", error.code());
        org.junit.jupiter.api.Assertions.assertFalse(error.traceId().isBlank());
    }
}

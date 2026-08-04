package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.NoSuchSessionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import yubi.core.base.consts.AttachmentType;
import yubi.core.common.Application;
import yubi.core.entity.User;
import yubi.core.mappers.ext.DownloadMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.artifact.ArtifactAccess;
import yubi.server.artifact.ArtifactContent;
import yubi.server.artifact.ArtifactDescriptor;
import yubi.server.artifact.ArtifactGenerationException;
import yubi.server.artifact.ArtifactProducer;
import yubi.server.artifact.ArtifactTaskState;
import yubi.server.artifact.ArtifactTasks;
import yubi.server.artifact.ArtifactWorkContext;
import yubi.server.artifact.TaskBatch;
import yubi.server.artifact.TaskHandle;
import yubi.server.artifact.TaskPage;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.service.AttachmentService;
import yubi.server.service.DownloadService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadServiceImplArtifactTest {

    @TempDir
    Path tempDir;

    private DownloadMapperExt downloadMapper;
    private YuBiSecurityManager securityManager;
    private RecordingArtifactTasks artifactTasks;
    private AttachmentService attachmentService;
    private DownloadServiceImpl service;

    @BeforeEach
    void setUp() {
        downloadMapper = mock(DownloadMapperExt.class);
        securityManager = mock(YuBiSecurityManager.class);
        artifactTasks = new RecordingArtifactTasks();
        attachmentService = mock(AttachmentService.class);
        service = new DownloadServiceImpl(downloadMapper, artifactTasks, ignored -> attachmentService);
        service.setSecurityManager(securityManager);

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.env.file-path")).thenReturn(tempDir.toString());
        new Application().setApplicationContext(applicationContext);

        User user = new User();
        user.setId("user-1");
        user.setUsername("alice");
        when(securityManager.getCurrentUser()).thenReturn(user);
    }

    @Test
    void 登录用户提交Excel导出后返回产物任务并删除附件临时文件() throws Exception {
        Path temporaryFile = tempDir.resolve("generated.xlsx");
        Files.writeString(temporaryFile, "excel-content");
        when(attachmentService.getFile(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("销售报表")
        )).thenReturn(temporaryFile.toFile());
        DownloadCreateParam param = downloadParam("销售报表", AttachmentType.EXCEL);

        TaskHandle result = service.submitDownloadTask(param);

        assertSame(artifactTasks.handle, result);
        assertEquals(ArtifactAccess.authenticated("user-1", "org-1", "alice"), artifactTasks.access);
        assertEquals(new ArtifactDescriptor(
                "销售报表",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ".xlsx",
                "VISUALIZATION"
        ), artifactTasks.descriptor);
        assertEquals("excel-content", artifactTasks.output.toString());
        assertFalse(Files.exists(temporaryFile));
        verify(securityManager).runAs("alice");
        verify(securityManager).releaseRunAs();
    }

    @Test
    void 登录导出统一使用产物任务且定时任务保留同步附件接口() throws Exception {
        assertEquals(
                TaskHandle.class,
                DownloadService.class.getMethod(
                        "submitDownloadTask",
                        DownloadCreateParam.class
                ).getReturnType()
        );
        assertThrows(
                NoSuchMethodException.class,
                () -> DownloadService.class.getMethod(
                        "submitDownloadTask",
                        DownloadCreateParam.class,
                        String.class
                )
        );
        assertEquals(
                File.class,
                AttachmentService.class.getMethod(
                        "getFile",
                        DownloadCreateParam.class,
                        String.class,
                        String.class
                ).getReturnType()
        );
    }

    @Test
    void PDF渲染服务不可用时返回稳定分类且不暴露底层地址() throws Exception {
        when(attachmentService.getFile(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).thenThrow(new SessionNotCreatedException(
                "无法连接 http://renderer.internal:4444，token=secret"));

        AssertionError thrown = assertThrows(AssertionError.class,
                () -> service.submitDownloadTask(downloadParam("经营报告", AttachmentType.PDF)));

        ArtifactGenerationException failure = assertInstanceOf(
                ArtifactGenerationException.class, thrown.getCause());
        assertEquals("ARTIFACT_PDF_RENDERER_UNAVAILABLE", failure.code());
        assertEquals("PDF 渲染服务暂时不可用，请稍后重试", failure.hint());
        assertFalse(failure.hint().contains("renderer.internal"));
        assertFalse(failure.hint().contains("secret"));
    }

    @Test
    void PDF渲染会话中途丢失时归类为渲染服务不可用() throws Exception {
        when(attachmentService.getFile(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).thenThrow(new IllegalStateException(
                "附件渲染失败",
                new NoSuchSessionException("renderer.internal 会话已丢失")
        ));

        AssertionError thrown = assertThrows(AssertionError.class,
                () -> service.submitDownloadTask(downloadParam("经营报告", AttachmentType.PDF)));

        ArtifactGenerationException failure = assertInstanceOf(
                ArtifactGenerationException.class, thrown.getCause());
        assertEquals("ARTIFACT_PDF_RENDERER_UNAVAILABLE", failure.code());
        assertEquals("PDF 渲染服务暂时不可用，请稍后重试", failure.hint());
        assertFalse(failure.hint().contains("renderer.internal"));
    }

    private DownloadCreateParam downloadParam(String fileName, AttachmentType type) {
        DownloadQueryRequest request = new DownloadQueryRequest();
        request.setViewId("view-1");
        DownloadCreateParam param = new DownloadCreateParam();
        param.setOrgId("org-1");
        param.setFileName(fileName);
        param.setDownloadType(type);
        param.setDownloadParams(List.of(request));
        return param;
    }

    private static final class RecordingArtifactTasks implements ArtifactTasks {

        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final TaskHandle handle = new TaskHandle(
                "task-1",
                new ArtifactDescriptor("销售报表", "application/octet-stream", ".bin"),
                ArtifactTaskState.QUEUED,
                Instant.parse("2026-07-24T08:00:00Z"),
                Instant.parse("2026-07-24T08:15:00Z"),
                "trace-1"
        );
        private ArtifactAccess access;
        private ArtifactDescriptor descriptor;

        @Override
        public TaskHandle submit(ArtifactAccess access,
                                 ArtifactDescriptor descriptor,
                                 ArtifactProducer producer) {
            this.access = access;
            this.descriptor = descriptor;
            try {
                producer.produce(new ArtifactWorkContext(
                        output,
                        Instant.parse("2026-07-24T08:15:00Z"),
                        "alice"
                ));
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
            return handle;
        }

        @Override
        public TaskBatch inspect(ArtifactAccess access, Set<String> ids) {
            throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public void confirmDelivery(ArtifactAccess access, String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(ArtifactAccess access, String id) {
            throw new UnsupportedOperationException();
        }
    }
}

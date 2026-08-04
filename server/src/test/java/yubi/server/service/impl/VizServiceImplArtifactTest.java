package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import yubi.core.entity.Datachart;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.artifact.ArtifactAccess;
import yubi.server.artifact.ArtifactContent;
import yubi.server.artifact.ArtifactDescriptor;
import yubi.server.artifact.ArtifactProducer;
import yubi.server.artifact.ArtifactTaskState;
import yubi.server.artifact.ArtifactTasks;
import yubi.server.artifact.ArtifactWorkContext;
import yubi.server.artifact.TaskBatch;
import yubi.server.artifact.TaskHandle;
import yubi.server.artifact.TaskPage;
import yubi.server.base.transfer.DatachartTemplateParam;
import yubi.server.base.transfer.ResourceTransferParam;
import yubi.server.base.transfer.model.DatachartTemplateModel;
import yubi.server.base.transfer.model.ResourceModel;
import yubi.server.service.DashboardService;
import yubi.server.service.DatachartService;
import yubi.server.service.FileService;
import yubi.server.service.FolderService;
import yubi.server.service.SourceService;
import yubi.server.service.StoryboardService;
import yubi.server.service.StorypageService;
import yubi.server.service.VariableService;
import yubi.server.service.ViewService;
import yubi.core.common.MessageResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VizServiceImplArtifactTest {

    private RecordingArtifactTasks artifactTasks;
    private YuBiSecurityManager securityManager;
    private VizServiceImpl service;

    @BeforeEach
    void setUp() {
        new MessageResolver().setMessageSource(new StaticMessageSource());
        artifactTasks = new RecordingArtifactTasks();
        securityManager = mock(YuBiSecurityManager.class);
        service = new VizServiceImpl(
                mock(DatachartService.class),
                mock(DashboardService.class),
                mock(StoryboardService.class),
                mock(StorypageService.class),
                mock(FolderService.class),
                mock(ViewService.class),
                mock(SourceService.class),
                mock(VariableService.class),
                mock(FileService.class),
                artifactTasks
        );
        service.setSecurityManager(securityManager);
        User user = new User();
        user.setId("user-1");
        user.setUsername("alice");
        when(securityManager.getCurrentUser()).thenReturn(user);
    }

    @Test
    void 数据图表模板通过产物输出流生成Gzip对象文件() throws Exception {
        Datachart datachart = new Datachart();
        datachart.setName("销售图表");
        DatachartTemplateParam param = new DatachartTemplateParam();
        param.setOrgId("org-1");
        param.setDatachart(datachart);

        TaskHandle result = service.exportDatachartTemplate(param);

        assertSame(artifactTasks.handle, result);
        assertEquals(ArtifactAccess.authenticated("user-1", "org-1", "alice"), artifactTasks.access);
        assertEquals(".ybt", artifactTasks.descriptor.suffix());
        assertEquals("application/octet-stream", artifactTasks.descriptor.mediaType());
        try (ObjectInputStream input = new ObjectInputStream(
                new GZIPInputStream(new ByteArrayInputStream(artifactTasks.output.toByteArray()))
        )) {
            DatachartTemplateModel model = assertInstanceOf(DatachartTemplateModel.class, input.readObject());
            assertEquals("销售图表", model.getDatachart().getName());
        }
        verify(securityManager).runAs("alice");
        verify(securityManager).releaseRunAs();
    }

    @Test
    void 资源导出继续返回ybr产物任务() throws Exception {
        ResourceTransferParam param = new ResourceTransferParam();
        param.setOrgId("org-1");
        param.setResources(List.of());

        service.exportResource(param);

        assertEquals(".ybr", artifactTasks.descriptor.suffix());
        try (ObjectInputStream input = new ObjectInputStream(
                new GZIPInputStream(new ByteArrayInputStream(artifactTasks.output.toByteArray()))
        )) {
            assertInstanceOf(ResourceModel.class, input.readObject());
        }
    }

    private static final class RecordingArtifactTasks implements ArtifactTasks {

        private ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final TaskHandle handle = new TaskHandle(
                "task-1",
                new ArtifactDescriptor("export", "application/octet-stream", ".ybt"),
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
            output = new ByteArrayOutputStream();
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

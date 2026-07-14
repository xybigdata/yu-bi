package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import yubi.core.common.MessageResolver;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.transfer.DatachartTemplateParam;
import yubi.server.service.DashboardService;
import yubi.server.service.DatachartService;
import yubi.server.service.DownloadService;
import yubi.server.service.FileService;
import yubi.server.service.FolderService;
import yubi.server.service.SourceService;
import yubi.server.service.StoryboardService;
import yubi.server.service.StorypageService;
import yubi.server.service.VariableService;
import yubi.server.service.ViewService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VizServiceImplDownloadBoundaryTest {

    private DownloadService downloadService;
    private VizServiceImpl vizService;

    @BeforeEach
    void setUp() {
        new MessageResolver().setMessageSource(new StaticMessageSource());
        downloadService = mock(DownloadService.class);
        vizService = new VizServiceImpl(
                mock(DatachartService.class),
                mock(DashboardService.class),
                mock(StoryboardService.class),
                mock(StorypageService.class),
                mock(FolderService.class),
                mock(ViewService.class),
                mock(SourceService.class),
                mock(VariableService.class),
                mock(FileService.class),
                downloadService
        );
    }

    @Test
    void shouldCreateTemplateExportThroughUnifiedDownloadService() {
        DownloadTaskDTO expected = new DownloadTaskDTO("task-1", "template", (byte) 0);
        when(downloadService.submitGeneratedTask(any(), any())).thenReturn(expected);

        DownloadTaskDTO result = vizService.exportDatachartTemplate(new DatachartTemplateParam());

        assertSame(expected, result);
        verify(downloadService).submitGeneratedTask(
                argThat(name -> name != null && !name.contains("/") && !name.contains("\\")),
                any()
        );
    }
}

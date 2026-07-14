package yubi.server.service.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import yubi.core.common.Application;
import yubi.core.common.WebUtils;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.data.provider.Dataframe;
import yubi.core.entity.Folder;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.security.base.ResourceType;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.base.params.ShareToken;
import yubi.server.query.DownloadQueryExecutor;
import yubi.server.service.FolderService;
import yubi.server.service.OrgSettingService;
import yubi.server.service.ShareService;
import yubi.server.service.TemporaryAttachmentShareService;
import yubi.server.service.ViewService;
import yubi.server.service.VizService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentTemporaryShareLifecycleTest {

    private static final String SHARE_ID = "secret-share-id";

    @TempDir
    Path tempDir;

    private ShareService shareService;
    private TemporaryAttachmentShareService temporaryShareService;
    private Logger rootLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        YuBiSecurityManager securityManager = mock(YuBiSecurityManager.class);
        User user = new User();
        user.setId("user-1");
        when(securityManager.getCurrentUser()).thenReturn(user);

        shareService = mock(ShareService.class);
        ShareToken share = new ShareToken();
        share.setId(SHARE_ID);
        when(shareService.createShare(anyString(), any())).thenReturn(share);
        when(shareService.delete(SHARE_ID, false)).thenReturn(true);

        FolderService folderService = mock(FolderService.class);
        Folder folder = new Folder();
        folder.setRelId("dashboard-1");
        folder.setRelType(ResourceType.DASHBOARD.name());
        when(folderService.retrieve("folder-1")).thenReturn(folder);
        temporaryShareService = new TemporaryAttachmentShareService(
                securityManager,
                shareService,
                folderService
        );

        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logAppender = new ListAppender<>();
        logAppender.start();
        rootLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        rootLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void shouldRevokeTemporaryShareOnceAfterSuccessfulImage() throws Exception {
        Path screenshot = Files.writeString(tempDir.resolve("screenshot.png"), "image");
        try (MockedStatic<Application> application = mockStatic(Application.class);
            MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            application.when(Application::getWebRootURL).thenReturn("https://internal.example");
            application.when(Application::getFileBasePath).thenReturn(tempDir.toString());
            webUtils.when(() -> WebUtils.screenShot2File(
                    anyString(),
                    anyString(),
                    eq(640)
            )).thenReturn(screenshot.toFile());

            File file = new AttachmentImageServiceImpl(temporaryShareService).getFile(
                    downloadParam(),
                    tempDir.toString(),
                    "dashboard"
            );

            assertTrue(file.isFile());
            assertEquals("image", Files.readString(file.toPath()));
        }

        verify(shareService).delete(SHARE_ID, false);
        assertLogsContainNoCredential();
    }

    @Test
    void shouldPreserveScreenshotFailureAndStillRevokeOnce() {
        IOException screenshotFailure = new IOException("screenshot raw-secret failure");
        when(shareService.delete(SHARE_ID, false))
                .thenThrow(new IllegalStateException("cleanup raw-secret failure"));
        try (MockedStatic<Application> application = mockStatic(Application.class);
            MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            application.when(Application::getWebRootURL).thenReturn("https://internal.example");
            application.when(Application::getFileBasePath).thenReturn(tempDir.toString());
            webUtils.when(() -> WebUtils.screenShot2File(
                    anyString(),
                    anyString(),
                    eq(640)
            )).thenThrow(screenshotFailure);

            Exception actual = assertThrows(
                    Exception.class,
                    () -> new AttachmentImageServiceImpl(temporaryShareService).getFile(
                            downloadParam(),
                            tempDir.toString(),
                            "dashboard"
                    )
            );
            assertEquals(screenshotFailure, actual);
        }

        verify(shareService).delete(SHARE_ID, false);
        assertLogsContainNoCredential();
    }

    @Test
    void shouldRevokeTemporaryShareOnceWhenPdfConversionFails() throws Exception {
        Path screenshot = Files.writeString(tempDir.resolve("pdf-source.png"), "image");
        AttachmentPdfServiceImpl pdf = spy(new AttachmentPdfServiceImpl(temporaryShareService));
        doThrow(new IOException("pdf raw-secret failure"))
                .when(pdf).createPDFFromImage(anyString(), anyString());

        try (MockedStatic<Application> application = mockStatic(Application.class);
            MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            application.when(Application::getWebRootURL).thenReturn("https://internal.example");
            application.when(Application::getFileBasePath).thenReturn(tempDir.toString());
            webUtils.when(() -> WebUtils.screenShot2File(
                    anyString(),
                    anyString(),
                    eq(640)
            )).thenReturn(screenshot.toFile());

            assertThrows(
                    IOException.class,
                    () -> pdf.getFile(downloadParam(), tempDir.toString(), "dashboard")
            );
        }

        verify(shareService).delete(SHARE_ID, false);
        assertFalse(Files.exists(screenshot));
        assertLogsContainNoCredential();
    }

    @Test
    void shouldFailClosedWhenSuccessfulGenerationReturnsFalseOnRevoke() {
        when(shareService.delete(SHARE_ID, false)).thenReturn(false);
        try (MockedStatic<Application> application = mockStatic(Application.class)) {
            application.when(Application::getWebRootURL).thenReturn("https://internal.example");

            NotAllowedException exception = assertThrows(
                    NotAllowedException.class,
                    () -> temporaryShareService.withTemporaryShare(downloadParam(), url -> "generated")
            );

            assertEquals("临时下载分享不可用", exception.getMessage());
        }

        verify(shareService).delete(SHARE_ID, false);
        assertLogsContainNoCredential();
    }

    @Test
    void shouldFailClosedWhenSuccessfulGenerationThrowsOnRevoke() {
        when(shareService.delete(SHARE_ID, false))
                .thenThrow(new IllegalStateException("cleanup raw-secret failure"));
        try (MockedStatic<Application> application = mockStatic(Application.class)) {
            application.when(Application::getWebRootURL).thenReturn("https://internal.example");

            NotAllowedException exception = assertThrows(
                    NotAllowedException.class,
                    () -> temporaryShareService.withTemporaryShare(downloadParam(), url -> "generated")
            );

            assertEquals("临时下载分享不可用", exception.getMessage());
        }

        verify(shareService).delete(SHARE_ID, false);
        assertLogsContainNoCredential();
    }

    @Test
    void shouldRevokeOnceWhenUrlConstructionFailsAfterShareCreation() {
        IllegalStateException failure = new IllegalStateException("web root unavailable");
        try (MockedStatic<Application> application = mockStatic(Application.class)) {
            application.when(Application::getWebRootURL).thenThrow(failure);

            IllegalStateException actual = assertThrows(
                    IllegalStateException.class,
                    () -> temporaryShareService.withTemporaryShare(downloadParam(), url -> "not-run")
            );

            assertEquals(failure, actual);
        }

        verify(shareService).delete(SHARE_ID, false);
        assertLogsContainNoCredential();
    }

    @Test
    void shouldGenerateSharedExcelWithoutReadingUntrustedVizConfiguration() throws Exception {
        OrgSettingService orgSettingService = mock(OrgSettingService.class);
        when(orgSettingService.getDownloadRecordLimit("org-1")).thenReturn(1000);
        ViewService viewService = mock(ViewService.class);
        View view = new View();
        view.setId("view-1");
        view.setOrgId("org-1");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        VizService vizService = mock(VizService.class);
        DownloadQueryExecutor queryExecutor = mock(DownloadQueryExecutor.class);

        DownloadQueryRequest query = new DownloadQueryRequest();
        query.setViewId("view-1");
        query.setVizType(ResourceType.DATACHART);
        query.setVizId("outside-datachart");
        DownloadCreateParam param = new DownloadCreateParam();
        param.setDownloadParams(List.of(query));
        when(queryExecutor.executeShared(query, "visitor-1", "org-1"))
                .thenReturn(Dataframe.empty());

        try (MockedStatic<Application> application = mockStatic(Application.class)) {
            application.when(() -> Application.getBean(OrgSettingService.class))
                    .thenReturn(orgSettingService);
            application.when(() -> Application.getBean(ViewService.class))
                    .thenReturn(viewService);

            File file = new AttachmentExcelServiceImpl(vizService, queryExecutor).getFile(
                    param,
                    tempDir.toString(),
                    "shared-dashboard",
                    "visitor-1",
                    "org-1"
            );

            assertTrue(file.isFile());
        }

        verify(queryExecutor).executeShared(query, "visitor-1", "org-1");
        verify(vizService, never()).getChartConfigByVizId(any(), any());
    }

    private DownloadCreateParam downloadParam() {
        DownloadQueryRequest query = new DownloadQueryRequest();
        query.setVizId("folder-1");
        DownloadCreateParam param = new DownloadCreateParam();
        param.setDownloadParams(List.of(query));
        param.setImageWidth(640);
        return param;
    }

    private void assertLogsContainNoCredential() {
        String logs = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
        assertFalse(logs.contains(SHARE_ID));
        assertFalse(logs.contains("internal.example"));
        assertFalse(logs.contains("raw-secret"));
        assertFalse(logs.contains("authentication"));
        assertFalse(logs.contains("?eager=true"));
    }
}

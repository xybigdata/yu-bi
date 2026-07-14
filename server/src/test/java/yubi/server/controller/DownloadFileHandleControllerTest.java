package yubi.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.common.Application;
import yubi.server.service.DownloadFileResolver;
import yubi.server.service.DownloadFileResource;
import yubi.server.service.DownloadFileTarget;
import yubi.server.service.DownloadService;
import yubi.server.service.ShareDownloadSession;
import yubi.server.service.ShareDownloadSessionManager;
import yubi.server.service.ShareService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadFileHandleControllerTest {

    @TempDir
    Path tempDir;

    private DownloadFileResolver resolver;

    @BeforeEach
    void setUp() {
        ApplicationContext context = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(context.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.env.file-path")).thenReturn(tempDir.toString());
        new Application().setApplicationContext(context);
        resolver = new DownloadFileResolver();
    }

    @Test
    void authenticatedControllerShouldUseAndCloseOpenedHandleAfterSymlinkSwap() throws Exception {
        Path target = createDownloadFile("task-1", "orders.xlsx", "safe-authenticated");
        Path outside = Files.writeString(tempDir.resolve("outside-auth.txt"), "outside-secret");
        AtomicReference<DownloadFileResource> opened = new AtomicReference<>();
        DownloadService downloadService = mock(DownloadService.class);
        when(downloadService.downloadFile("task-1")).thenAnswer(ignored -> {
            DownloadFileResource resource = resolver.openForRead("download/task-1/orders.xlsx");
            opened.set(resource);
            replaceWithSymlink(target, outside);
            return resource;
        });

        MockHttpServletResponse response = new MockHttpServletResponse();
        new DownloadController(downloadService).downloadFile("task-1", response);

        assertArrayEquals("safe-authenticated".getBytes(), response.getContentAsByteArray());
        assertThrows(IOException.class, () -> opened.get().inputStream().read());
    }

    @Test
    void sharedControllerShouldUseAndCloseOpenedHandleAfterSymlinkSwap() throws Exception {
        Path target = createDownloadFile("task-2", "shared.xlsx", "safe-shared");
        Path outside = Files.writeString(tempDir.resolve("outside-share.txt"), "outside-secret");
        AtomicReference<DownloadFileResource> opened = new AtomicReference<>();
        ShareService shareService = mock(ShareService.class);
        ShareDownloadSessionManager sessionManager = mock(ShareDownloadSessionManager.class);
        ShareDownloadSession session = new ShareDownloadSession(
                "share-1",
                "a".repeat(64),
                "b".repeat(64),
                ShareAuthenticationMode.NONE,
                null
        );
        when(sessionManager.require(eq("share-1"), any())).thenReturn(session);
        when(shareService.download("share-1", session, "task-2")).thenAnswer(ignored -> {
            DownloadFileResource resource = resolver.openForRead("download/task-2/shared.xlsx");
            opened.set(resource);
            replaceWithSymlink(target, outside);
            return resource;
        });

        MockHttpServletResponse response = new MockHttpServletResponse();
        new ShareDownloadController(shareService, sessionManager).downloadFile(
                "share-1",
                "task-2",
                new MockHttpServletRequest(),
                response
        );

        assertArrayEquals("safe-shared".getBytes(), response.getContentAsByteArray());
        assertThrows(IOException.class, () -> opened.get().inputStream().read());
    }

    private Path createDownloadFile(String taskId, String fileName, String content) throws Exception {
        Path taskDirectory = resolver.createTaskDirectory(taskId);
        try (DownloadFileTarget target = resolver.createTarget(taskDirectory, fileName)) {
            target.outputStream().write(content.getBytes());
        }
        return taskDirectory.resolve(fileName);
    }

    private void replaceWithSymlink(Path target, Path outside) throws Exception {
        Files.delete(target);
        Files.createSymbolicLink(target, outside);
    }
}

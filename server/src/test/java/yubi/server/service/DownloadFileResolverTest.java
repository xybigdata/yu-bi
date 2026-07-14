package yubi.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.common.Application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DownloadFileResolverTest {

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
    void shouldCreateExclusiveTaskDirectoryAndTarget() throws Exception {
        Path taskDirectory = resolver.createTaskDirectory("task-1");
        writeTarget(resolver, taskDirectory, "orders.xlsx", "content");

        try (DownloadFileResource resource = resolver.openForRead("download/task-1/orders.xlsx")) {
            assertEquals("content", new String(
                    resource.inputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ));
            assertEquals("orders.xlsx", resource.fileName());
        }
        assertThrows(NotAllowedException.class, () -> resolver.createTaskDirectory("task-1"));
        assertThrows(
                NotAllowedException.class,
                () -> resolver.createTarget(taskDirectory, "orders.xlsx")
        );

        Path legacyFile = resolver.downloadRoot().resolve("legacy.xlsx");
        Files.writeString(legacyFile, "legacy");
        assertThrows(NotAllowedException.class, () -> resolver.openForRead("download/legacy.xlsx"));
        assertEquals("legacy", Files.readString(legacyFile));
    }

    @Test
    void shouldRejectTraversalOutsidePathAndCrossTaskResult() throws Exception {
        assertThrows(NotAllowedException.class, () -> resolver.createTaskDirectory("../outside"));

        Path firstDirectory = resolver.createTaskDirectory("task-1");
        Path secondDirectory = resolver.createTaskDirectory("task-2");
        assertThrows(
                NotAllowedException.class,
                () -> resolver.createTarget(firstDirectory, "../outside.xlsx")
        );

        Path staging = Files.createTempDirectory(tempDir, "staging-");
        Path source = Files.writeString(staging.resolve("orders.xlsx"), "second");
        assertEquals(
                "download/task-2/orders.xlsx",
                resolver.copyIntoTask(secondDirectory, staging, source)
        );
        assertThrows(
                NotAllowedException.class,
                () -> resolver.copyIntoTask(firstDirectory, staging, tempDir.resolve("outside.xlsx"))
        );

        Path outside = tempDir.resolve("outside.xlsx");
        Files.writeString(outside, "secret");
        assertThrows(NotAllowedException.class, () -> resolver.openForRead(outside.toString()));
        assertThrows(
                NotAllowedException.class,
                () -> resolver.openForRead("download/task-2/../task-2/orders.xlsx")
        );
    }

    @Test
    void shouldRejectExistingSymlinkDirectoryAndTargetWithoutWritingOutsideRoot() throws Exception {
        Path outsideDirectory = Files.createDirectory(tempDir.resolve("outside-directory"));
        Path linkedTaskDirectory = resolver.downloadRoot().resolve("linked-task");
        Files.createSymbolicLink(linkedTaskDirectory, outsideDirectory);
        assertThrows(NotAllowedException.class, () -> resolver.createTaskDirectory("linked-task"));

        Path taskDirectory = resolver.createTaskDirectory("task-1");
        Path outside = tempDir.resolve("outside.xlsx");
        Files.writeString(outside, "secret");
        Path linkedTarget = taskDirectory.resolve("orders.xlsx");
        Files.createSymbolicLink(linkedTarget, outside);
        assertThrows(
                NotAllowedException.class,
                () -> resolver.createTarget(taskDirectory, "orders.xlsx")
        );
        assertEquals("secret", Files.readString(outside));
        assertThrows(
                NotAllowedException.class,
                () -> resolver.openForRead("download/task-1/orders.xlsx")
        );

        Files.createDirectory(taskDirectory.resolve("directory.xlsx"));
        assertThrows(
                NotAllowedException.class,
                () -> resolver.openForRead("download/task-1/directory.xlsx")
        );
    }

    @Test
    void shouldKeepOpenedHandleOnValidatedFileWhenPathIsReplacedBySymlink() throws Exception {
        Path taskDirectory = resolver.createTaskDirectory("task-1");
        writeTarget(resolver, taskDirectory, "orders.xlsx", "safe-content");
        Path target = taskDirectory.resolve("orders.xlsx");
        Path outside = tempDir.resolve("outside-secret.xlsx");
        Files.writeString(outside, "outside-secret");

        DownloadFileResource resource = resolver.openForRead("download/task-1/orders.xlsx");
        Files.delete(target);
        Files.createSymbolicLink(target, outside);

        assertEquals("safe-content", new String(
                resource.inputStream().readAllBytes(),
                StandardCharsets.UTF_8
        ));
        resource.close();
        assertThrows(IOException.class, resource.inputStream()::read);
        assertEquals("outside-secret", Files.readString(outside));
    }

    @Test
    void shouldRejectDownloadRootSymlink() throws Exception {
        Path outsideDirectory = Files.createDirectory(tempDir.resolve("outside-directory"));
        Files.createSymbolicLink(tempDir.resolve("download"), outsideDirectory);

        assertThrows(NotAllowedException.class, resolver::downloadRoot);
    }

    private void writeTarget(DownloadFileResolver targetResolver,
                             Path taskDirectory,
                             String fileName,
                             String content) throws Exception {
        try (DownloadFileTarget target = targetResolver.createTarget(taskDirectory, fileName)) {
            target.outputStream().write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}

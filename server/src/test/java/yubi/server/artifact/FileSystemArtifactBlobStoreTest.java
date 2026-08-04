package yubi.server.artifact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemArtifactBlobStoreTest {

    @TempDir
    Path directory;

    @Test
    void shouldAtomicallyPublishOpenAndIdempotentlyDeleteABlob() throws Exception {
        ArtifactBlobStore store = new FileSystemArtifactBlobStore(directory);
        byte[] expected = "安全产物内容".getBytes(StandardCharsets.UTF_8);
        String blobKey;

        try (ArtifactBlobWriter writer = store.begin("task-1")) {
            writer.output().write(expected);
            blobKey = writer.commit();
        }

        StoredBlob stored = store.open(blobKey);
        try (var stream = stored.stream()) {
            assertEquals(expected.length, stored.length());
            assertArrayEquals(expected, stream.readAllBytes());
        }
        assertEquals(1, regularFiles(directory));

        store.delete(blobKey);
        store.delete(blobKey);
        assertEquals(0, regularFiles(directory));
        assertThrows(IllegalStateException.class, () -> store.open(blobKey));
    }

    @Test
    void shouldCleanTemporaryFilesOnAbortAndUncommittedClose() throws Exception {
        ArtifactBlobStore store = new FileSystemArtifactBlobStore(directory);
        ArtifactBlobWriter aborted = store.begin("task-abort");
        aborted.output().write(1);

        aborted.abort();
        aborted.abort();
        aborted.close();

        assertEquals(0, regularFiles(directory));
        assertThrows(IllegalStateException.class, aborted::commit);

        try (ArtifactBlobWriter uncommitted = store.begin("task-close")) {
            uncommitted.output().write(2);
        }
        assertEquals(0, regularFiles(directory));
        assertThrows(IllegalArgumentException.class, () -> store.open("../outside"));
    }

    private long regularFiles(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }
}

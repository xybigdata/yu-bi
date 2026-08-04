package yubi.server.artifact;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

final class InMemoryArtifactBlobStore implements ArtifactBlobStore {

    private final Map<String, byte[]> blobs = new HashMap<>();

    @Override
    public ArtifactBlobWriter begin(String taskId) {
        return new Writer(taskId);
    }

    @Override
    public synchronized StoredBlob open(String blobKey) {
        byte[] content = blobs.get(blobKey);
        if (content == null) {
            throw new IllegalStateException("产物文件不存在");
        }
        return new StoredBlob(content.length, new ByteArrayInputStream(content));
    }

    @Override
    public synchronized void delete(String blobKey) {
        blobs.remove(blobKey);
    }

    private final class Writer implements ArtifactBlobWriter {

        private final String blobKey;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private boolean completed;

        private Writer(String blobKey) {
            this.blobKey = blobKey;
        }

        @Override
        public ByteArrayOutputStream output() {
            return output;
        }

        @Override
        public String commit() {
            synchronized (InMemoryArtifactBlobStore.this) {
                blobs.put(blobKey, output.toByteArray());
            }
            completed = true;
            return blobKey;
        }

        @Override
        public void abort() {
            if (completed) {
                InMemoryArtifactBlobStore.this.delete(blobKey);
            }
            completed = true;
        }

        @Override
        public void close() {
            if (!completed) {
                abort();
            }
        }
    }
}

package yubi.server.artifact;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

interface ArtifactBlobStore {

    ArtifactBlobWriter begin(String taskId) throws IOException;

    StoredBlob open(String blobKey);

    void delete(String blobKey);
}

interface ArtifactBlobWriter extends AutoCloseable {

    OutputStream output();

    String commit();

    void abort();

    @Override
    void close();
}

record StoredBlob(long length, InputStream stream) {
}

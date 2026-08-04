package yubi.server.artifact;

import java.io.IOException;
import java.io.InputStream;

public record ArtifactContent(String fileName,
                              String mediaType,
                              long length,
                              InputStream stream) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        stream.close();
    }
}

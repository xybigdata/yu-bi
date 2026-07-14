package yubi.server.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public record DownloadFileResource(InputStream inputStream, String fileName) implements AutoCloseable {

    public DownloadFileResource {
        Objects.requireNonNull(inputStream, "inputStream");
        Objects.requireNonNull(fileName, "fileName");
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}

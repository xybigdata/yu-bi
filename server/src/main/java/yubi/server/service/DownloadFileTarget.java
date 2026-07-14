package yubi.server.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public record DownloadFileTarget(
        OutputStream outputStream,
        String storedPath,
        String fileName
) implements AutoCloseable {

    public DownloadFileTarget {
        Objects.requireNonNull(outputStream, "outputStream");
        Objects.requireNonNull(storedPath, "storedPath");
        Objects.requireNonNull(fileName, "fileName");
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}

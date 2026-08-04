package yubi.server.artifact;

import java.io.OutputStream;
import java.time.Instant;

public record ArtifactWorkContext(OutputStream output, Instant deadlineAt, String executionUser) {
}

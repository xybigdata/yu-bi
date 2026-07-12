package yubi.server.query;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyQueryArtifactArchitectureTest {

    private static final List<String> FORBIDDEN_PRODUCTION_ARTIFACTS = List.of(
            "ViewExecuteParam",
            "TestExecuteParam",
            "ServerQueryCompatibilityMapper",
            "concurrencyControlModel",
            "/data-provider/execute",
            "/shares/execute"
    );

    @Test
    void shouldKeepLegacyQueryArtifactsOutOfServerProductionCode() throws IOException {
        List<String> violations;
        try (var sources = Files.walk(Path.of("src/main/java"))) {
            violations = sources.filter(Files::isRegularFile)
                    .flatMap(path -> FORBIDDEN_PRODUCTION_ARTIFACTS.stream()
                            .filter(forbidden -> contains(path, forbidden))
                            .map(forbidden -> path + ": " + forbidden))
                    .toList();
        }

        assertEquals(List.of(), violations);
    }

    private boolean contains(Path path, String value) {
        try {
            return Files.readString(path).contains(value);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取生产源码 " + path, exception);
        }
    }
}

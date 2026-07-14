package yubi.query.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryModuleBoundaryTest {

    private static final List<String> FORBIDDEN = List.of(
            "yubi.core.", "yubi.security.", "yubi.server.", "org.springframework.",
            "org.mybatis.", "tools.jackson.", "com.fasterxml.jackson.", "AESUtil",
            "DataProvider", "Dataframe", "QueryScript", "ExecuteParam",
            "com.openai.", "dev.langchain4j.", "com.anthropic.", "yubi.agent.",
            "AgentRuntime", "ToolRegistry");

    @Test
    void productionSourcesMustNotReferenceInfrastructureOrProviderTypes() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        try (var files = Files.walk(sourceRoot)) {
            List<String> violations = files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path).stream())
                    .toList();
            assertTrue(violations.isEmpty(), () -> "query 模块越界引用: " + violations);
        }
    }

    private List<String> violations(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN.stream()
                    .filter(source::contains)
                    .map(value -> path + " -> " + value)
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

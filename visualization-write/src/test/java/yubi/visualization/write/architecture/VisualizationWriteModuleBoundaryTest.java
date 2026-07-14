package yubi.visualization.write.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualizationWriteModuleBoundaryTest {

    private static final List<String> FORBIDDEN = List.of(
            "yubi.core.", "yubi.security.", "yubi.server.", "yubi.agent.", "yubi.query.",
            "org.springframework.", "org.mybatis.", "com.baomidou.", "tools.jackson.",
            "com.fasterxml.jackson.", "AESUtil", "DataProvider", "ProviderManager", "Dataframe",
            "Controller", "Mapper", "@Transactional", "java.sql.", "javax.sql.",
            "com.openai.", "dev.langchain4j.", "com.anthropic.", "azure.ai.openai.");

    @Test
    void productionSourcesMustRemainPureJavaAndIndependentFromAgentQueryAndInfrastructure() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java"))) {
            List<String> violations = files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path).stream())
                    .toList();
            assertTrue(violations.isEmpty(), () -> "visualization-write 模块越界引用: " + violations);
        }
    }

    private List<String> violations(Path path) {
        try {
            String source = Files.readString(path);
            return FORBIDDEN.stream()
                    .filter(source::contains)
                    .map(value -> path + " -> " + value)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

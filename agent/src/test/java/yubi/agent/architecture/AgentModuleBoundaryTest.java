package yubi.agent.architecture;

import org.junit.jupiter.api.Test;
import yubi.agent.api.AgentRequest;
import yubi.agent.domain.ModelProtocol.ModelTurn;
import yubi.agent.domain.AgentModels.ToolMetric;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentModuleBoundaryTest {

    private static final List<String> FORBIDDEN = List.of(
            "yubi.core.", "yubi.security.", "yubi.server.", "org.springframework.",
            "org.mybatis.", "tools.jackson.", "com.fasterxml.jackson.", "AESUtil",
            "DataProvider", "ProviderManager", "Dataframe", "PreviewQueryUseCase", "PreviewQueryCommand",
            "com.openai.", "dev.langchain4j.", "com.anthropic.", "azure.ai.openai.");
    private static final Set<String> ALLOWED_JAVA_SQL_VALUE_TYPES = Set.of("Date", "Time", "Timestamp");
    private static final Pattern JAVA_SQL_REFERENCE =
            Pattern.compile("java\\.sql\\.([A-Za-z][A-Za-z0-9_]*)");

    @Test
    void productionSourcesMustNotReferenceServerFrameworkProviderPreviewOrModelSdk() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java"))) {
            List<String> violations = files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path).stream())
                    .toList();
            assertTrue(violations.isEmpty(), () -> "agent 模块越界引用: " + violations);
        }
    }

    @Test
    void requestAndModelTurnMustNotCarryTrustedIdentityOrOrganization() {
        assertEquals(Set.of("message"), components(AgentRequest.class));
        assertEquals(Set.of("userMessage", "stepIndex", "tools", "history"), components(ModelTurn.class));
        assertEquals(Set.of("toolName", "status", "failureCategory", "durationMillis", "argumentNodes",
                "resultItems", "resultBytes", "queryRows"), components(ToolMetric.class));
    }

    @Test
    void productionSourcesMayOnlyUseJdbcTemporalValueTypes() throws IOException {
        try (var files = Files.walk(Path.of("src/main/java"))) {
            List<String> violations = files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> databaseApiViolations(path).stream())
                    .toList();
            assertTrue(violations.isEmpty(), () -> "agent 模块数据库 API 越界引用: " + violations);
        }
    }

    private Set<String> components(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(Collectors.toUnmodifiableSet());
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

    private List<String> databaseApiViolations(Path path) {
        try {
            String source = Files.readString(path);
            List<String> violations = JAVA_SQL_REFERENCE.matcher(source).results()
                    .map(result -> result.group(1))
                    .filter(type -> !ALLOWED_JAVA_SQL_VALUE_TYPES.contains(type))
                    .distinct()
                    .map(type -> path + " -> java.sql." + type)
                    .collect(Collectors.toCollection(ArrayList::new));
            if (source.contains("java.sql.*")) {
                violations.add(path + " -> java.sql.*");
            }
            if (source.contains("javax.sql.")) {
                violations.add(path + " -> javax.sql.");
            }
            return List.copyOf(violations);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

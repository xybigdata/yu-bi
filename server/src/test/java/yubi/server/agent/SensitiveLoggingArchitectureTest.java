package yubi.server.agent;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveLoggingArchitectureTest {

    private static final List<String> HARDENED_PRODUCTION_FILES = List.of(
            "src/main/java/yubi/server/query/ServerQueryDefinitionAdapter.java",
            "src/main/java/yubi/server/service/impl/UserServiceImpl.java",
            "../data-providers/data-provider-base/src/main/java/yubi/data/provider/DefaultDataProvider.java",
            "../data-providers/data-provider-base/src/main/java/yubi/data/provider/ProviderManager.java",
            "../data-providers/data-provider-base/src/main/java/yubi/data/provider/calcite/SpecialSqlCallConverter.java",
            "../data-providers/data-provider-base/src/main/java/yubi/data/provider/freemarker/FreemarkerContext.java",
            "../data-providers/data-provider-base/src/main/java/yubi/data/provider/local/LocalDB.java",
            "../data-providers/data-provider-base/src/main/java/yubi/data/provider/script/VariablePlaceholder.java",
            "../data-providers/jdbc-data-provider/src/main/java/yubi/data/provider/JdbcDataProvider.java",
            "../data-providers/jdbc-data-provider/src/main/java/yubi/data/provider/jdbc/adapters/JdbcDataProviderAdapter.java",
            "../data-providers/jdbc-data-provider/src/main/java/yubi/data/provider/jdbc/adapters/OracleDataProviderAdapter.java");
    private static final Set<String> LOG_METHODS = Set.of("trace", "debug", "info", "warn", "error");
    private static final Set<String> FLUENT_LOGGING_ENTRY_METHODS = Set.of(
            "atTrace", "atDebug", "atInfo", "atWarn", "atError", "atLevel", "makeLoggingEventBuilder");

    @Test
    void hardenedProductionLogsMustUseOnlyFixedSemanticMessages() throws IOException {
        List<Violation> violations = new ArrayList<>();
        for (String file : HARDENED_PRODUCTION_FILES) {
            Path path = Path.of(file);
            assertTrue(Files.isRegularFile(path), () -> "目标 G 日志门禁文件不存在: " + path);
            violations.addAll(scan(path.toString(), Files.readString(path)));
        }
        assertTrue(violations.isEmpty(), () -> "目标 G 加固日志包含动态参数: " + violations);
    }

    @Test
    void scannerMustRejectTraditionalAndFluentDynamicLogging() {
        List<String> unsafeSources = List.of(
                "class Fixture { Sink AUDIT; void x(Throwable failure) { AUDIT.error(\"failed\", failure); } }",
                "class Fixture { Sink securityLog; void x(Exception problem) { securityLog.warn(\"failed\", problem); } }",
                "class Fixture { Sink audit; void x(String statement) { audit.debug(\"{}\", decorate(statement)); } }",
                "class Fixture { Sink audit; void x(String queryText) { String command = queryText; audit.info(command); } }",
                "class Fixture { Sink loginLog; void x(Credential credential) { loginLog.error(\"{}\", credential.getPassword()); } }",
                "class Fixture { Sink anyName; void x(Config properties) { anyName.debug(\"{}\", wrap(properties.getUrl())); } }");

        for (int index = 0; index < unsafeSources.size(); index++) {
            assertFalse(scan("UnsafeFixture" + index + ".java", unsafeSources.get(index)).isEmpty(),
                    "动态日志夹具未被受控集合门禁识别: " + index);
        }

        List<String> fluentSources = List.of(
                "class Fixture { Sink log; void x(Credential credential) { log.atInfo().addArgument(credential.getPassword()).log(\"login failed\"); } }",
                "class Fixture { Sink log; void x(Config source) { log.atDebug().addArgument(source.getUrl()).log(\"source failed\"); } }",
                "class Fixture { Sink log; void x(String statement) { log.atWarn().addArgument(statement).log(\"query failed\"); } }",
                "class Fixture { Sink log; void x(Throwable failure) { log.atError().setCause(failure).log(\"failed\"); } }",
                "class Fixture { Sink log; void x(Level level, String message) { log.atLevel(level).log(message); } }",
                "class Fixture { Sink log; void x(Level level, String message) { log.makeLoggingEventBuilder(level).log(message); } }");

        for (int index = 0; index < fluentSources.size(); index++) {
            List<Violation> violations = scan("FluentUnsafeFixture" + index + ".java", fluentSources.get(index));
            assertTrue(violations.stream()
                            .anyMatch(violation -> violation.category() == ViolationCategory.FLUENT_LOGGING_BUILDER),
                    "Fluent 日志入口未被受控集合门禁识别: " + index);
        }
    }

    @Test
    void scannerMustAllowOnlyCompileTimeFixedSemanticMessages() {
        String source = """
                class SafeFixture {
                    Sink AUDIT;
                    void x() {
                        AUDIT.debug("Executing paged read-only query");
                        AUDIT.info("Fixed " + "semantic status");
                    }
                }
                """;

        assertTrue(scan("SafeFixture.java", source).isEmpty());
    }

    private static List<Violation> scan(String fileName, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("当前运行时缺少 Java 编译器");
        }
        SourceFile sourceFile = new SourceFile(fileName, source);
        try {
            JavacTask task = (JavacTask) compiler.getTask(null, null, null,
                    List.of("-proc:none"), null, List.of(sourceFile));
            List<Violation> violations = new ArrayList<>();
            Trees trees = Trees.instance(task);
            for (CompilationUnitTree unit : task.parse()) {
                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
                        long position = trees.getSourcePositions().getStartPosition(unit, invocation);
                        long line = position < 0 ? 0 : unit.getLineMap().getLineNumber(position);
                        if (isFluentLoggingEntry(invocation)) {
                            violations.add(new Violation(fileName, line,
                                    ViolationCategory.FLUENT_LOGGING_BUILDER));
                        } else if (isLoggingLevelCall(invocation)) {
                            if (invocation.getArguments().stream().anyMatch(argument -> !isFixedString(argument))) {
                                violations.add(new Violation(fileName, line, ViolationCategory.DYNAMIC_LOG_ARGUMENT));
                            }
                        }
                        return super.visitMethodInvocation(invocation, unused);
                    }
                }.scan(unit, null);
            }
            return List.copyOf(violations);
        } catch (IOException exception) {
            throw new IllegalStateException("Java 日志结构解析失败: " + fileName, exception);
        }
    }

    private static boolean isLoggingLevelCall(MethodInvocationTree invocation) {
        return invocation.getMethodSelect() instanceof MemberSelectTree select
                && LOG_METHODS.contains(select.getIdentifier().toString());
    }

    private static boolean isFluentLoggingEntry(MethodInvocationTree invocation) {
        Tree method = invocation.getMethodSelect();
        String methodName = method instanceof MemberSelectTree select
                ? select.getIdentifier().toString()
                : method instanceof IdentifierTree identifier ? identifier.getName().toString() : "";
        return FLUENT_LOGGING_ENTRY_METHODS.contains(methodName);
    }

    private static boolean isFixedString(Tree expression) {
        if (expression instanceof LiteralTree literal) {
            return literal.getValue() instanceof String;
        }
        if (expression instanceof ParenthesizedTree parenthesized) {
            return isFixedString(parenthesized.getExpression());
        }
        return expression instanceof BinaryTree binary && binary.getKind() == Tree.Kind.PLUS
                && isFixedString(binary.getLeftOperand()) && isFixedString(binary.getRightOperand());
    }

    private enum ViolationCategory { DYNAMIC_LOG_ARGUMENT, FLUENT_LOGGING_BUILDER }

    private record Violation(String file, long line, ViolationCategory category) {
    }

    private static final class SourceFile extends SimpleJavaFileObject {
        private final String source;

        private SourceFile(String fileName, String source) {
            super(URI.create("string:///" + fileName.replace('\\', '/')), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}

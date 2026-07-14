package yubi.server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentDocumentationTest {

    private static final String SESSION_SECRET = "YUBI_SHARE_DOWNLOAD_SESSION_SECRET";
    private static final Pattern LOCAL_MARKDOWN_LINK = Pattern.compile(
            "\\[[^]]+]\\((\\./[^)#\\s]+)(?:#[^)]*)?\\)"
    );
    private static final Pattern FIXED_SESSION_SECRET = Pattern.compile(
            SESSION_SECRET + "\\s*=\\s*[\\\"']?[A-Za-z0-9+/_-]{43,}={0,2}"
    );
    private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void shouldDocumentRequiredSecretForEverySupportedStartupPath() throws IOException {
        String deployment = read("Deployment.md");
        String dockerfile = read("Dockerfile");
        String commonApplicationConfig = read("server/src/main/resources/application.yml");
        String applicationConfig = read("config/profiles/application-config.yml");
        String healthCheck = read("scripts/check-demo-health.sh");

        List<String> yubiDockerCommands = deployment.lines()
                .filter(line -> line.contains("docker run") && line.contains("yubi/yu-bi"))
                .toList();
        assertEquals(3, yubiDockerCommands.size());
        assertTrue(yubiDockerCommands.stream().allMatch(line -> line.contains(
                "-e " + SESSION_SECRET + " "
        )));
        assertTrue(yubiDockerCommands.stream().noneMatch(line -> line.contains(
                SESSION_SECRET + "="
        )));

        assertTrue(deployment.contains("openssl rand -base64 32"));
        assertTrue(deployment.contains("解码后至少 32 字节"));
        assertTrue(deployment.contains("同一集群的全部实例"));
        assertTrue(deployment.contains("密钥轮换会立即撤销"));
        assertTrue(deployment.contains("./bin/yu-bi-server.sh start"));
        assertTrue(deployment.contains("demo H2 数据库已内置下载任务安全 Schema"));
        assertTrue(deployment.contains("原地升级编排属于 Deferred"));
        assertTrue(deployment.contains("scripts/check-demo-health.sh"));
        for (String removedPromise : List.of(
                "scripts/upgrade-demo-download-schema.sh",
                "JNA",
                "openat",
                "原子相对打开",
                "安全拒绝下载文件读写",
                "独立锁",
                "候选文件",
                "候选库",
                "原库哈希",
                "原子替换"
        )) {
            assertFalse(deployment.contains(removedPromise), "发布文档仍包含已移出承诺: " + removedPromise);
        }
        assertTrue(deployment.contains("MySQL 8.4.10"));
        assertFalse(deployment.toLowerCase().contains("mysql5.7"));
        assertTrue(dockerfile.contains(SESSION_SECRET + "=\"\""));
        assertTrue(commonApplicationConfig.contains("${" + SESSION_SECRET + ":}"));
        assertTrue(applicationConfig.contains("${" + SESSION_SECRET + ":}"));
        assertTrue(healthCheck.contains(SESSION_SECRET + "=\"${SESSION_SECRET}\" java"));
        assertFalse(healthCheck.contains("-Dyubi.security.share-download-session.secret"));

        String publishedExamples = deployment + dockerfile + commonApplicationConfig
                + applicationConfig + healthCheck;
        assertFalse(FIXED_SESSION_SECRET.matcher(publishedExamples).find());
    }

    @Test
    void shouldKeepReadmeLinksValidWithoutPublishingPrivateDocs() throws IOException {
        assertReadmeLinksValid(REPOSITORY_ROOT);
        assertTrue(read(REPOSITORY_ROOT, ".gitignore").lines().anyMatch("/docs/"::equals));
    }

    @Test
    void shouldValidatePublishedReadmesWithoutPrivateDocs(@TempDir Path publishedRoot)
            throws IOException {
        Files.copy(REPOSITORY_ROOT.resolve(".gitignore"), publishedRoot.resolve(".gitignore"));
        for (String readmeName : List.of("README.md", "README_zh.md")) {
            String readme = read(REPOSITORY_ROOT, readmeName);
            Files.writeString(publishedRoot.resolve(readmeName), readme);

            Matcher matcher = LOCAL_MARKDOWN_LINK.matcher(readme);
            while (matcher.find()) {
                Path source = REPOSITORY_ROOT.resolve(matcher.group(1)).normalize();
                Path target = publishedRoot.resolve(matcher.group(1)).normalize();
                Files.createDirectories(target.getParent());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        assertFalse(Files.exists(publishedRoot.resolve("docs")));
        assertReadmeLinksValid(publishedRoot);
        assertTrue(read(publishedRoot, ".gitignore").lines().anyMatch("/docs/"::equals));
    }

    private String read(String relativePath) throws IOException {
        return read(REPOSITORY_ROOT, relativePath);
    }

    private String read(Path root, String relativePath) throws IOException {
        return Files.readString(root.resolve(relativePath));
    }

    private void assertReadmeLinksValid(Path root) throws IOException {
        for (String readmeName : List.of("README.md", "README_zh.md")) {
            String readme = read(root, readmeName);
            assertFalse(readme.contains("docs/"));

            Matcher matcher = LOCAL_MARKDOWN_LINK.matcher(readme);
            while (matcher.find()) {
                Path target = root.resolve(matcher.group(1)).normalize();
                assertTrue(Files.exists(target), readmeName + " 本地链接不存在: " + matcher.group(1));
            }
        }
    }
}

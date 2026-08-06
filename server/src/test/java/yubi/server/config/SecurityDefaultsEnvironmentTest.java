package yubi.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityDefaultsEnvironmentTest {

    private static final String APPLICATION_CONFIG = "config/profiles/application-config.yml";
    private static final String TOKEN_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void config配置缺少令牌密钥时应拒绝启动() throws IOException {
        ConfigurableEnvironment environment = configEnvironment();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new CustomPropertiesValidate().postProcessEnvironment(environment, new SpringApplication()));

        assertEquals("非 demo 环境必须通过 YUBI_SECURITY_TOKEN_SECRET 配置至少 32 字节的令牌密钥",
                failure.getMessage());
    }

    @Test
    void config配置提供令牌密钥时应采用安全账户默认值() throws IOException {
        ConfigurableEnvironment environment = configEnvironment(TOKEN_SECRET);

        new CustomPropertiesValidate().postProcessEnvironment(environment, new SpringApplication());

        assertEquals(TOKEN_SECRET, environment.getProperty("yubi.security.token.secret"));
        assertFalse(environment.getProperty("yubi.user.register", Boolean.class, true));
        assertTrue(environment.getProperty("yubi.user.admin-username", "").isBlank());
    }

    @Test
    void config配置令牌密钥短于三十二字节时应拒绝启动() throws IOException {
        ConfigurableEnvironment environment = configEnvironment("too-short");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new CustomPropertiesValidate().postProcessEnvironment(environment, new SpringApplication()));

        assertEquals("非 demo 环境必须通过 YUBI_SECURITY_TOKEN_SECRET 配置至少 32 字节的令牌密钥",
                failure.getMessage());
    }

    @Test
    void config配置应允许环境变量显式开启注册() throws IOException {
        ConfigurableEnvironment environment = configEnvironment(TOKEN_SECRET);
        environment.getPropertySources().addFirst(new MapPropertySource(
                "register-override", Map.of("YUBI_USER_REGISTER", "true")));

        new CustomPropertiesValidate().postProcessEnvironment(environment, new SpringApplication());

        assertTrue(environment.getProperty("yubi.user.register", Boolean.class, false));
    }

    @Test
    void config配置不应包含公开数据库或Ssl密码() throws IOException {
        ConfigurableEnvironment environment = configEnvironment(TOKEN_SECRET);

        new CustomPropertiesValidate().postProcessEnvironment(environment, new SpringApplication());

        assertTrue(environment.getProperty("spring.datasource.username", "").isBlank());
        assertTrue(environment.getProperty("spring.datasource.password", "").isBlank());
        assertTrue(environment.getProperty("server.ssl.key-store-password", "").isBlank());
    }

    @Test
    void demo配置应保持演示能力且不自动提升管理员() throws IOException {
        ConfigurableEnvironment environment = demoEnvironment();

        new CustomPropertiesValidate().postProcessEnvironment(environment, new SpringApplication());

        assertTrue(environment.getProperty("yubi.user.register", Boolean.class, false));
        assertTrue(environment.getProperty("yubi.user.admin-username", "").isBlank());
        assertTrue(environment.getProperty("yubi.security.token.secret", "").length() > 0);
        assertTrue(environment.getRequiredProperty("spring.datasource.url").contains("IFEXISTS=TRUE"));
    }

    @Test
    void demo配置默认应仅监听本机地址() throws IOException {
        ConfigurableEnvironment environment = demoEnvironment();

        new CustomPropertiesValidate().postProcessEnvironment(environment, new SpringApplication());

        assertEquals("127.0.0.1", environment.getProperty("server.address"));
    }

    private ConfigurableEnvironment configEnvironment() throws IOException {
        return configEnvironment(null);
    }

    private ConfigurableEnvironment configEnvironment(String tokenSecret) throws IOException {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("datasource.ip", "127.0.0.1");
        if (tokenSecret != null) {
            environment.withProperty("YUBI_SECURITY_TOKEN_SECRET", tokenSecret);
        }
        environment.setActiveProfiles("config", "test");
        var propertySources = new YamlPropertySourceLoader()
                .load("application-config", new FileSystemResource(projectFile(APPLICATION_CONFIG)));
        propertySources.forEach(environment.getPropertySources()::addLast);
        addShippedYuBiConfig(environment);
        return environment;
    }

    private ConfigurableEnvironment demoEnvironment() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("demo", "test");
        var propertySources = new YamlPropertySourceLoader()
                .load("application-demo", new ClassPathResource("application-demo.yml"));
        propertySources.forEach(environment.getPropertySources()::addLast);
        addShippedYuBiConfig(environment);
        return environment;
    }

    private void addShippedYuBiConfig(ConfigurableEnvironment environment) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(projectFile("config/yubi.conf"))) {
            properties.load(inputStream);
        }
        properties.entrySet().removeIf(entry -> entry.getValue().toString().isBlank());
        environment.getPropertySources().addFirst(new PropertiesPropertySource("shipped-yubi-config", properties));
    }

    private Path projectFile(String relativePath) {
        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        Path candidate = workingDirectory.resolve(relativePath);
        if (Files.exists(candidate)) {
            return candidate;
        }
        return workingDirectory.getParent().resolve(relativePath);
    }
}

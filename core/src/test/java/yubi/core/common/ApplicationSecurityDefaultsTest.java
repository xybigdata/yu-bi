package yubi.core.common;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationSecurityDefaultsTest {

    @Test
    void 未配置注册开关时应默认关闭注册() {
        useEnvironment(new MockEnvironment());

        assertFalse(Application.canRegister());
    }

    @Test
    void 未配置令牌密钥时不应使用公开默认值() {
        useEnvironment(new MockEnvironment());

        IllegalStateException failure = assertThrows(IllegalStateException.class, Application::getTokenSecret);

        assertEquals("必须通过 YUBI_SECURITY_TOKEN_SECRET 配置至少 32 字节的令牌密钥", failure.getMessage());
    }

    @Test
    void 令牌密钥短于三十二字节时应拒绝使用() {
        useEnvironment(new MockEnvironment()
                .withProperty("yubi.security.token.secret", "too-short"));

        IllegalStateException failure = assertThrows(IllegalStateException.class, Application::getTokenSecret);

        assertEquals("必须通过 YUBI_SECURITY_TOKEN_SECRET 配置至少 32 字节的令牌密钥", failure.getMessage());
    }

    private void useEnvironment(MockEnvironment environment) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);
        new Application().setApplicationContext(context);
    }
}

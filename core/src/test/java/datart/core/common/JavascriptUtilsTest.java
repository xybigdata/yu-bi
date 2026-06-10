package datart.core.common;

import datart.core.base.exception.BaseException;
import org.junit.jupiter.api.Test;

import javax.script.Invocable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptUtilsTest {

    @Test
    void shouldLoadJavascriptResourceAndInvokeFunction() throws Exception {
        Invocable invocable = JavascriptUtils.load("javascript/test-parser.js");

        Object result = JavascriptUtils.invoke(invocable, "joinWords", "hello", "datart");

        assertEquals("hello-datart", result);
    }

    @Test
    void shouldFailFastWhenConfiguredEngineIsUnavailable() {
        String previousEngineName = System.getProperty(JavascriptUtils.SCRIPT_ENGINE_PROPERTY);
        System.setProperty(JavascriptUtils.SCRIPT_ENGINE_PROPERTY, "missing-engine");

        try {
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> JavascriptUtils.load("javascript/test-parser.js")
            );
            assertTrue(exception.getMessage().contains("missing-engine"));
        } finally {
            if (previousEngineName == null) {
                System.clearProperty(JavascriptUtils.SCRIPT_ENGINE_PROPERTY);
            } else {
                System.setProperty(JavascriptUtils.SCRIPT_ENGINE_PROPERTY, previousEngineName);
            }
        }
    }
}

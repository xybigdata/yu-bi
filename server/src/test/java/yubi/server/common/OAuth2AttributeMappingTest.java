package yubi.server.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuth2AttributeMappingTest {

    @Test
    void shouldReadNestedStringAttribute() {
        Map<String, Object> attributes = Map.of(
                "profile", Map.of(
                        "name", "alice",
                        "contacts", Map.of("email", "alice@example.com")
                )
        );

        assertEquals("alice", OAuth2AttributeMapping.readString(attributes, "$.profile.name"));
        assertEquals("alice@example.com", OAuth2AttributeMapping.readString(attributes, "$.profile.contacts.email"));
    }

    @Test
    void shouldIgnoreMissingOptionalMapping() {
        assertNull(OAuth2AttributeMapping.readOptionalString(Map.of("email", "alice@example.com"), null));
    }

    @Test
    void shouldKeepStringCastBehaviorForNonStringAttribute() {
        Map<String, Object> attributes = Map.of("profile", Map.of("age", 18));

        assertThrows(ClassCastException.class,
                () -> OAuth2AttributeMapping.readString(attributes, "$.profile.age"));
    }
}

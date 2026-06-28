package yubi.server.common;

import com.jayway.jsonpath.JsonPath;

public final class OAuth2AttributeMapping {

    private OAuth2AttributeMapping() {
    }

    public static String readString(Object attributeDocument, String mapping) {
        return JsonPath.read(attributeDocument, mapping);
    }

    public static String readOptionalString(Object attributeDocument, String mapping) {
        if (mapping == null) {
            return null;
        }
        return readString(attributeDocument, mapping);
    }
}

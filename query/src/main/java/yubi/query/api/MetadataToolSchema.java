package yubi.query.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MetadataToolSchema(String name,
                                 String description,
                                 Schema inputSchema,
                                 Schema outputSchema,
                                 boolean readOnly) {

    public record Schema(Type type,
                         String description,
                         Map<String, Schema> properties,
                         List<String> required,
                         Schema items,
                         List<String> enumValues) {
        public Schema {
            properties = properties == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            required = required == null ? List.of() : List.copyOf(required);
            enumValues = enumValues == null ? List.of() : List.copyOf(enumValues);
        }

        public static Schema object(String description, Map<String, Schema> properties, List<String> required) {
            return new Schema(Type.OBJECT, description, properties, required, null, List.of());
        }

        public static Schema array(String description, Schema items) {
            return new Schema(Type.ARRAY, description, Map.of(), List.of(), items, List.of());
        }

        public static Schema string(String description) {
            return new Schema(Type.STRING, description, Map.of(), List.of(), null, List.of());
        }

        public static Schema enumeration(String description, List<String> values) {
            return new Schema(Type.STRING, description, Map.of(), List.of(), null, values);
        }

        public static Schema integer(String description) {
            return new Schema(Type.INTEGER, description, Map.of(), List.of(), null, List.of());
        }

        public static Schema bool(String description) {
            return new Schema(Type.BOOLEAN, description, Map.of(), List.of(), null, List.of());
        }
    }

    public enum Type { OBJECT, ARRAY, STRING, INTEGER, BOOLEAN }
}

package yubi.agent.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 模型、工具与运行时之间使用的不可变结构化值。 */
public sealed interface StructuredValue permits StructuredValue.ObjectValue,
        StructuredValue.ArrayValue, StructuredValue.TextValue, StructuredValue.IntegerValue,
        StructuredValue.BooleanValue, StructuredValue.NullValue {

    record ObjectValue(Map<String, StructuredValue> values) implements StructuredValue {
        public ObjectValue {
            values = values == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(values));
            if (values.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
                throw new IllegalArgumentException("结构化对象不能包含空键或空值");
            }
        }
    }

    record ArrayValue(List<StructuredValue> values) implements StructuredValue {
        public ArrayValue {
            values = values == null ? List.of() : List.copyOf(values);
        }
    }

    record TextValue(String value) implements StructuredValue {
        public TextValue {
            java.util.Objects.requireNonNull(value, "value");
        }
    }

    record IntegerValue(long value) implements StructuredValue {
    }

    record BooleanValue(boolean value) implements StructuredValue {
    }

    enum NullValue implements StructuredValue {
        INSTANCE
    }

    static ObjectValue object(Map<String, StructuredValue> values) {
        return new ObjectValue(values);
    }

    static ArrayValue array(List<StructuredValue> values) {
        return new ArrayValue(values);
    }

    static TextValue text(String value) {
        return new TextValue(value);
    }

    static IntegerValue integer(long value) {
        return new IntegerValue(value);
    }

    static BooleanValue bool(boolean value) {
        return new BooleanValue(value);
    }
}

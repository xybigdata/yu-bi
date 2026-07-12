package yubi.agent.application;

import yubi.agent.api.ToolInputException;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.BooleanValue;
import yubi.agent.domain.StructuredValue.IntegerValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.StructuredValue.TextValue;

import java.util.List;
import java.util.Set;

final class ToolArgumentReader {

    private final ObjectValue source;

    ToolArgumentReader(ObjectValue source) {
        if (source == null) {
            throw invalid("工具参数必须是对象");
        }
        this.source = source;
    }

    ToolArgumentReader exact(Set<String> allowed) {
        if (!allowed.containsAll(source.values().keySet())) {
            throw invalid("工具参数包含未允许字段");
        }
        return this;
    }

    String requiredText(String name, int maximumLength) {
        String value = optionalText(name, maximumLength);
        if (value == null || value.isBlank()) {
            throw invalid("工具必填文本参数无效");
        }
        return value.trim();
    }

    String optionalText(String name, int maximumLength) {
        StructuredValue value = source.values().get(name);
        if (value == null || value == StructuredValue.NullValue.INSTANCE) {
            return null;
        }
        if (!(value instanceof TextValue text) || text.value() == null || text.value().length() > maximumLength) {
            throw invalid("工具文本参数无效");
        }
        return text.value();
    }

    long optionalInteger(String name, long defaultValue, long minimum, long maximum) {
        StructuredValue value = source.values().get(name);
        if (value == null || value == StructuredValue.NullValue.INSTANCE) {
            return defaultValue;
        }
        if (!(value instanceof IntegerValue number) || number.value() < minimum || number.value() > maximum) {
            throw invalid("工具整数参数无效");
        }
        return number.value();
    }

    boolean optionalBoolean(String name, boolean defaultValue) {
        StructuredValue value = source.values().get(name);
        if (value == null || value == StructuredValue.NullValue.INSTANCE) {
            return defaultValue;
        }
        if (!(value instanceof BooleanValue bool)) {
            throw invalid("工具布尔参数无效");
        }
        return bool.value();
    }

    List<StructuredValue> array(String name, int maximumSize) {
        StructuredValue value = source.values().get(name);
        if (value == null || value == StructuredValue.NullValue.INSTANCE) {
            return List.of();
        }
        if (!(value instanceof ArrayValue array) || array.values().size() > maximumSize
                || array.values().stream().anyMatch(java.util.Objects::isNull)) {
            throw invalid("工具数组参数无效");
        }
        return array.values();
    }

    ObjectValue optionalObject(String name) {
        StructuredValue value = source.values().get(name);
        if (value == null || value == StructuredValue.NullValue.INSTANCE) {
            return new ObjectValue(java.util.Map.of());
        }
        if (!(value instanceof ObjectValue object)) {
            throw invalid("工具对象参数无效");
        }
        return object;
    }

    static ObjectValue object(StructuredValue value) {
        if (!(value instanceof ObjectValue object)) {
            throw invalid("工具数组元素必须是对象");
        }
        return object;
    }

    static String text(StructuredValue value, int maximumLength) {
        if (!(value instanceof TextValue text) || text.value() == null || text.value().isBlank()
                || text.value().length() > maximumLength) {
            throw invalid("工具数组文本元素无效");
        }
        return text.value().trim();
    }

    static ToolInputException invalid(String message) {
        return new ToolInputException("INVALID_TOOL_INPUT", message);
    }
}

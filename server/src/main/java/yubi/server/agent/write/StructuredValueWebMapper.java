package yubi.server.agent.write;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.BooleanValue;
import yubi.agent.domain.StructuredValue.IntegerValue;
import yubi.agent.domain.StructuredValue.NullValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.StructuredValue.TextValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public final class StructuredValueWebMapper {

    private static final int MAXIMUM_DEPTH = 8;
    private static final int MAXIMUM_NODES = 128;
    private static final int MAXIMUM_FIELD_LENGTH = 64;

    private final tools.jackson.databind.ObjectMapper objectMapper = JsonMapper.builder().build();

    public ObjectValue toObject(JsonNode value) {
        Counter counter = new Counter();
        StructuredValue mapped = map(value, 0, counter);
        if (!(mapped instanceof ObjectValue object)) {
            throw invalid();
        }
        return object;
    }

    public String write(ObjectValue value) {
        try {
            return objectMapper.writeValueAsString(toPlainValue(value));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("受控写参数序列化失败");
        }
    }

    public ObjectValue read(String value) {
        try {
            return toObject(objectMapper.readTree(value));
        } catch (AgentWriteWebException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("受控写参数反序列化失败");
        }
    }

    private StructuredValue map(JsonNode value, int depth, Counter counter) {
        if (value == null || depth > MAXIMUM_DEPTH || ++counter.nodes > MAXIMUM_NODES) {
            throw invalid();
        }
        if (value.isNull()) {
            return NullValue.INSTANCE;
        }
        if (value.isTextual()) {
            return new TextValue(value.textValue());
        }
        if (value.isBoolean()) {
            return new BooleanValue(value.booleanValue());
        }
        if (value.isIntegralNumber() && value.canConvertToLong()) {
            return new IntegerValue(value.longValue());
        }
        if (value.isArray()) {
            List<StructuredValue> values = new ArrayList<>();
            for (JsonNode item : value) {
                values.add(map(item, depth + 1, counter));
            }
            return new ArrayValue(values);
        }
        if (value.isObject()) {
            Map<String, StructuredValue> values = new LinkedHashMap<>();
            value.properties().forEach(entry -> {
                if (entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getKey().length() > MAXIMUM_FIELD_LENGTH) {
                    throw invalid();
                }
                values.put(entry.getKey(), map(entry.getValue(), depth + 1, counter));
            });
            return new ObjectValue(values);
        }
        throw invalid();
    }

    private Object toPlainValue(StructuredValue value) {
        if (value == NullValue.INSTANCE) {
            return null;
        }
        if (value instanceof TextValue text) {
            return text.value();
        }
        if (value instanceof IntegerValue integer) {
            return integer.value();
        }
        if (value instanceof BooleanValue bool) {
            return bool.value();
        }
        if (value instanceof ArrayValue array) {
            return array.values().stream().map(this::toPlainValue).toList();
        }
        ObjectValue object = (ObjectValue) value;
        Map<String, Object> values = new LinkedHashMap<>();
        object.values().forEach((key, item) -> values.put(key, toPlainValue(item)));
        return values;
    }

    private AgentWriteWebException invalid() {
        return new AgentWriteWebException("INVALID_WRITE_REQUEST", "受控写请求参数无效");
    }

    private static final class Counter {
        private int nodes;
    }
}

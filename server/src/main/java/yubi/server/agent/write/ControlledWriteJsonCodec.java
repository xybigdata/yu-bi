package yubi.server.agent.write;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.agent.domain.ControlledWriteModels.CreateChartAction;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.RenameDashboardAction;
import yubi.agent.domain.ControlledWriteModels.WriteAction;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.StructuredValue.ObjectValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 只接受首批受控写协议，避免 Jackson 类型信息或宽松绑定扩大工具边界。 */
final class ControlledWriteJsonCodec {

    private static final int FORMAT_VERSION = 1;
    private static final int MAXIMUM_JSON_LENGTH = 65_536;
    private static final int MAXIMUM_PREVIEW_ITEMS = 32;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final StructuredValueWebMapper structuredValueMapper;

    ControlledWriteJsonCodec(StructuredValueWebMapper structuredValueMapper) {
        this.structuredValueMapper = java.util.Objects.requireNonNull(structuredValueMapper,
                "structuredValueMapper");
    }

    String writeArguments(ObjectValue arguments) {
        return safelyWrite(() -> structuredValueMapper.write(arguments));
    }

    ObjectValue readArguments(String json) {
        requireJsonSize(json);
        try {
            return structuredValueMapper.read(json);
        } catch (RuntimeException exception) {
            throw damaged();
        }
    }

    String writePrepared(PreparedWrite prepared) {
        return safelyWrite(() -> objectMapper.writeValueAsString(preparedMap(prepared)));
    }

    String writePreview(WritePreview preview) {
        return safelyWrite(() -> objectMapper.writeValueAsString(previewMap(preview)));
    }

    PreparedWrite readPrepared(String preparedJson, String previewJson, String toolName) {
        requireJsonSize(preparedJson);
        requireJsonSize(previewJson);
        try {
            JsonNode root = object(objectMapper.readTree(preparedJson));
            exactFields(root, Set.of("version", "action", "expectedStateDigest", "preview"));
            JsonNode version = root.get("version");
            if (!version.isIntegralNumber() || version.intValue() != FORMAT_VERSION) {
                throw damaged();
            }
            WriteAction action = readAction(root.get("action"));
            if (!action.toolName().equals(toolName)) {
                throw damaged();
            }
            String expectedStateDigest = text(root, "expectedStateDigest", 256);
            WritePreview embeddedPreview = readPreview(root.get("preview"));
            WritePreview separatePreview = readPreview(objectMapper.readTree(previewJson));
            if (!embeddedPreview.equals(separatePreview)
                    || !expectedResourceType(action).equals(embeddedPreview.resourceType())) {
                throw damaged();
            }
            return new PreparedWrite(action, expectedStateDigest, embeddedPreview);
        } catch (RuntimeException exception) {
            throw damaged();
        }
    }

    String resourceType(WriteAction action) {
        return expectedResourceType(action);
    }

    String changeAction(WriteAction action) {
        if (action instanceof CreateChartAction) {
            return "CREATE";
        }
        if (action instanceof RenameDashboardAction) {
            return "RENAME";
        }
        throw new IllegalArgumentException("未注册的受控写操作");
    }

    private Map<String, Object> preparedMap(PreparedWrite prepared) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("version", FORMAT_VERSION);
        value.put("action", actionMap(prepared.action()));
        value.put("expectedStateDigest", prepared.expectedStateDigest());
        value.put("preview", previewMap(prepared.preview()));
        return value;
    }

    private Map<String, Object> actionMap(WriteAction action) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("type", action.toolName());
        if (action instanceof CreateChartAction create) {
            value.put("name", create.name());
            value.put("viewId", create.viewId());
            putIfPresent(value, "parentId", create.parentId());
            putIfPresent(value, "description", create.description());
            return value;
        }
        if (action instanceof RenameDashboardAction rename) {
            value.put("dashboardId", rename.dashboardId());
            value.put("newName", rename.newName());
            return value;
        }
        throw new IllegalArgumentException("未注册的受控写操作");
    }

    private Map<String, Object> previewMap(WritePreview preview) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("title", preview.title());
        value.put("resourceType", preview.resourceType());
        putIfPresent(value, "targetId", preview.targetId());
        value.put("fields", preview.fields().stream().map(this::previewFieldMap).toList());
        value.put("impacts", preview.impacts());
        return value;
    }

    private Map<String, Object> previewFieldMap(PreviewField field) {
        return Map.of("label", field.label(), "value", field.value());
    }

    private WriteAction readAction(JsonNode value) {
        JsonNode action = object(value);
        String type = text(action, "type", 32);
        if ("create_chart".equals(type)) {
            exactFields(action, Set.of("type", "name", "viewId"),
                    Set.of("parentId", "description"));
            return new CreateChartAction(text(action, "name", 255),
                    text(action, "viewId", 128), optionalText(action, "parentId", 128),
                    optionalText(action, "description", 255));
        }
        if ("rename_dashboard".equals(type)) {
            exactFields(action, Set.of("type", "dashboardId", "newName"));
            return new RenameDashboardAction(text(action, "dashboardId", 128),
                    text(action, "newName", 255));
        }
        throw damaged();
    }

    private WritePreview readPreview(JsonNode value) {
        JsonNode preview = object(value);
        exactFields(preview, Set.of("title", "resourceType", "fields", "impacts"), Set.of("targetId"));
        JsonNode fieldsNode = array(preview.get("fields"));
        JsonNode impactsNode = array(preview.get("impacts"));
        if (fieldsNode.size() > MAXIMUM_PREVIEW_ITEMS || impactsNode.size() > MAXIMUM_PREVIEW_ITEMS) {
            throw damaged();
        }
        List<PreviewField> fields = new ArrayList<>();
        for (JsonNode fieldValue : fieldsNode) {
            JsonNode field = object(fieldValue);
            exactFields(field, Set.of("label", "value"));
            fields.add(new PreviewField(text(field, "label", 255), text(field, "value", 2_048)));
        }
        List<String> impacts = new ArrayList<>();
        for (JsonNode impact : impactsNode) {
            impacts.add(text(impact, 2_048));
        }
        return new WritePreview(text(preview, "title", 255),
                text(preview, "resourceType", 32), optionalText(preview, "targetId", 128), fields, impacts);
    }

    private String expectedResourceType(WriteAction action) {
        if (action instanceof CreateChartAction) {
            return "CHART";
        }
        if (action instanceof RenameDashboardAction) {
            return "DASHBOARD";
        }
        throw new IllegalArgumentException("未注册的受控写操作");
    }

    private JsonNode object(JsonNode value) {
        if (value == null || !value.isObject()) {
            throw damaged();
        }
        return value;
    }

    private JsonNode array(JsonNode value) {
        if (value == null || !value.isArray()) {
            throw damaged();
        }
        return value;
    }

    private void exactFields(JsonNode value, Set<String> required) {
        exactFields(value, required, Set.of());
    }

    private void exactFields(JsonNode value, Set<String> required, Set<String> optional) {
        Set<String> allowed = new java.util.HashSet<>(required);
        allowed.addAll(optional);
        Set<String> found = new java.util.HashSet<>();
        value.properties().forEach(entry -> {
            if (!allowed.contains(entry.getKey())) {
                throw damaged();
            }
            found.add(entry.getKey());
        });
        if (!found.containsAll(required)) {
            throw damaged();
        }
    }

    private String text(JsonNode object, String field, int maximumLength) {
        return text(object.get(field), maximumLength);
    }

    private String text(JsonNode value, int maximumLength) {
        if (value == null || !value.isString()) {
            throw damaged();
        }
        String result = value.stringValue();
        if (result == null || result.isBlank() || result.length() > maximumLength) {
            throw damaged();
        }
        return result;
    }

    private String optionalText(JsonNode object, String field, int maximumLength) {
        JsonNode value = object.get(field);
        return value == null ? null : text(value, maximumLength);
    }

    private void putIfPresent(Map<String, Object> value, String name, String fieldValue) {
        if (fieldValue != null) {
            value.put(name, fieldValue);
        }
    }

    private String safelyWrite(JsonWriter writer) {
        try {
            String value = writer.write();
            requireJsonSize(value);
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("受控写记录序列化失败");
        }
    }

    private void requireJsonSize(String value) {
        if (value == null || value.isBlank() || value.length() > MAXIMUM_JSON_LENGTH) {
            throw damaged();
        }
    }

    private IllegalStateException damaged() {
        return new IllegalStateException("受控写持久记录损坏");
    }

    @FunctionalInterface
    private interface JsonWriter {
        String write();
    }
}

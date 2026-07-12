package yubi.agent.application;

import yubi.agent.domain.ControlledWriteModels.CreateChartAction;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.RenameDashboardAction;
import yubi.agent.domain.ControlledWriteModels.WriteAction;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.BooleanValue;
import yubi.agent.domain.StructuredValue.IntegerValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.StructuredValue.TextValue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WriteDigests {

    private WriteDigests() {
    }

    static String structured(ObjectValue value) {
        MessageDigest digest = sha256();
        append(digest, value);
        return hex(digest.digest());
    }

    static String idempotencyKey(String value) {
        MessageDigest digest = sha256();
        appendBytes(digest, "IDEMPOTENCY_KEY_V1".getBytes(StandardCharsets.UTF_8));
        appendBytes(digest, value.getBytes(StandardCharsets.UTF_8));
        return hex(digest.digest());
    }

    static String prepared(PreparedWrite prepared) {
        LinkedHashMap<String, StructuredValue> values = new LinkedHashMap<>();
        values.put("action", canonicalParameters(prepared.action()));
        values.put("expectedStateDigest", StructuredValue.text(prepared.expectedStateDigest()));
        values.put("preview", canonicalPreview(prepared));
        return structured(new ObjectValue(values));
    }

    static ObjectValue canonicalParameters(WriteAction action) {
        LinkedHashMap<String, StructuredValue> values = new LinkedHashMap<>();
        if (action instanceof CreateChartAction create) {
            values.put("name", StructuredValue.text(create.name()));
            values.put("viewId", StructuredValue.text(create.viewId()));
            if (create.parentId() != null) {
                values.put("parentId", StructuredValue.text(create.parentId()));
            }
            if (create.description() != null) {
                values.put("description", StructuredValue.text(create.description()));
            }
        } else if (action instanceof RenameDashboardAction rename) {
            values.put("dashboardId", StructuredValue.text(rename.dashboardId()));
            values.put("newName", StructuredValue.text(rename.newName()));
        } else {
            throw new IllegalArgumentException("未知写操作类型");
        }
        return new ObjectValue(values);
    }

    private static ObjectValue canonicalPreview(PreparedWrite prepared) {
        var preview = prepared.preview();
        LinkedHashMap<String, StructuredValue> values = new LinkedHashMap<>();
        values.put("title", StructuredValue.text(preview.title()));
        values.put("resourceType", StructuredValue.text(preview.resourceType()));
        if (preview.targetId() != null) {
            values.put("targetId", StructuredValue.text(preview.targetId()));
        }
        values.put("fields", StructuredValue.array(preview.fields().stream()
                .map(WriteDigests::canonicalPreviewField)
                .map(StructuredValue.class::cast)
                .toList()));
        values.put("impacts", StructuredValue.array(preview.impacts().stream()
                .map(StructuredValue::text)
                .map(StructuredValue.class::cast)
                .toList()));
        return new ObjectValue(values);
    }

    private static ObjectValue canonicalPreviewField(PreviewField field) {
        LinkedHashMap<String, StructuredValue> values = new LinkedHashMap<>();
        values.put("label", StructuredValue.text(field.label()));
        values.put("value", StructuredValue.text(field.value()));
        return new ObjectValue(values);
    }

    private static void append(MessageDigest digest, StructuredValue value) {
        if (value == StructuredValue.NullValue.INSTANCE) {
            digest.update((byte) 'N');
            return;
        }
        if (value instanceof TextValue text) {
            digest.update((byte) 'T');
            appendBytes(digest, text.value().getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (value instanceof IntegerValue integer) {
            digest.update((byte) 'I');
            appendBytes(digest, Long.toString(integer.value()).getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (value instanceof BooleanValue bool) {
            digest.update((byte) 'B');
            digest.update((byte) (bool.value() ? 1 : 0));
            return;
        }
        if (value instanceof ArrayValue array) {
            digest.update((byte) 'A');
            appendLength(digest, array.values().size());
            array.values().forEach(child -> append(digest, child));
            return;
        }
        ObjectValue object = (ObjectValue) value;
        digest.update((byte) 'O');
        List<Map.Entry<String, StructuredValue>> entries = new ArrayList<>(object.values().entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        appendLength(digest, entries.size());
        for (Map.Entry<String, StructuredValue> entry : entries) {
            appendBytes(digest, entry.getKey().getBytes(StandardCharsets.UTF_8));
            append(digest, entry.getValue());
        }
    }

    private static void appendBytes(MessageDigest digest, byte[] value) {
        appendLength(digest, value.length);
        digest.update(value);
    }

    private static void appendLength(MessageDigest digest, int length) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(length).array());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK 缺少 SHA-256", exception);
        }
    }

    private static String hex(byte[] value) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] result = new char[value.length * 2];
        for (int index = 0; index < value.length; index++) {
            int current = value[index] & 0xff;
            result[index * 2] = digits[current >>> 4];
            result[index * 2 + 1] = digits[current & 0xf];
        }
        return new String(result);
    }
}

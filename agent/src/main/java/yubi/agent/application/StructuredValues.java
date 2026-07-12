package yubi.agent.application;

import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.BooleanValue;
import yubi.agent.domain.StructuredValue.IntegerValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.StructuredValue.TextValue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class StructuredValues {

    private StructuredValues() {
    }

    static ObjectValue object(Object... entries) {
        Map<String, StructuredValue> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            StructuredValue value = (StructuredValue) entries[index + 1];
            if (value != null) {
                values.put((String) entries[index], value);
            }
        }
        return new ObjectValue(values);
    }

    static ArrayValue array(List<? extends StructuredValue> values) {
        return new ArrayValue(List.copyOf(values));
    }

    static TextValue text(String value) {
        return value == null ? null : new TextValue(value);
    }

    static IntegerValue integer(long value) {
        return new IntegerValue(value);
    }

    static BooleanValue bool(boolean value) {
        return new BooleanValue(value);
    }

    static long utf8Bytes(StructuredValue value) {
        if (value == null || value == StructuredValue.NullValue.INSTANCE) {
            return 4;
        }
        if (value instanceof TextValue text) {
            return textUtf8Bytes(text.value());
        }
        if (value instanceof IntegerValue integer) {
            return Long.toString(integer.value()).length();
        }
        if (value instanceof BooleanValue bool) {
            return bool.value() ? 4 : 5;
        }
        if (value instanceof ArrayValue array) {
            long bytes = 2;
            for (int index = 0; index < array.values().size(); index++) {
                bytes += utf8Bytes(array.values().get(index));
                if (index > 0) {
                    bytes++;
                }
            }
            return bytes;
        }
        ObjectValue object = (ObjectValue) value;
        long bytes = 2;
        int index = 0;
        for (Map.Entry<String, StructuredValue> entry : object.values().entrySet()) {
            bytes += textUtf8Bytes(entry.getKey()) + 1 + utf8Bytes(entry.getValue());
            if (index++ > 0) {
                bytes++;
            }
        }
        return bytes;
    }

    static long textUtf8Bytes(CharSequence value) {
        if (value == null) {
            return 4;
        }
        long bytes = 2;
        for (int offset = 0; offset < value.length();) {
            EncodedCharacter character = encodedCharacter(value, offset);
            bytes += character.bytes();
            offset += character.characters();
        }
        return bytes;
    }

    static BoundedText boundedText(String value, long maximumBytes) {
        return boundedText((CharSequence) value, maximumBytes);
    }

    static BoundedText boundedText(CharSequence value, long maximumBytes) {
        if (value == null) {
            return new BoundedText(null, 0, 0, false);
        }
        if (maximumBytes < 2) {
            throw new IllegalArgumentException("文本字节上限必须至少容纳空字符串");
        }
        long observed = textUtf8Bytes(value);
        if (observed <= maximumBytes) {
            String result = value instanceof String string ? string : value.toString();
            return new BoundedText(text(result), observed, observed, false);
        }
        StringBuilder bounded = new StringBuilder();
        long returned = 2;
        for (int offset = 0; offset < value.length();) {
            EncodedCharacter character = encodedCharacter(value, offset);
            if (returned + character.bytes() > maximumBytes) {
                break;
            }
            bounded.append(value, offset, offset + character.characters());
            returned += character.bytes();
            offset += character.characters();
        }
        return new BoundedText(text(bounded.toString()), observed, returned, true);
    }

    static BoundedText boundedCell(Object value, long maximumBytes) {
        if (value instanceof String text) {
            return boundedText(text, maximumBytes);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof Float || value instanceof Double
                || value instanceof Boolean || value instanceof Character) {
            return boundedText(String.valueOf(value), maximumBytes);
        }
        if (value instanceof BigInteger integer) {
            if (integer.bitLength() > 14_000) {
                return marker("<numeric:oversized>", maximumBytes, maximumBytes + 1);
            }
            return boundedText(integer.toString(), maximumBytes);
        }
        if (value instanceof BigDecimal decimal) {
            if (decimal.precision() > 4_096 || Math.abs((long) decimal.scale()) > 4_096) {
                return marker("<numeric:oversized>", maximumBytes, maximumBytes + 1);
            }
            return boundedText(decimal.toString(), maximumBytes);
        }
        if (value instanceof byte[] bytes) {
            return marker("<binary:" + bytes.length + " bytes>", maximumBytes,
                    Math.max(maximumBytes + 1, bytes.length));
        }
        if (value != null && value.getClass() == java.sql.Timestamp.class) {
            java.sql.Timestamp timestamp = (java.sql.Timestamp) value;
            return boundedText(timestamp.toLocalDateTime().toString(), maximumBytes);
        }
        if (value != null && value.getClass() == java.sql.Date.class) {
            java.sql.Date date = (java.sql.Date) value;
            return boundedText(date.toLocalDate().toString(), maximumBytes);
        }
        if (value != null && value.getClass() == java.sql.Time.class) {
            java.sql.Time time = (java.sql.Time) value;
            return boundedText(time.toLocalTime().toString(), maximumBytes);
        }
        if (value != null && value.getClass() == java.util.Date.class) {
            java.util.Date date = (java.util.Date) value;
            return boundedText(date.toInstant().toString(), maximumBytes);
        }
        if (value instanceof UUID uuid) {
            return boundedText(uuid.toString(), maximumBytes);
        }
        if (value != null && value.getClass().getPackageName().equals("java.time")) {
            return boundedText(String.valueOf(value), maximumBytes);
        }
        String type = value == null ? "null" : value.getClass().getName();
        return marker("<unsupported:" + type + ">", maximumBytes, maximumBytes + 1);
    }

    private static BoundedText marker(String value, long maximumBytes, long observedLowerBound) {
        BoundedText bounded = boundedText(value, maximumBytes);
        return new BoundedText(bounded.value(), Math.max(bounded.observedBytes(), observedLowerBound),
                bounded.returnedBytes(), true);
    }

    private static EncodedCharacter encodedCharacter(CharSequence value, int offset) {
        char current = value.charAt(offset);
        if (current == '"' || current == '\\') {
            return new EncodedCharacter(1, 2);
        }
        if (current <= 0x1F || Character.isSurrogate(current)
                && !(Character.isHighSurrogate(current) && offset + 1 < value.length()
                && Character.isLowSurrogate(value.charAt(offset + 1)))) {
            return new EncodedCharacter(1, 6);
        }
        if (Character.isHighSurrogate(current)) {
            return new EncodedCharacter(2, 4);
        }
        if (current <= 0x7F) {
            return new EncodedCharacter(1, 1);
        }
        if (current <= 0x7FF) {
            return new EncodedCharacter(1, 2);
        }
        return new EncodedCharacter(1, 3);
    }

    private record EncodedCharacter(int characters, int bytes) {
    }

    record BoundedText(TextValue value, long observedBytes, long returnedBytes, boolean truncated) {
    }
}

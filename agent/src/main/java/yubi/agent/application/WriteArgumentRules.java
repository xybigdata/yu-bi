package yubi.agent.application;

import yubi.agent.api.ToolInputException;

import java.util.regex.Pattern;

final class WriteArgumentRules {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    private WriteArgumentRules() {
    }

    static String identifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw invalid();
        }
        return value;
    }

    static String optionalIdentifier(String value) {
        return value == null ? null : identifier(value.trim());
    }

    static String normalizedName(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty() || normalized.length() > 255) {
            throw invalid();
        }
        return normalized;
    }

    static String optionalDescription(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static ToolInputException invalid() {
        return new ToolInputException("INVALID_WRITE_TOOL_INPUT", "受控写工具参数无效");
    }
}

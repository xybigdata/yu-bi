package yubi.agent.application;

import yubi.query.domain.QueryModels.ValueType;

import java.math.BigDecimal;

final class SafeScalarValues {

    private static final int MAXIMUM_NUMERIC_PRECISION = 1_000;
    private static final int MAXIMUM_NUMERIC_SCALE = 1_000;

    private SafeScalarValues() {
    }

    static String normalize(String value, ValueType type) {
        if (value == null || type == null) {
            throw ToolArgumentReader.invalid("安全标量值无效");
        }
        return switch (type) {
            case STRING, DATE -> value;
            case NUMERIC -> numeric(value);
            case BOOLEAN -> bool(value);
            default -> throw ToolArgumentReader.invalid("值类型不允许进入 execute_view");
        };
    }

    private static String numeric(String value) {
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.precision() > MAXIMUM_NUMERIC_PRECISION
                    || Math.abs((long) number.scale()) > MAXIMUM_NUMERIC_SCALE) {
                throw ToolArgumentReader.invalid("数值超出 execute_view 的确定性范围");
            }
            return number.signum() == 0 ? "0" : number.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            throw ToolArgumentReader.invalid("NUMERIC 值必须是纯数值");
        }
    }

    private static String bool(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return "true";
        }
        if ("false".equalsIgnoreCase(value)) {
            return "false";
        }
        throw ToolArgumentReader.invalid("BOOLEAN 值只能是 true 或 false");
    }
}

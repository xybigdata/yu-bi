package datart.core.common;

public class NamingUtils {

    private NamingUtils() {
    }

    public static String lowerCamelToLowerHyphen(String value) {
        return splitCamelCase(value, '-');
    }

    public static String lowerCamelToLowerUnderscore(String value) {
        return splitCamelCase(value, '_');
    }

    private static String splitCamelCase(String value, char separator) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current) && i > 0) {
                result.append(separator);
            }
            result.append(Character.toLowerCase(current));
        }
        return result.toString();
    }

}

package yubi.agent.domain;

public record ToolResultLimits(int maximumItems, long maximumBytes) {
    public ToolResultLimits {
        if (maximumItems < 1 || maximumItems > 10_000) {
            throw new IllegalArgumentException("工具结果数量限制无效");
        }
        if (maximumBytes < 128 || maximumBytes > 10_000_000) {
            throw new IllegalArgumentException("工具结果字节限制无效");
        }
    }

    public static ToolResultLimits defaults() {
        return new ToolResultLimits(100, 64 * 1024L);
    }
}

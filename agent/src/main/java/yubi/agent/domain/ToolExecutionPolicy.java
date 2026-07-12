package yubi.agent.domain;

public record ToolExecutionPolicy(int maximumPageSize,
                                  long timeoutMillis,
                                  int maximumConcurrentCalls) {

    public ToolExecutionPolicy {
        if (maximumPageSize < 1 || maximumPageSize > 10_000) {
            throw new IllegalArgumentException("Agent 查询页大小限制无效");
        }
        if (timeoutMillis < 1 || timeoutMillis > 600_000) {
            throw new IllegalArgumentException("Agent Tool 超时限制无效");
        }
        if (maximumConcurrentCalls < 1 || maximumConcurrentCalls > 64) {
            throw new IllegalArgumentException("Agent Tool 并发限制无效");
        }
    }

    public static ToolExecutionPolicy defaults() {
        return new ToolExecutionPolicy(100, 30_000, 4);
    }
}

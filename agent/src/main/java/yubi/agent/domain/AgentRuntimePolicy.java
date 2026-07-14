package yubi.agent.domain;

public record AgentRuntimePolicy(int maximumSteps,
                                 int maximumRecoverableFailures,
                                 int maximumPromptLength,
                                 int maximumArgumentNodes,
                                 int maximumArgumentDepth) {
    public AgentRuntimePolicy(int maximumSteps,
                              int maximumRecoverableFailures,
                              int maximumPromptLength) {
        this(maximumSteps, maximumRecoverableFailures, maximumPromptLength, 2_000, 16);
    }

    public AgentRuntimePolicy {
        if (maximumSteps < 1 || maximumSteps > 100) {
            throw new IllegalArgumentException("最大步骤数必须在 1 到 100 之间");
        }
        if (maximumRecoverableFailures < 0 || maximumRecoverableFailures >= maximumSteps) {
            throw new IllegalArgumentException("可恢复失败次数必须小于最大步骤数");
        }
        if (maximumPromptLength < 1 || maximumPromptLength > 100_000) {
            throw new IllegalArgumentException("请求长度限制无效");
        }
        if (maximumArgumentNodes < 1 || maximumArgumentNodes > 100_000
                || maximumArgumentDepth < 1 || maximumArgumentDepth > 100) {
            throw new IllegalArgumentException("模型工具参数复杂度限制无效");
        }
    }

    public static AgentRuntimePolicy defaults() {
        return new AgentRuntimePolicy(8, 1, 8_000, 2_000, 16);
    }
}

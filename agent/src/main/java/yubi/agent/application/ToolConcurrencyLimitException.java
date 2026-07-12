package yubi.agent.application;

final class ToolConcurrencyLimitException extends RuntimeException {
    ToolConcurrencyLimitException() {
        super("只读工具并发已达到上限");
    }
}

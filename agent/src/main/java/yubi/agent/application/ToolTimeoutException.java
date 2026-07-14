package yubi.agent.application;

final class ToolTimeoutException extends RuntimeException {
    ToolTimeoutException() {
        super("只读工具执行超时");
    }
}

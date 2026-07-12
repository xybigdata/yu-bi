package yubi.agent.application;

final class ToolExecutionInterruptedException extends RuntimeException {
    ToolExecutionInterruptedException(InterruptedException cause) {
        super("只读工具执行被中断", cause);
    }
}

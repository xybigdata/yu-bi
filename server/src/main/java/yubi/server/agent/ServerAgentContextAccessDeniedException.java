package yubi.server.agent;

public final class ServerAgentContextAccessDeniedException extends RuntimeException {
    public ServerAgentContextAccessDeniedException() {
        super("无法为当前认证用户创建可信 Agent 执行上下文");
    }
}

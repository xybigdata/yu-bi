package yubi.server.recycle;

import java.util.Objects;

public record RecycleExecutionCommand(RecycleResourceType resourceType,
                                      String operationToken,
                                      String clientRequestId) {

    public RecycleExecutionCommand {
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(operationToken, "operationToken");
        Objects.requireNonNull(clientRequestId, "clientRequestId");
        if (operationToken.isBlank() || clientRequestId.isBlank()) {
            throw new IllegalArgumentException("操作令牌和客户端请求 ID 不能为空");
        }
    }
}

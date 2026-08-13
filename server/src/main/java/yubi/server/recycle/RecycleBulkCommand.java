package yubi.server.recycle;

import java.util.List;
import java.util.Objects;

public record RecycleBulkCommand(RecycleResourceType resourceType,
                                 List<String> recordIds,
                                 String clientRequestId) {

    public RecycleBulkCommand {
        Objects.requireNonNull(resourceType, "resourceType");
        recordIds = recordIds == null ? List.of() : List.copyOf(recordIds);
        Objects.requireNonNull(clientRequestId, "clientRequestId");
        if (recordIds.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个回收站根项");
        }
        if (clientRequestId.isBlank()) {
            throw new IllegalArgumentException("客户端请求 ID 不能为空");
        }
    }
}

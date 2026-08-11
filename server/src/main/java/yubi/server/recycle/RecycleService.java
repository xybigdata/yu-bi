package yubi.server.recycle;

import java.util.List;

public interface RecycleService {

    RecyclePreflight preflight(RecycleAccess access, RecyclePreflightCommand command);

    RecycleBatch moveToRecycle(RecycleAccess access, RecycleExecutionCommand command);

    RecycleBatch getBatch(RecycleAccess access,
                          RecycleResourceType resourceType,
                          String batchId);

    List<RecycleEntry> list(RecycleAccess access, RecycleResourceType resourceType);

    RecycleBatch restore(RecycleAccess access, RecycleBulkCommand command);

    RecycleBatch permanentlyDelete(RecycleAccess access, RecycleBulkCommand command);

    RecycleBatch undo(RecycleAccess access, String batchId, String undoToken);

    RecyclePolicy getPolicy(RecycleAccess access, RecycleResourceType resourceType);

    RecyclePolicy updatePolicy(RecycleAccess access,
                               RecycleResourceType resourceType,
                               RecyclePolicy policy);

    RecycleMaintenanceResult maintain(boolean cleanupEnabled, int limit);
}

package yubi.server.recycle;

public record RecycleMaintenanceResult(int processed,
                                       int deleted,
                                       int failed) {
}

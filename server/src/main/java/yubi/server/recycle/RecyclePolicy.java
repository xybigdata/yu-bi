package yubi.server.recycle;

import java.util.Set;

public record RecyclePolicy(boolean enabled, int retentionDays) {

    private static final Set<Integer> ALLOWED_RETENTION_DAYS = Set.of(7, 30, 60, 90);

    public RecyclePolicy {
        if (!ALLOWED_RETENTION_DAYS.contains(retentionDays)) {
            throw new IllegalArgumentException("保留期限仅支持 7、30、60 或 90 天");
        }
    }

    public static RecyclePolicy defaults() {
        return new RecyclePolicy(true, 30);
    }
}

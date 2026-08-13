package yubi.server.recycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
final class RecycleMaintenance {

    private final RecycleService service;
    private final boolean cleanupEnabled;
    private final int batchSize;

    RecycleMaintenance(RecycleService service,
                       @Value("${recycle.cleanup.enabled:true}") boolean cleanupEnabled,
                       @Value("${recycle.cleanup.batch-size:500}") int batchSize) {
        this.service = service;
        this.cleanupEnabled = cleanupEnabled;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${recycle.cleanup.cron:0 0 6 * * *}")
    void run() {
        RecycleMaintenanceResult result = service.maintain(cleanupEnabled, batchSize);
        if (result.processed() > 0) {
            log.info("回收站自动清理完成：处理 {} 项，删除 {} 项，失败 {} 项",
                    result.processed(), result.deleted(), result.failed());
        }
    }
}

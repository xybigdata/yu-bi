package yubi.server.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ArtifactRetryRegistryTest {

    @Test
    void claim期间删除任务后不再恢复producer() {
        ArtifactRetryRegistry registry = new ArtifactRetryRegistry();
        ArtifactProducer producer = context -> context.output().write(1);
        registry.register("task-1", producer);

        ArtifactProducer claimed = registry.claim("task-1");
        registry.remove("task-1");
        registry.restore("task-1", claimed);

        assertNull(registry.claim("task-1"));
    }

    @Test
    void 提交失败且原任务未删除时可恢复producer() {
        ArtifactRetryRegistry registry = new ArtifactRetryRegistry();
        ArtifactProducer producer = context -> context.output().write(1);
        registry.register("task-1", producer);

        ArtifactProducer claimed = registry.claim("task-1");
        registry.restore("task-1", claimed);

        assertSame(producer, registry.claim("task-1"));
    }
}

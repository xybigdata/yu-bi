package yubi.server.artifact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import yubi.core.common.Application;
import yubi.core.common.FileUtils;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ArtifactTaskConfiguration {

    @Bean
    @ConditionalOnMissingBean(ArtifactTaskWebMapper.class)
    ArtifactTaskWebMapper artifactTaskWebMapper() {
        return new ArtifactTaskWebMapper();
    }

    @Bean
    @ConditionalOnMissingBean(ArtifactTaskStore.class)
    ArtifactTaskStore artifactTaskStore(JdbcTemplate jdbcTemplate) {
        return new JdbcArtifactTaskStore(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ArtifactBlobStore.class)
    ArtifactBlobStore artifactBlobStore(
            Application application,
            @Value("${yubi.artifact.storage-directory:artifact-task}") String storageDirectory) {
        return new FileSystemArtifactBlobStore(Path.of(FileUtils.withBasePath(storageDirectory)));
    }

    @Bean(name = "artifactTaskClock")
    @ConditionalOnMissingBean(Clock.class)
    Clock artifactTaskClock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(ArtifactExecutor.class)
    BoundedArtifactExecutor artifactExecutor(
            @Value("${yubi.artifact.executor.thread-count:4}") int threadCount,
            @Value("${yubi.artifact.executor.queue-capacity:100}") int queueCapacity) {
        return new BoundedArtifactExecutor(threadCount, queueCapacity);
    }

    @Bean
    ArtifactRetryRegistry artifactRetryRegistry() {
        return new ArtifactRetryRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(ArtifactTasks.class)
    ArtifactTasks artifactTasks(
            ArtifactTaskStore taskStore,
            ArtifactBlobStore blobStore,
            ArtifactExecutor executor,
            ArtifactRetryRegistry retryRegistry,
            Clock clock,
            @Value("${yubi.artifact.timeout-minutes:15}") long timeoutMinutes,
            @Value("${yubi.artifact.undelivered-retention-hours:24}") long undeliveredRetentionHours,
            @Value("${yubi.artifact.delivered-retention-minutes:15}") long deliveredRetentionMinutes,
            @Value("${yubi.artifact.max-concurrent-per-owner:3}") int maxConcurrentPerOwner) {
        return new DefaultArtifactTasks(
                taskStore,
                blobStore,
                executor,
                clock,
                Duration.ofMinutes(timeoutMinutes),
                Duration.ofHours(undeliveredRetentionHours),
                Duration.ofMinutes(deliveredRetentionMinutes),
                maxConcurrentPerOwner,
                retryRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(ArtifactTaskMaintenance.class)
    ArtifactTaskMaintenance artifactTaskMaintenance(
            ArtifactTaskStore taskStore,
            ArtifactBlobStore blobStore,
            ArtifactRetryRegistry retryRegistry,
            Clock clock,
            @Value("${yubi.artifact.undelivered-retention-hours:24}") long undeliveredRetentionHours,
            @Value("${yubi.artifact.maintenance-batch-size:500}") int batchSize) {
        return new ArtifactTaskMaintenance(
                taskStore,
                blobStore,
                clock,
                Duration.ofHours(undeliveredRetentionHours),
                batchSize,
                retryRegistry
        );
    }
}

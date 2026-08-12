package yubi.server.artifact;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.manager.PermissionDataCache;
import yubi.server.recycle.RecycleService;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ArtifactRecycleConfigurationContextTest {

    @Test
    void shouldStartArtifactAndRecycleConfigurationsTogether() throws ClassNotFoundException {
        Class<?> recycleConfiguration = Class.forName(
                "yubi.server.recycle.RecycleConfiguration");
        Class<?> recycleHistoryMigrator = Class.forName(
                "yubi.server.recycle.RecycleHistoryMigrator");

        new ApplicationContextRunner()
                .withUserConfiguration(
                        ArtifactTaskConfiguration.class,
                        recycleConfiguration,
                        recycleHistoryMigrator)
                .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .withBean(UserMapperExt.class, () -> mock(UserMapperExt.class))
                .withBean(PermissionDataCache.class, () -> mock(PermissionDataCache.class))
                .withBean(ArtifactTaskStore.class, () -> mock(ArtifactTaskStore.class))
                .withBean(ArtifactBlobStore.class, () -> mock(ArtifactBlobStore.class))
                .withBean(ArtifactExecutor.class, () -> Runnable::run)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ArtifactTasks.class);
                    assertThat(context).hasSingleBean(RecycleService.class);
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context).hasBean("artifactTaskClock");
                });
    }
}

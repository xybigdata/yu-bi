package yubi.server.recycle;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import yubi.core.entity.User;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.manager.PermissionDataCache;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
class RecycleConfiguration {

    @Bean
    RecycleStore recycleStore(JdbcTemplate jdbcTemplate) {
        return new JdbcRecycleStore(jdbcTemplate);
    }

    @Bean(name = "recycleAsyncExecutor", destroyMethod = "close")
    BoundedRecycleExecutor recycleAsyncExecutor(
            @Value("${recycle.executor.thread-count:2}") int threadCount,
            @Value("${recycle.executor.queue-capacity:100}") int queueCapacity,
            PermissionDataCache permissionDataCache) {
        return new BoundedRecycleExecutor(threadCount, queueCapacity, permissionDataCache);
    }

    @Bean
    RecycleService recycleService(List<RecycleResourceAdapter> adapters,
                                  RecycleStore store,
                                  @Qualifier("recycleAsyncExecutor") Executor asyncExecutor,
                                  UserMapperExt userMapper) {
        return new DefaultRecycleService(
                adapters, Clock.systemDefaultZone(), store, asyncExecutor,
                actorId -> resolveDeletedByName(userMapper, actorId));
    }

    private String resolveDeletedByName(UserMapperExt userMapper, String actorId) {
        if ("SYSTEM".equals(actorId)) {
            return "系统";
        }
        User user = userMapper.selectByPrimaryKey(actorId);
        return user == null || user.getUsername() == null || user.getUsername().isBlank()
                ? actorId
                : user.getUsername();
    }
}

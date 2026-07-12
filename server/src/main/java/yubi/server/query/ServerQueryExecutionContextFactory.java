package yubi.server.query;

import org.springframework.stereotype.Component;
import yubi.core.entity.User;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.security.manager.YuBiSecurityManager;

import java.util.UUID;

@Component
public class ServerQueryExecutionContextFactory {

    private final YuBiSecurityManager securityManager;

    public ServerQueryExecutionContextFactory(YuBiSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public QueryExecutionContext forView(boolean checkViewPermission) {
        return create(checkViewPermission ? Channel.AUTHENTICATED : Channel.SHARED);
    }

    public QueryExecutionContext forSource() {
        return create(Channel.AUTHENTICATED);
    }

    private QueryExecutionContext create(Channel channel) {
        User user = securityManager.getCurrentUser();
        return new QueryExecutionContext(channel, user.getId(), null, UUID.randomUUID().toString());
    }
}

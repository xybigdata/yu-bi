package yubi.server.agent;

import org.springframework.stereotype.Component;
import yubi.agent.api.AgentExecutionContext;
import yubi.core.entity.User;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.service.OrgService;

import java.util.UUID;

@Component
public final class ServerAgentExecutionContextFactory {

    private final YuBiSecurityManager securityManager;
    private final OrgService orgService;

    public ServerAgentExecutionContextFactory(YuBiSecurityManager securityManager, OrgService orgService) {
        this.securityManager = securityManager;
        this.orgService = orgService;
    }

    /** organizationId 必须来自服务器已控制的组织选择范围，不属于 AgentRequest 或模型参数。 */
    public AgentExecutionContext create(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            throw denied();
        }
        User user;
        try {
            user = securityManager.getCurrentUser();
        } catch (RuntimeException exception) {
            throw denied();
        }
        if (user == null || user.getId() == null || user.getId().isBlank() || !isMember(organizationId)) {
            throw denied();
        }
        String sessionId = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        QueryExecutionContext queryContext = new QueryExecutionContext(
                Channel.AUTHENTICATED, user.getId(), organizationId, correlationId);
        return new AgentExecutionContext(sessionId, requestId, queryContext);
    }

    private boolean isMember(String organizationId) {
        try {
            var organizations = orgService.listOrganizations();
            return organizations != null && organizations.stream()
                    .anyMatch(value -> value != null && organizationId.equals(value.getId()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private ServerAgentContextAccessDeniedException denied() {
        return new ServerAgentContextAccessDeniedException();
    }
}

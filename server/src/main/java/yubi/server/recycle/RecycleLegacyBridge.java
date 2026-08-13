package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;

import java.util.List;
import java.util.UUID;

@Component
public final class RecycleLegacyBridge {

    private final RecycleService service;
    private final YuBiSecurityManager securityManager;

    public RecycleLegacyBridge(RecycleService service,
                               YuBiSecurityManager securityManager) {
        this.service = service;
        this.securityManager = securityManager;
    }

    public boolean moveToRecycle(String organizationId,
                                 RecycleResourceType resourceType,
                                 String rootId) {
        User user = securityManager.getCurrentUser();
        if (user == null) {
            throw new SecurityException("用户未登录");
        }
        RecycleAccess access = RecycleAccess.authenticated(
                user.getId(), organizationId, securityManager.isOrgOwner(organizationId));
        RecyclePreflight preflight = service.preflight(
                access, new RecyclePreflightCommand(resourceType, List.of(rootId)));
        if (preflight.items().getFirst().status() != RecycleItemStatus.SUCCESS) {
            return false;
        }
        RecycleBatch batch = service.moveToRecycle(
                access,
                new RecycleExecutionCommand(
                        resourceType, preflight.operationToken(), UUID.randomUUID().toString()));
        return batch.state() == RecycleBatchState.PROCESSING
                || batch.items().getFirst().status() == RecycleItemStatus.SUCCESS;
    }
}

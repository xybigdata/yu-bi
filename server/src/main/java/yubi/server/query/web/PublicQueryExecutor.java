package yubi.server.query.web;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import yubi.core.common.Application;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryResult;
import yubi.security.base.ResourceType;
import yubi.security.manager.YuBiSecurityManager;
import yubi.security.util.AESUtil;
import yubi.server.base.params.ShareAuthorizedToken;
import yubi.server.query.ServerQueryExecutionContextFactory;

import java.util.Objects;

@Component
public class PublicQueryExecutor {

    private static final String ACCESS_DENIED_MESSAGE = "执行权限不足";

    private final ExecuteQueryUseCase executeQueryUseCase;
    private final ServerQueryExecutionContextFactory contextFactory;
    private final YuBiSecurityManager securityManager;

    public PublicQueryExecutor(ExecuteQueryUseCase executeQueryUseCase,
                               ServerQueryExecutionContextFactory contextFactory,
                               YuBiSecurityManager securityManager) {
        this.executeQueryUseCase = executeQueryUseCase;
        this.contextFactory = contextFactory;
        this.securityManager = securityManager;
    }

    public QueryResult execute(String token, ExecuteQueryCommand command) {
        ShareAuthorizedToken authorizedToken = validate(token, command.viewId());
        try {
            securityManager.runAs(authorizedToken.getPermissionBy());
            return executeQueryUseCase.execute(command, contextFactory.forView(false));
        } finally {
            securityManager.releaseRunAs();
        }
    }

    private ShareAuthorizedToken validate(String token, String viewId) {
        if (StringUtils.isBlank(token)) {
            throw denied(null);
        }
        try {
            ShareAuthorizedToken authorizedToken = AESUtil.decrypt(token, Application.getTokenSecret(),
                    ShareAuthorizedToken.class);
            if (authorizedToken == null || !ResourceType.VIEW.equals(authorizedToken.getVizType())
                    || !Objects.equals(authorizedToken.getVizId(), viewId)) {
                throw denied(null);
            }
            return authorizedToken;
        } catch (QueryAccessDeniedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw denied(exception);
        }
    }

    private QueryAccessDeniedException denied(Throwable cause) {
        return new QueryAccessDeniedException(ACCESS_DENIED_MESSAGE, cause);
    }
}

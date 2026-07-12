package yubi.server.query.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import yubi.core.common.Application;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryResult;
import yubi.security.base.ResourceType;
import yubi.security.manager.YuBiSecurityManager;
import yubi.security.util.AESUtil;
import yubi.server.base.params.ShareAuthorizedToken;
import yubi.server.query.ServerQueryExecutionContextFactory;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PublicQueryExecutorTest {

    private static final String TOKEN_SECRET = "query-web-test-secret";

    private ExecuteQueryUseCase useCase;
    private ServerQueryExecutionContextFactory contextFactory;
    private YuBiSecurityManager securityManager;
    private PublicQueryExecutor executor;
    private ExecuteQueryCommand command;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.security.token.secret", "d@a$t%a^r&a*t")).thenReturn(TOKEN_SECRET);
        new Application().setApplicationContext(applicationContext);

        useCase = mock(ExecuteQueryUseCase.class);
        contextFactory = mock(ServerQueryExecutionContextFactory.class);
        securityManager = mock(YuBiSecurityManager.class);
        executor = new PublicQueryExecutor(useCase, contextFactory, securityManager);
        command = mock(ExecuteQueryCommand.class);
        when(command.viewId()).thenReturn("view-1");
    }

    @Test
    void shouldExecuteWithBoundViewAndReleaseIdentity() {
        QueryExecutionContext context = mock(QueryExecutionContext.class);
        QueryResult expected = QueryResult.empty();
        when(contextFactory.forView(false)).thenReturn(context);
        when(useCase.execute(command, context)).thenReturn(expected);

        QueryResult result = executor.execute(tokenFor(ResourceType.VIEW, "view-1"), command);

        assertSame(expected, result);
        verify(securityManager).runAs("share-owner");
        verify(useCase).execute(command, context);
        verify(securityManager).releaseRunAs();
    }

    @Test
    void shouldReleaseIdentityWhenExecutionFails() {
        QueryExecutionContext context = mock(QueryExecutionContext.class);
        RuntimeException failure = new RuntimeException("failed");
        when(contextFactory.forView(false)).thenReturn(context);
        when(useCase.execute(command, context)).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> executor.execute(tokenFor(ResourceType.VIEW, "view-1"), command));

        assertSame(failure, thrown);
        verify(securityManager).releaseRunAs();
    }

    @Test
    void shouldReleaseIdentityWhenRunAsFailsWithoutExecutingQuery() {
        RuntimeException failure = new RuntimeException("runAs failed");
        doThrow(failure).when(securityManager).runAs("share-owner");

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> executor.execute(tokenFor(ResourceType.VIEW, "view-1"), command));

        assertSame(failure, thrown);
        verify(securityManager).runAs("share-owner");
        verify(securityManager, times(1)).releaseRunAs();
        verifyNoInteractions(contextFactory, useCase);
    }

    @Test
    void shouldRejectMissingInvalidTypeAndWrongViewTokensWithoutSwitchingIdentity() {
        assertThrows(QueryAccessDeniedException.class, () -> executor.execute(null, command));
        assertThrows(QueryAccessDeniedException.class, () -> executor.execute("invalid-token", command));
        assertThrows(QueryAccessDeniedException.class,
                () -> executor.execute(tokenFor(ResourceType.DATACHART, "view-1"), command));
        assertThrows(QueryAccessDeniedException.class,
                () -> executor.execute(tokenFor(ResourceType.VIEW, "view-2"), command));

        verify(securityManager, never()).runAs("share-owner");
        verify(securityManager, never()).releaseRunAs();
    }

    private String tokenFor(ResourceType resourceType, String viewId) {
        ShareAuthorizedToken authorizedToken = new ShareAuthorizedToken();
        authorizedToken.setVizType(resourceType);
        authorizedToken.setVizId(viewId);
        authorizedToken.setPermissionBy("share-owner");
        return AESUtil.encrypt(authorizedToken, TOKEN_SECRET);
    }
}

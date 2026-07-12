package yubi.server.agent;

import org.junit.jupiter.api.Test;
import yubi.core.entity.Organization;
import yubi.core.entity.User;
import yubi.query.domain.QueryModels.Channel;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.OrganizationBaseInfo;
import yubi.server.service.OrgService;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerAgentExecutionContextFactoryTest {

    @Test
    void shouldCreateAuthenticatedContextFromCurrentUserAndValidatedOrganizationMembership() {
        User user = new User();
        user.setId("user-1");
        ServerAgentExecutionContextFactory factory = new ServerAgentExecutionContextFactory(
                security(user, null), organizations(List.of(organization("org-1")), null));

        var first = factory.create("org-1");
        var second = factory.create("org-1");

        assertEquals(Channel.AUTHENTICATED, first.queryContext().channel());
        assertEquals("user-1", first.queryContext().subjectId());
        assertEquals("org-1", first.queryContext().organizationId());
        assertNotNull(first.queryContext().correlationId());
        assertNotEquals(first.sessionId(), second.sessionId());
        assertNotEquals(first.requestId(), second.requestId());
        assertNotEquals(first.queryContext().correlationId(), second.queryContext().correlationId());
    }

    @Test
    void shouldRejectBlankUnauthenticatedNonMemberAndAdapterFailuresWithStableMessage() {
        User user = new User();
        user.setId("user-1");
        AtomicInteger securityCalls = new AtomicInteger();
        ServerAgentExecutionContextFactory valid = new ServerAgentExecutionContextFactory(
                security(user, securityCalls), organizations(List.of(organization("org-1")), null));

        assertDenied(() -> valid.create(" "));
        assertEquals(0, securityCalls.get());

        assertDenied(() -> new ServerAgentExecutionContextFactory(
                security(null, null), organizations(List.of(organization("org-1")), null)).create("org-1"));
        assertDenied(() -> new ServerAgentExecutionContextFactory(
                security(user, null), organizations(List.of(organization("org-2")), null)).create("org-1"));
        assertDenied(() -> new ServerAgentExecutionContextFactory(
                security(user, null), organizations(null, new IllegalStateException("jdbc:secret password=x")))
                .create("org-1"));
        assertDenied(() -> new ServerAgentExecutionContextFactory(
                failingSecurity(new IllegalStateException("principal secret")),
                organizations(List.of(organization("org-1")), null))
                .create("org-1"));
    }

    private void assertDenied(org.junit.jupiter.api.function.Executable operation) {
        ServerAgentContextAccessDeniedException failure = assertThrows(
                ServerAgentContextAccessDeniedException.class, operation);
        assertEquals("无法为当前认证用户创建可信 Agent 执行上下文", failure.getMessage());
        assertTrue(failure.getCause() == null);
        assertTrue(!failure.toString().contains("secret"));
    }

    private YuBiSecurityManager security(User user, AtomicInteger calls) {
        return proxy(YuBiSecurityManager.class, (method, arguments) -> {
            if (method.getName().equals("getCurrentUser")) {
                if (calls != null) {
                    calls.incrementAndGet();
                }
                return user;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private YuBiSecurityManager failingSecurity(RuntimeException failure) {
        return proxy(YuBiSecurityManager.class, (method, arguments) -> {
            if (method.getName().equals("getCurrentUser")) {
                throw failure;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private OrgService organizations(List<OrganizationBaseInfo> values, RuntimeException failure) {
        return proxy(OrgService.class, (method, arguments) -> {
            if (method.getName().equals("listOrganizations")) {
                if (failure != null) {
                    throw failure;
                }
                return values;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private OrganizationBaseInfo organization(String id) {
        Organization organization = new Organization();
        organization.setId(id);
        return new OrganizationBaseInfo(organization);
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> invocation.invoke(method, arguments));
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] arguments) throws Throwable;
    }
}

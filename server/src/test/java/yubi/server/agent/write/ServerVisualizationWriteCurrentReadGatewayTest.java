package yubi.server.agent.write;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import yubi.core.mappers.ext.DashboardMapperExt;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.OrganizationMapperExt;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.core.mappers.ext.RelRoleUserMapperExt;
import yubi.core.mappers.ext.RoleMapperExt;
import yubi.core.mappers.ext.ViewMapperExt;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerVisualizationWriteCurrentReadGatewayTest {

    @Test
    void shouldBypassAndFlushEveryControlledCurrentReadMappedStatement() {
        Configuration configuration = mapperConfiguration();
        List<String> controlledReads = List.of(
                id(ViewMapperExt.class, "selectControlledWriteCurrentForUpdate"),
                id(FolderMapperExt.class, "selectControlledWriteCurrentNamesForUpdate"),
                id(FolderMapperExt.class, "selectControlledWriteCurrentForUpdate"),
                id(FolderMapperExt.class, "selectControlledWriteCurrentByRelationForUpdate"),
                id(FolderMapperExt.class, "selectControlledWriteRootNamespaceForUpdate"),
                id(DashboardMapperExt.class, "selectControlledWriteCurrentForUpdate"),
                id(OrganizationMapperExt.class, "selectControlledWriteCurrentMembershipForUpdate"),
                id(RoleMapperExt.class, "selectControlledWriteCurrentAuthorizationForUpdate"),
                id(RelRoleResourceMapperExt.class,
                        "selectControlledWriteCurrentAuthorizationForUpdate"));

        for (String statementId : controlledReads) {
            MappedStatement statement = configuration.getMappedStatement(statementId);
            assertFalse(statement.isUseCache(), statementId + " 禁止读取二级缓存");
            assertTrue(statement.isFlushCacheRequired(), statementId + " 必须刷新所在缓存命名空间");
        }
        assertForUpdate(configuration, ViewMapperExt.class,
                "selectControlledWriteCurrentForUpdate", Map.of("id", "view-1"));
        assertForUpdate(configuration, FolderMapperExt.class,
                "selectControlledWriteCurrentNamesForUpdate",
                parameters("orgId", "org-1", "parentId", null, "name", "图表"));
        assertForUpdate(configuration, FolderMapperExt.class,
                "selectControlledWriteCurrentNamesForUpdate",
                parameters("orgId", "org-1", "parentId", "folder-1", "name", "图表"));
        assertForUpdate(configuration, FolderMapperExt.class,
                "selectControlledWriteCurrentForUpdate", Map.of("id", "folder-1"));
        assertForUpdate(configuration, FolderMapperExt.class,
                "selectControlledWriteCurrentByRelationForUpdate",
                Map.of("relType", "DASHBOARD", "relId", "dashboard-1"));
        assertForUpdate(configuration, FolderMapperExt.class,
                "selectControlledWriteRootNamespaceForUpdate", Map.of("orgId", "org-1"));
        assertForUpdate(configuration, DashboardMapperExt.class,
                "selectControlledWriteCurrentForUpdate", Map.of("id", "dashboard-1"));
        assertForUpdate(configuration, OrganizationMapperExt.class,
                "selectControlledWriteCurrentMembershipForUpdate",
                Map.of("orgId", "org-1", "userId", "user-1"));
        assertForUpdate(configuration, RoleMapperExt.class,
                "selectControlledWriteCurrentAuthorizationForUpdate",
                Map.of("orgId", "org-1", "userId", "user-1"));
        assertForUpdate(configuration, RelRoleResourceMapperExt.class,
                "selectControlledWriteCurrentAuthorizationForUpdate",
                Map.of("orgId", "org-1", "userId", "user-1"));
        assertAuthorizationLimit(configuration, RoleMapperExt.class,
                "selectControlledWriteCurrentAuthorizationForUpdate");
        assertAuthorizationLimit(configuration, RelRoleResourceMapperExt.class,
                "selectControlledWriteCurrentAuthorizationForUpdate");
        assertAuthorizationResourceIndexOrder(configuration);

        var folderCache = configuration.getMappedStatement(id(
                FolderMapperExt.class, "selectControlledWriteCurrentForUpdate")).getCache();
        var roleCache = configuration.getMappedStatement(id(
                RoleMapperExt.class, "selectControlledWriteCurrentAuthorizationForUpdate")).getCache();
        assertNotNull(folderCache);
        assertNotNull(roleCache);
        assertSame(folderCache, configuration.getMappedStatement(id(
                DashboardMapperExt.class, "selectControlledWriteCurrentForUpdate")).getCache());
        assertSame(roleCache, configuration.getMappedStatement(id(
                RelRoleResourceMapperExt.class,
                "selectControlledWriteCurrentAuthorizationForUpdate")).getCache());
        assertSame(roleCache, configuration.getMappedStatement(id(
                RelRoleUserMapperExt.class, "selectByUserAndRole")).getCache());
    }

    @Test
    void shouldStopBeforeRoleAuthorizationWhenCurrentMembershipDoesNotMatch() {
        ViewMapperExt views = mock(ViewMapperExt.class);
        FolderMapperExt folders = mock(FolderMapperExt.class);
        DashboardMapperExt dashboards = mock(DashboardMapperExt.class);
        OrganizationMapperExt organizations = mock(OrganizationMapperExt.class);
        RoleMapperExt roles = mock(RoleMapperExt.class);
        RelRoleResourceMapperExt resources = mock(RelRoleResourceMapperExt.class);
        ServerVisualizationWriteCurrentReadGateway gateway = new ServerVisualizationWriteCurrentReadGateway(
                views, folders, dashboards, organizations, roles, resources);
        when(organizations.selectControlledWriteCurrentMembershipForUpdate("org-1", "user-1"))
                .thenReturn("org-2");

        assertFalse(gateway.lockTrustedAuthorization("org-1", "user-1"));

        verifyNoInteractions(roles, resources);
    }

    @Test
    void shouldLockMembershipBeforeRoleAndResourceAuthorization() {
        ViewMapperExt views = mock(ViewMapperExt.class);
        FolderMapperExt folders = mock(FolderMapperExt.class);
        DashboardMapperExt dashboards = mock(DashboardMapperExt.class);
        OrganizationMapperExt organizations = mock(OrganizationMapperExt.class);
        RoleMapperExt roles = mock(RoleMapperExt.class);
        RelRoleResourceMapperExt resources = mock(RelRoleResourceMapperExt.class);
        ServerVisualizationWriteCurrentReadGateway gateway = new ServerVisualizationWriteCurrentReadGateway(
                views, folders, dashboards, organizations, roles, resources);
        when(organizations.selectControlledWriteCurrentMembershipForUpdate("org-1", "user-1"))
                .thenReturn("org-1");

        assertTrue(gateway.lockTrustedAuthorization("org-1", "user-1"));

        InOrder ordered = inOrder(organizations, roles, resources);
        ordered.verify(organizations).selectControlledWriteCurrentMembershipForUpdate("org-1", "user-1");
        ordered.verify(roles).selectControlledWriteCurrentAuthorizationForUpdate("org-1", "user-1");
        ordered.verify(resources).selectControlledWriteCurrentAuthorizationForUpdate("org-1", "user-1");
    }

    @Test
    void shouldFailClosedBeforeResourceLocksWhenRoleLimitIsExceeded() {
        ViewMapperExt views = mock(ViewMapperExt.class);
        FolderMapperExt folders = mock(FolderMapperExt.class);
        DashboardMapperExt dashboards = mock(DashboardMapperExt.class);
        OrganizationMapperExt organizations = mock(OrganizationMapperExt.class);
        RoleMapperExt roles = mock(RoleMapperExt.class);
        RelRoleResourceMapperExt resources = mock(RelRoleResourceMapperExt.class);
        ServerVisualizationWriteCurrentReadGateway gateway = new ServerVisualizationWriteCurrentReadGateway(
                views, folders, dashboards, organizations, roles, resources);
        when(organizations.selectControlledWriteCurrentMembershipForUpdate("org-1", "user-1"))
                .thenReturn("org-1");
        when(roles.selectControlledWriteCurrentAuthorizationForUpdate("org-1", "user-1"))
                .thenReturn(java.util.Collections.nCopies(
                        RoleMapperExt.CONTROLLED_WRITE_AUTHORIZATION_LOCK_LIMIT + 1, "role"));

        assertFalse(gateway.lockTrustedAuthorization("org-1", "user-1"));

        verifyNoInteractions(resources);
    }

    @Test
    void shouldFailClosedWhenResourceGrantLimitIsExceeded() {
        ViewMapperExt views = mock(ViewMapperExt.class);
        FolderMapperExt folders = mock(FolderMapperExt.class);
        DashboardMapperExt dashboards = mock(DashboardMapperExt.class);
        OrganizationMapperExt organizations = mock(OrganizationMapperExt.class);
        RoleMapperExt roles = mock(RoleMapperExt.class);
        RelRoleResourceMapperExt resources = mock(RelRoleResourceMapperExt.class);
        ServerVisualizationWriteCurrentReadGateway gateway = new ServerVisualizationWriteCurrentReadGateway(
                views, folders, dashboards, organizations, roles, resources);
        when(organizations.selectControlledWriteCurrentMembershipForUpdate("org-1", "user-1"))
                .thenReturn("org-1");
        when(roles.selectControlledWriteCurrentAuthorizationForUpdate("org-1", "user-1"))
                .thenReturn(List.of("role-1"));
        when(resources.selectControlledWriteCurrentAuthorizationForUpdate("org-1", "user-1"))
                .thenReturn(java.util.Collections.nCopies(
                        RoleMapperExt.CONTROLLED_WRITE_AUTHORIZATION_LOCK_LIMIT + 1, "grant"));

        assertFalse(gateway.lockTrustedAuthorization("org-1", "user-1"));

        verify(resources).selectControlledWriteCurrentAuthorizationForUpdate("org-1", "user-1");
        verifyNoMoreInteractions(resources);
    }

    private Configuration mapperConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setCacheEnabled(true);
        configuration.addMapper(ViewMapperExt.class);
        configuration.addMapper(FolderMapperExt.class);
        configuration.addMapper(DashboardMapperExt.class);
        configuration.addMapper(OrganizationMapperExt.class);
        configuration.addMapper(RoleMapperExt.class);
        configuration.addMapper(RelRoleUserMapperExt.class);
        configuration.addMapper(RelRoleResourceMapperExt.class);
        return configuration;
    }

    private String id(Class<?> mapper, String method) {
        return mapper.getName() + "." + method;
    }

    private void assertForUpdate(Configuration configuration,
                                 Class<?> mapper,
                                 String method,
                                 Map<String, Object> parameters) {
        String statementId = id(mapper, method);
        String sql = configuration.getMappedStatement(statementId)
                .getBoundSql(parameters).getSql().toUpperCase(Locale.ROOT);
        assertTrue(sql.contains("FOR UPDATE"), statementId + " 必须执行锁定当前读");
    }

    private void assertAuthorizationLimit(Configuration configuration,
                                          Class<?> mapper,
                                          String method) {
        String statementId = id(mapper, method);
        String sql = configuration.getMappedStatement(statementId)
                .getBoundSql(Map.of("orgId", "org-1", "userId", "user-1"))
                .getSql().toUpperCase(Locale.ROOT);
        assertTrue(sql.contains("LIMIT " + (RoleMapperExt.CONTROLLED_WRITE_AUTHORIZATION_LOCK_LIMIT + 1)),
                statementId + " 必须有界锁定授权行");
    }

    private void assertAuthorizationResourceIndexOrder(Configuration configuration) {
        String statementId = id(RelRoleResourceMapperExt.class,
                "selectControlledWriteCurrentAuthorizationForUpdate");
        String sql = configuration.getMappedStatement(statementId)
                .getBoundSql(Map.of("orgId", "org-1", "userId", "user-1"))
                .getSql().toUpperCase(Locale.ROOT);
        assertTrue(sql.contains("ORDER BY RRR.ROLE_ID, RRR.RESOURCE_ID, RRR.RESOURCE_TYPE"),
                statementId + " 必须按生产唯一索引前缀执行授权锁定读");
    }

    private Map<String, Object> parameters(Object... values) {
        java.util.HashMap<String, Object> parameters = new java.util.HashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            parameters.put((String) values[index], values[index + 1]);
        }
        return parameters;
    }
}

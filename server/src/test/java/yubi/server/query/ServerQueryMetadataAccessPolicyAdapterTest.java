package yubi.server.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yubi.core.base.consts.Const;
import yubi.core.entity.RelSubjectColumns;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.core.mappers.ext.RelSubjectColumnsMapperExt;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryMetadataModels.AssetReference;
import yubi.query.domain.QueryModels.Channel;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.service.ViewService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerQueryMetadataAccessPolicyAdapterTest {

    private final AssetReference asset = new AssetReference(
            "view-1", "org-1", "Orders", null, "folder-1", "source-1");
    private final QueryExecutionContext context = new QueryExecutionContext(
            Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1");
    private ViewService viewService;
    private RelSubjectColumnsMapperExt columnMapper;
    private YuBiSecurityManager securityManager;
    private ServerQueryMetadataAccessPolicyAdapter adapter;

    @BeforeEach
    void setUp() {
        viewService = mock(ViewService.class);
        columnMapper = mock(RelSubjectColumnsMapperExt.class);
        securityManager = mock(YuBiSecurityManager.class);
        User user = new User();
        user.setId("user-1");
        when(securityManager.getCurrentUser()).thenReturn(user);
        adapter = new ServerQueryMetadataAccessPolicyAdapter(viewService, columnMapper, securityManager);
    }

    @Test
    void ownerShouldReceiveAllFieldsAndManageScriptAfterReadAuthorization() {
        when(securityManager.isOrgOwner("org-1")).thenReturn(true);

        var decision = adapter.authorize(asset, context);

        assertTrue(decision.organizationOwner());
        assertTrue(decision.scriptVisible());
        assertEquals(List.of("*"), decision.allowedColumns().iterator().next().path());
        verify(viewService).requirePermission(any(View.class), eq(Const.READ));
        verify(viewService).requirePermission(any(View.class), eq(Const.MANAGE));
        verifyNoInteractions(columnMapper);
    }

    @Test
    void nonOwnerShouldReceiveRoleColumnsAndNoScriptWithoutManagePermission() {
        when(securityManager.isOrgOwner("org-1")).thenReturn(false);
        doAnswer(invocation -> {
            if ((int) invocation.getArgument(1) == Const.MANAGE) {
                throw new SecurityException("MANAGE denied");
            }
            return null;
        }).when(viewService).requirePermission(any(View.class), anyInt());
        RelSubjectColumns permission = new RelSubjectColumns();
        permission.setColumnPermission("[\"orders.amount\",\"orders.status\"]");
        when(columnMapper.listByUser("view-1", "user-1")).thenReturn(List.of(permission));

        var decision = adapter.authorize(asset, context);

        assertFalse(decision.organizationOwner());
        assertFalse(decision.scriptVisible());
        assertEquals(java.util.Set.of("orders.amount", "orders.status"),
                decision.allowedColumns().stream()
                        .map(column -> String.join(".", column.path())).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void shouldRejectForgedSubjectBeforePermissionOrColumnReads() {
        QueryExecutionContext forged = new QueryExecutionContext(
                Channel.AUTHENTICATED, "attacker", "org-1", "correlation-1");

        assertThrows(SecurityException.class, () -> adapter.validateContext(forged));
        assertThrows(SecurityException.class, () -> adapter.authorize(asset, forged));
        verifyNoInteractions(viewService, columnMapper);
    }
}

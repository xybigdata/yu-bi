package yubi.server.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yubi.core.base.consts.Const;
import yubi.core.data.provider.DataProviderManager;
import yubi.core.entity.Source;
import yubi.core.entity.View;
import yubi.core.mappers.ext.RelSubjectColumnsMapperExt;
import yubi.core.mappers.ext.SourceMapperExt;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.PreviewQueryCommand;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.application.DefaultQueryService;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.ScriptType;
import yubi.query.port.QueryVariablePort;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.service.SourceService;
import yubi.server.service.ViewService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerQueryAuthorizationOrderTest {

    private ViewService viewService;
    private SourceService sourceService;
    private SourceMapperExt sourceMapper;
    private DataProviderManager dataProviderManager;
    private ServerSourceConfigMapper sourceConfigMapper;
    private QueryVariablePort variablePort;
    private DefaultQueryService service;

    @BeforeEach
    void setUp() {
        viewService = mock(ViewService.class);
        sourceService = mock(SourceService.class);
        sourceMapper = mock(SourceMapperExt.class);
        dataProviderManager = mock(DataProviderManager.class);
        sourceConfigMapper = mock(ServerSourceConfigMapper.class);
        variablePort = mock(QueryVariablePort.class);
        YuBiSecurityManager securityManager = mock(YuBiSecurityManager.class);
        ServerQueryDefinitionAdapter definitionAdapter = new ServerQueryDefinitionAdapter(viewService, sourceMapper);
        ServerQueryAccessPolicyAdapter accessAdapter = new ServerQueryAccessPolicyAdapter(
                viewService, sourceService, mock(RelSubjectColumnsMapperExt.class), securityManager);
        ServerQueryEngineAdapter engineAdapter = new ServerQueryEngineAdapter(
                dataProviderManager, sourceService, sourceConfigMapper);
        service = new DefaultQueryService(
                definitionAdapter, accessAdapter, variablePort, engineAdapter, event -> { });
    }

    @Test
    void deniedViewMustNotReadOrDecryptSourceConfigurationOrCallProvider() {
        View view = view("view-1", "source-1");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        doThrow(new SecurityException("read denied"))
                .when(viewService).requirePermission(any(View.class), eq(Const.READ));

        assertThrows(QueryAccessDeniedException.class, () -> service.execute(
                executeCommand(), context(Channel.AUTHENTICATED)));

        verify(viewService).retrieve("view-1", false);
        verifyNoInteractions(sourceMapper, sourceConfigMapper, dataProviderManager, variablePort);
        verify(sourceService, never()).retrieve(anyString(), anyBoolean());
    }

    @Test
    void deniedPreviewMustUseSafeProjectionWithoutReadingOrDecryptingConfiguration() {
        Source projection = new Source();
        projection.setId("source-1");
        projection.setOrgId("org-1");
        projection.setName("orders");
        projection.setParentId("folder-1");
        when(sourceMapper.selectQueryAccessProjection("source-1")).thenReturn(projection);
        doThrow(new SecurityException("read denied"))
                .when(sourceService).requirePermission(any(Source.class), eq(Const.READ));

        assertThrows(QueryAccessDeniedException.class, () -> service.preview(
                new PreviewQueryCommand("source-1", "select 1", ScriptType.SQL, List.of(), List.of(), 10),
                context(Channel.AUTHENTICATED)));

        verify(sourceMapper).selectQueryAccessProjection("source-1");
        verify(sourceService, never()).retrieve(anyString(), anyBoolean());
        verifyNoInteractions(sourceConfigMapper, dataProviderManager, variablePort);
    }

    private ExecuteQueryCommand executeCommand() {
        return new ExecuteQueryCommand("view-1", List.of(),
                List.of(new ColumnSelection(null, List.of("orders", "amount"))), List.of(), List.of(),
                List.of(), List.of(), List.of(), null, Map.of(), false, false, 0, false);
    }

    private QueryExecutionContext context(Channel channel) {
        return new QueryExecutionContext(channel, "user-1", null, "correlation-1");
    }

    private View view(String viewId, String sourceId) {
        View view = new View();
        view.setId(viewId);
        view.setName("orders-view");
        view.setOrgId("org-1");
        view.setSourceId(sourceId);
        view.setType("SQL");
        view.setScript("select amount from orders");
        view.setModel("{}");
        return view;
    }
}

package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yubi.core.data.provider.DataProviderManager;
import yubi.core.data.provider.Dataframe;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.PreviewQueryCommand;
import yubi.query.api.PreviewQueryUseCase;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryResult;
import yubi.query.domain.QueryModels.Channel;
import yubi.server.base.params.TestExecuteParam;
import yubi.server.base.params.ViewExecuteParam;
import yubi.server.query.ServerQueryCompatibilityMapper;
import yubi.server.query.ServerQueryExecutionContextFactory;
import yubi.server.query.ServerSourceConfigMapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataProviderServiceImplDelegationTest {

    private ExecuteQueryUseCase executeUseCase;
    private PreviewQueryUseCase previewUseCase;
    private ServerQueryCompatibilityMapper mapper;
    private ServerQueryExecutionContextFactory contextFactory;
    private DataProviderServiceImpl service;

    @BeforeEach
    void setUp() {
        executeUseCase = mock(ExecuteQueryUseCase.class);
        previewUseCase = mock(PreviewQueryUseCase.class);
        mapper = mock(ServerQueryCompatibilityMapper.class);
        contextFactory = mock(ServerQueryExecutionContextFactory.class);
        service = new DataProviderServiceImpl(mock(DataProviderManager.class), executeUseCase, previewUseCase,
                mapper, contextFactory, new ServerSourceConfigMapper());
    }

    @Test
    void legacyExecuteMustOnlyMapAndDelegate() throws Exception {
        ViewExecuteParam request = new ViewExecuteParam();
        request.setViewId("view-1");
        request.setColumns(java.util.List.of(yubi.core.data.provider.SelectColumn.of(null, "amount")));
        ExecuteQueryCommand command = mock(ExecuteQueryCommand.class);
        QueryExecutionContext context = new QueryExecutionContext(
                Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1");
        QueryResult queryResult = QueryResult.empty();
        Dataframe expected = Dataframe.empty();
        when(mapper.toCommand(request)).thenReturn(command);
        when(contextFactory.forView(true)).thenReturn(context);
        when(executeUseCase.execute(command, context)).thenReturn(queryResult);
        when(mapper.toDataframe(queryResult)).thenReturn(expected);

        assertSame(expected, service.execute(request));
        verify(executeUseCase).execute(command, context);
    }

    @Test
    void legacyPreviewMustOnlyMapAndDelegate() throws Exception {
        TestExecuteParam request = new TestExecuteParam();
        request.setSourceId("source-1");
        PreviewQueryCommand command = mock(PreviewQueryCommand.class);
        QueryExecutionContext context = new QueryExecutionContext(
                Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1");
        QueryResult queryResult = QueryResult.empty();
        Dataframe expected = Dataframe.empty();
        when(mapper.toCommand(request)).thenReturn(command);
        when(contextFactory.forSource()).thenReturn(context);
        when(previewUseCase.preview(command, context)).thenReturn(queryResult);
        when(mapper.toDataframe(queryResult)).thenReturn(expected);

        assertSame(expected, service.testExecute(request));
        verify(previewUseCase).preview(command, context);
    }
}

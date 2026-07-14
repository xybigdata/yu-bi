package yubi.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.PreviewQueryUseCase;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryExecutionException;
import yubi.query.api.QueryResult;
import yubi.query.api.QueryValidationException;
import yubi.server.config.QueryWebExceptionHandler;
import yubi.server.config.WebExceptionHandler;
import yubi.server.query.ServerQueryExecutionContextFactory;
import yubi.server.query.web.PublicQueryExecutor;
import yubi.server.query.web.QueryWebMapper;
import yubi.server.service.DataProviderService;
import yubi.server.service.ShareService;
import yubi.server.service.ShareDownloadSessionManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QueryControllerWebTest {

    private ExecuteQueryUseCase executeUseCase;
    private PreviewQueryUseCase previewUseCase;
    private ServerQueryExecutionContextFactory contextFactory;
    private PublicQueryExecutor publicQueryExecutor;
    private MockMvc authenticatedMvc;
    private MockMvc publicMvc;

    @BeforeEach
    void setUp() {
        executeUseCase = mock(ExecuteQueryUseCase.class);
        previewUseCase = mock(PreviewQueryUseCase.class);
        contextFactory = mock(ServerQueryExecutionContextFactory.class);
        publicQueryExecutor = mock(PublicQueryExecutor.class);
        QueryWebMapper mapper = new QueryWebMapper();
        QueryController queryController = new QueryController(executeUseCase, previewUseCase, mapper, contextFactory);
        PublicQueryController publicQueryController = new PublicQueryController(publicQueryExecutor, mapper);
        authenticatedMvc = MockMvcBuilders.standaloneSetup(queryController)
                .setControllerAdvice(new QueryWebExceptionHandler(), new WebExceptionHandler()).build();
        publicMvc = MockMvcBuilders.standaloneSetup(publicQueryController)
                .setControllerAdvice(new QueryWebExceptionHandler(), new WebExceptionHandler()).build();
    }

    @Test
    void shouldMapAuthenticatedExecuteAndPreviewRequestsToUseCases() throws Exception {
        QueryExecutionContext context = mock(QueryExecutionContext.class);
        when(contextFactory.forView(true)).thenReturn(context);
        when(contextFactory.forSource()).thenReturn(context);
        when(executeUseCase.execute(any(), any())).thenReturn(QueryResult.empty());
        when(previewUseCase.preview(any(), any())).thenReturn(QueryResult.empty());

        authenticatedMvc.perform(post("/queries/execute").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"viewId":"view-1","columns":[{"alias":"amount","column":["amount"]}],
                                 "aggregators":[],"filters":[],"groups":[],"orders":[]}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        authenticatedMvc.perform(post("/queries/preview").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceId":"source-1","script":"select 1","scriptType":"SQL","variables":[]}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        verify(contextFactory).forView(true);
        verify(contextFactory).forSource();
        verify(executeUseCase).execute(any(), any());
        verify(previewUseCase).preview(any(), any());
    }

    @Test
    void shouldReadPublicTokenFromHeaderOnly() throws Exception {
        when(publicQueryExecutor.execute(any(), any())).thenReturn(QueryResult.empty());

        publicMvc.perform(post("/public/queries/execute")
                        .queryParam("executeToken", "query-token")
                        .header(PublicQueryController.SHARE_TOKEN_HEADER, "header-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"viewId":"view-1","columns":[{"alias":"amount","column":["amount"]}],
                                 "aggregators":[],"filters":[],"groups":[],"orders":[]}
                                """))
                .andExpect(status().isOk());

        verify(publicQueryExecutor).execute(org.mockito.ArgumentMatchers.eq("header-token"), any());
    }

    @Test
    void shouldMapStableQueryFailuresToHttpStatuses() throws Exception {
        QueryExecutionContext context = mock(QueryExecutionContext.class);
        when(contextFactory.forView(true)).thenReturn(context);
        String body = """
                {"viewId":"view-1","columns":[{"alias":"amount","column":["amount"]}],
                 "aggregators":[],"filters":[],"groups":[],"orders":[]}
                """;

        doThrow(new QueryValidationException("invalid")).when(executeUseCase).execute(any(), any());
        authenticatedMvc.perform(post("/queries/execute").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("invalid"));

        doThrow(new QueryAccessDeniedException("denied", null)).when(executeUseCase).execute(any(), any());
        authenticatedMvc.perform(post("/queries/execute").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("denied"));

        doThrow(new QueryDefinitionException("definition", null)).when(executeUseCase).execute(any(), any());
        authenticatedMvc.perform(post("/queries/execute").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.message").value("definition"));

        doThrow(new QueryExecutionException("execution", null)).when(executeUseCase).execute(any(), any());
        authenticatedMvc.perform(post("/queries/execute").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.message").value("execution"));
    }

    @Test
    void shouldRejectInvalidInputBeforeAuthenticatedUseCases() throws Exception {
        authenticatedMvc.perform(post("/queries/execute").contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("查询请求参数无效"));
        authenticatedMvc.perform(post("/queries/preview").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceId":"source-1","script":"select 1","scriptType":"INVALID"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("查询请求参数无效"));

        verifyNoInteractions(executeUseCase, previewUseCase, contextFactory);
    }

    @Test
    void shouldRejectInvalidPublicInputBeforeTokenExecution() throws Exception {
        publicMvc.perform(post("/public/queries/execute")
                        .header(PublicQueryController.SHARE_TOKEN_HEADER, "header-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("查询请求参数无效"));
        publicMvc.perform(post("/public/queries/execute")
                        .header(PublicQueryController.SHARE_TOKEN_HEADER, "header-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"viewId":"view-1","columns":[null],"aggregators":[],"filters":[],
                                 "groups":[],"orders":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("查询请求参数无效"));

        verifyNoInteractions(publicQueryExecutor);
    }

    @Test
    void shouldReturnNotFoundForRemovedQueryEndpoints() throws Exception {
        MockMvc legacyMvc = MockMvcBuilders.standaloneSetup(
                new DataProviderController(mock(DataProviderService.class)),
                new ShareController(
                        mock(ShareService.class),
                        mock(ShareDownloadSessionManager.class)
                )).build();

        legacyMvc.perform(post("/data-provider/execute")).andExpect(status().isNotFound());
        legacyMvc.perform(post("/data-provider/execute/test")).andExpect(status().isNotFound());
        legacyMvc.perform(post("/shares/execute")).andExpect(status().isNotFound());
    }
}

package yubi.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.web.WebAppConfiguration;
import yubi.core.common.Application;
import yubi.query.api.ExecuteQueryUseCase;
import yubi.query.api.PreviewQueryUseCase;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryExecutionException;
import yubi.query.api.QueryResult;
import yubi.query.api.QueryValidationException;
import yubi.security.manager.AuthenticationAssembler;
import yubi.security.manager.YuBiSecurityManager;
import yubi.security.oauth2.ClientRegistrationRepositoryImpl;
import yubi.server.config.filter.JwtRequestAuthenticationFilter;
import yubi.server.config.security.YuBiAccessDeniedHandler;
import yubi.server.config.security.YuBiAuthenticationEntryPoint;
import yubi.server.controller.PublicQueryController;
import yubi.server.controller.QueryController;
import yubi.server.controller.BaseController;
import yubi.server.query.ServerQueryExecutionContextFactory;
import yubi.server.query.web.PublicQueryExecutor;
import yubi.server.query.web.QueryWebMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringJUnitConfig(QuerySecurityIntegrationTest.TestConfig.class)
@ContextConfiguration(initializers = QuerySecurityIntegrationTest.ApplicationInitializer.class)
@WebAppConfiguration
@TestPropertySource(properties = "yubi.server.path-prefix=/api/v1")
class QuerySecurityIntegrationTest {

    static class ApplicationInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            new Application().setApplicationContext(applicationContext);
        }
    }

    private static final String EXECUTE_BODY = """
            {"viewId":"view-1","columns":[{"alias":"amount","column":["amount"]}],
             "aggregators":[],"filters":[],"groups":[],"orders":[]}
            """;

    private final WebApplicationContext context;
    private MockMvc mockMvc;

    QuerySecurityIntegrationTest(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setUp() {
        PublicQueryExecutor publicQueryExecutor = context.getBean(PublicQueryExecutor.class);
        ExecuteQueryUseCase executeQueryUseCase = context.getBean(ExecuteQueryUseCase.class);
        PreviewQueryUseCase previewQueryUseCase = context.getBean(PreviewQueryUseCase.class);
        ServerQueryExecutionContextFactory contextFactory = context.getBean(ServerQueryExecutionContextFactory.class);
        reset(publicQueryExecutor, executeQueryUseCase, previewQueryUseCase, contextFactory);
        when(publicQueryExecutor.execute(any(), any())).thenReturn(QueryResult.empty());
        when(executeQueryUseCase.execute(any(), any())).thenReturn(QueryResult.empty());
        when(previewQueryUseCase.preview(any(), any())).thenReturn(QueryResult.empty());
        QueryExecutionContext executionContext = mock(QueryExecutionContext.class);
        when(contextFactory.forView(true)).thenReturn(executionContext);
        when(contextFactory.forSource()).thenReturn(executionContext);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void shouldRequireAuthenticationForPrivateQuery() throws Exception {
        mockMvc.perform(post("/api/v1/queries/execute").contentType("application/json").content(EXECUTE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowPublicQueryWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/public/queries/execute")
                        .header(PublicQueryController.SHARE_TOKEN_HEADER, "share-token")
                        .contentType("application/json").content(EXECUTE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAuthenticatedPrivateQuery() throws Exception {
        mockMvc.perform(post("/api/v1/queries/execute").with(user("query-user"))
                        .contentType("application/json").content(EXECUTE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPreferQueryAdviceForStableQueryStatuses() throws Exception {
        ExecuteQueryUseCase useCase = context.getBean(ExecuteQueryUseCase.class);

        doThrow(new QueryValidationException("invalid")).when(useCase).execute(any(), any());
        performAuthenticatedQuery().andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid"));

        doThrow(new QueryAccessDeniedException("denied", null)).when(useCase).execute(any(), any());
        performAuthenticatedQuery().andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("denied"));

        doThrow(new QueryDefinitionException("definition", null)).when(useCase).execute(any(), any());
        performAuthenticatedQuery().andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("definition"));

        doThrow(new QueryExecutionException("execution", null)).when(useCase).execute(any(), any());
        performAuthenticatedQuery().andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("execution"));
    }

    @Test
    void shouldKeepGlobalAdviceForNonQueryExceptions() throws Exception {
        mockMvc.perform(post("/api/v1/non-query/failure").with(user("query-user")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("non-query failure"));
    }

    private org.springframework.test.web.servlet.ResultActions performAuthenticatedQuery() throws Exception {
        return mockMvc.perform(post("/api/v1/queries/execute").with(user("query-user"))
                .contentType("application/json").content(EXECUTE_BODY));
    }

    @Configuration
    @EnableWebMvc
    @Import({WebSecurityConfig.class, WebMvcConfig.class, QueryWebExceptionHandler.class,
            WebExceptionHandler.class})
    static class TestConfig {

        @Bean
        Application application() {
            return new Application();
        }

        @Bean
        QueryController queryController(ExecuteQueryUseCase executeQueryUseCase,
                                        PreviewQueryUseCase previewQueryUseCase,
                                        QueryWebMapper mapper,
                                        ServerQueryExecutionContextFactory contextFactory) {
            return new QueryController(executeQueryUseCase, previewQueryUseCase, mapper, contextFactory);
        }

        @Bean
        PublicQueryController publicQueryController(PublicQueryExecutor executor, QueryWebMapper mapper) {
            return new PublicQueryController(executor, mapper);
        }

        @Bean
        NonQueryController nonQueryController() {
            return new NonQueryController();
        }

        @Bean
        QueryWebMapper queryWebMapper() {
            return new QueryWebMapper();
        }

        @Bean
        ExecuteQueryUseCase executeQueryUseCase() {
            return mock(ExecuteQueryUseCase.class);
        }

        @Bean
        PreviewQueryUseCase previewQueryUseCase() {
            return mock(PreviewQueryUseCase.class);
        }

        @Bean
        PublicQueryExecutor publicQueryExecutor() {
            return mock(PublicQueryExecutor.class);
        }

        @Bean
        ServerQueryExecutionContextFactory contextFactory() {
            return mock(ServerQueryExecutionContextFactory.class);
        }

        @Bean
        YuBiSecurityManager securityManager() {
            return mock(YuBiSecurityManager.class);
        }

        @Bean
        AuthenticationAssembler authenticationAssembler() {
            return mock(AuthenticationAssembler.class);
        }

        @Bean
        JwtRequestAuthenticationFilter jwtRequestAuthenticationFilter(AuthenticationAssembler assembler) {
            return new JwtRequestAuthenticationFilter(assembler);
        }

        @Bean
        YuBiAuthenticationEntryPoint authenticationEntryPoint() {
            return new YuBiAuthenticationEntryPoint();
        }

        @Bean
        YuBiAccessDeniedHandler accessDeniedHandler() {
            return new YuBiAccessDeniedHandler();
        }

        @Bean
        Oauth2AuthenticationSuccessHandler authenticationSuccessHandler() {
            return mock(Oauth2AuthenticationSuccessHandler.class);
        }

        @Bean
        Oauth2AuthenticationFailureHandler authenticationFailureHandler() {
            return mock(Oauth2AuthenticationFailureHandler.class);
        }

        @Bean
        ClientRegistrationRepositoryImpl clientRegistrations() {
            return mock(ClientRegistrationRepositoryImpl.class);
        }
    }

    @RestController
    @RequestMapping("/non-query")
    static class NonQueryController extends BaseController {
        @PostMapping("/failure")
        void failure() {
            throw new IllegalStateException("non-query failure");
        }
    }
}

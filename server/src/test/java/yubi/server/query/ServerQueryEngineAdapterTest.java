package yubi.server.query;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import yubi.core.base.consts.Const;
import yubi.core.common.Application;
import yubi.core.data.provider.DataProviderManager;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.ExecuteParam;
import yubi.core.data.provider.QueryScript;
import yubi.core.entity.Source;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Execution;
import yubi.query.domain.QueryModels.Page;
import yubi.query.domain.QueryModels.Plan;
import yubi.query.domain.QueryModels.Script;
import yubi.query.domain.QueryModels.ScriptType;
import yubi.query.domain.QueryModels.SourceReference;
import yubi.security.util.AESUtil;
import yubi.server.service.SourceService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServerQueryEngineAdapterTest {

    @Test
    void shouldReadAndDecryptSourceOnlyWhenExecutingProvider() throws Exception {
        String secret = "query-engine-adapter-test";
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.security.token.secret", "d@a$t%a^r&a*t")).thenReturn(secret);
        new Application().setApplicationContext(applicationContext);

        Source source = new Source();
        source.setId("source-1");
        source.setOrgId("org-1");
        source.setName("orders");
        source.setType("JDBC");
        source.setConfig("{\"password\":\"" + Const.ENCRYPT_FLAG
                + AESUtil.encrypt("secret-password", secret) + "\",\"serverAggregate\":true}");
        SourceService sourceService = mock(SourceService.class);
        when(sourceService.retrieve("source-1", false)).thenReturn(source);
        DataProviderManager providerManager = mock(DataProviderManager.class);
        when(providerManager.execute(any(), any(), any())).thenReturn(new Dataframe("result-1"));
        ServerQueryEngineAdapter adapter = new ServerQueryEngineAdapter(
                providerManager, sourceService, new ServerSourceConfigMapper());

        adapter.execute(plan());

        ArgumentCaptor<DataProviderSource> sourceCaptor = ArgumentCaptor.forClass(DataProviderSource.class);
        ArgumentCaptor<QueryScript> scriptCaptor = ArgumentCaptor.forClass(QueryScript.class);
        ArgumentCaptor<ExecuteParam> executeCaptor = ArgumentCaptor.forClass(ExecuteParam.class);
        verify(providerManager).execute(sourceCaptor.capture(), scriptCaptor.capture(), executeCaptor.capture());
        assertEquals("secret-password", sourceCaptor.getValue().getProperties().get("password"));
        assertEquals(yubi.core.data.provider.ScriptType.STRUCT, scriptCaptor.getValue().getScriptType());
        assertTrue(executeCaptor.getValue().isServerAggregate());
    }

    @Test
    void changedSourceOrganizationMustFailBeforeConfigurationParsing() {
        Source source = new Source();
        source.setId("source-1");
        source.setOrgId("other-org");
        SourceService sourceService = mock(SourceService.class);
        when(sourceService.retrieve("source-1", false)).thenReturn(source);
        DataProviderManager providerManager = mock(DataProviderManager.class);
        ServerSourceConfigMapper sourceConfigMapper = mock(ServerSourceConfigMapper.class);
        ServerQueryEngineAdapter adapter = new ServerQueryEngineAdapter(
                providerManager, sourceService, sourceConfigMapper);

        assertThrows(IllegalStateException.class, () -> adapter.execute(plan()));

        verifyNoInteractions(sourceConfigMapper, providerManager);
    }

    private Plan plan() {
        Script script = new Script(false, "source-1", "view-1", "{\"columns\":[]}",
                ScriptType.STRUCT, List.of(), java.util.Map.of());
        Execution execution = new Execution(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), Set.of(new ColumnSelection(null, List.of("*"))), Page.request(1, 1000, false),
                false, false, 0);
        return new Plan(new SourceReference("source-1", "org-1"), script, execution);
    }
}

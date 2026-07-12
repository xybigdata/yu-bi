package yubi.server.query;

import org.junit.jupiter.api.Test;
import yubi.core.entity.Source;
import yubi.core.entity.View;
import yubi.core.mappers.ext.SourceMapperExt;
import yubi.query.domain.QueryModels.Definition;
import yubi.server.service.ViewService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerQueryDefinitionAdapterTest {

    @Test
    void shouldReadPureProjectionAndParseSchemaWithoutSourceConfig() {
        View view = new View();
        view.setId("view-1");
        view.setOrgId("org-1");
        view.setSourceId("source-1");
        view.setType("SQL");
        view.setScript("select amount from orders");
        view.setModel("{\"columns\":{\"orders.amount\":{\"name\":[\"orders\",\"amount\"],\"type\":\"NUMERIC\"}}}");
        Source source = new Source();
        source.setId("source-1");
        source.setOrgId("org-1");
        source.setName("orders");
        source.setConfig("不应进入 Query 定义的敏感配置");

        ViewService viewService = mock(ViewService.class);
        SourceMapperExt sourceMapper = mock(SourceMapperExt.class);
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        when(sourceMapper.selectQueryAccessProjection("source-1")).thenReturn(source);

        ServerQueryDefinitionAdapter adapter = new ServerQueryDefinitionAdapter(viewService, sourceMapper);
        Definition definition = adapter.load("view-1");

        assertEquals("source-1", definition.sourceId());
        assertEquals(java.util.Set.of("orders.amount"), definition.schema().keySet());
        assertEquals("source-1", adapter.loadSource("source-1").id());
    }
}

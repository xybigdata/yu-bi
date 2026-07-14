package yubi.server.common;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.data.provider.DataProviderConfigTemplate;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.data.provider.QueryScript;
import yubi.core.data.provider.ScriptVariable;
import yubi.server.base.params.DashboardCreateParam;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.base.params.VariableCreateParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DownloadQueryRequestDeserializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void shouldIgnoreNullPrimitivePageInfoFields() throws Exception {
        DownloadQueryRequest param = objectMapper.readValue("""
                {
                  "viewId": "view-1",
                  "columns": [],
                  "aggregators": [],
                  "groups": [],
                  "pageInfo": {
                    "pageNo": null,
                    "pageSize": null,
                    "total": null,
                    "countTotal": null
                  },
                  "cacheExpires": 0,
                  "cache": false,
                  "concurrencyControl": true,
                  "script": false,
                  "analytics": false
                }
                """, DownloadQueryRequest.class);

        assertNotNull(param.getPageInfo());
        assertEquals(0L, param.getPageInfo().getPageNo());
        assertEquals(0L, param.getPageInfo().getPageSize());
        assertEquals(0L, param.getPageInfo().getTotal());
        assertFalse(param.getPageInfo().isCountTotal());
    }

    @Test
    void shouldReadDownloadQueryRequestWhenPrimitiveFieldsAreMissing() throws Exception {
        DownloadQueryRequest param = objectMapper.readValue("""
                {
                  "viewId": "view-1",
                  "columns": [],
                  "aggregators": [],
                  "groups": [],
                  "pageInfo": {}
                }
                """, DownloadQueryRequest.class);

        assertNotNull(param.getPageInfo());
        assertEquals(0L, param.getPageInfo().getPageNo());
        assertEquals(0L, param.getPageInfo().getPageSize());
        assertEquals(0, param.getCacheExpires());
        assertFalse(param.isCache());
        assertFalse(param.isScript());
        assertFalse(param.isAnalytics());
    }

    @Test
    void shouldIgnoreNullPrimitiveViewExecuteFields() throws Exception {
        DownloadQueryRequest param = objectMapper.readValue("""
                {
                  "concurrencyControl": null,
                  "cache": null,
                  "cacheExpires": null,
                  "script": null,
                  "analytics": null,
                  "concurrencyControlMode": "DIRTYREAD"
                }
                """, DownloadQueryRequest.class);

        assertFalse(param.isConcurrencyControl());
        assertFalse(param.isCache());
        assertEquals(0, param.getCacheExpires());
        assertFalse(param.isScript());
        assertFalse(param.isAnalytics());
        assertEquals("DIRTYREAD", param.getConcurrencyControlMode());
    }

    @Test
    void shouldIgnoreNullPrimitiveFieldsForOtherRequestParams() throws Exception {
        DownloadCreateParam downloadCreateParam = objectMapper.readValue(
                "{\"imageWidth\":null}",
                DownloadCreateParam.class
        );
        DashboardCreateParam dashboardCreateParam = objectMapper.readValue(
                "{\"index\":null}",
                DashboardCreateParam.class
        );
        VariableCreateParam variableCreateParam = objectMapper.readValue(
                "{\"expression\":null}",
                VariableCreateParam.class
        );

        assertEquals(0, downloadCreateParam.getImageWidth());
        assertEquals(0D, dashboardCreateParam.getIndex());
        assertFalse(variableCreateParam.isExpression());
    }

    @Test
    void shouldIgnoreNullPrimitiveFieldsForNestedScriptVariables() throws Exception {
        DataProviderSource dataProviderSource = objectMapper.readValue("""
                {
                  "sourceId": "source-1",
                  "variables": [
                    {
                      "name": "tenant",
                      "expression": null,
                      "disabled": null
                    }
                  ]
                }
                """, DataProviderSource.class);
        QueryScript queryScript = objectMapper.readValue(
                "{\"test\":null}",
                QueryScript.class
        );
        DataProviderConfigTemplate.Attribute attribute = objectMapper.readValue(
                "{\"required\":null,\"encrypt\":null}",
                DataProviderConfigTemplate.Attribute.class
        );

        ScriptVariable sourceVariable = dataProviderSource.getVariables().get(0);

        assertFalse(sourceVariable.isExpression());
        assertFalse(sourceVariable.isDisabled());
        assertFalse(queryScript.isTest());
        assertFalse(attribute.isRequired());
        assertFalse(attribute.isEncrypt());
    }
}

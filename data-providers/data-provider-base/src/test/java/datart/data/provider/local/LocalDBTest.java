package datart.data.provider.local;

import datart.core.base.PageInfo;
import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.Dataframes;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.QueryScript;
import datart.core.data.provider.ScriptType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDBTest {

    @Test
    void shouldExecuteLocalQueryWithInMemoryH2() throws Exception {
        Dataframe dataframe = new Dataframe("test-df");
        dataframe.setName("orders");
        dataframe.setColumns(List.of(
                Column.of(ValueType.STRING, "name"),
                Column.of(ValueType.NUMERIC, "score")
        ));
        dataframe.setRows(List.of(
                List.of("alice", 100),
                List.of("bob", 88),
                List.of("charlie", 76)
        ));

        QueryScript queryScript = new QueryScript();
        queryScript.setScript("SELECT `name`, `score` FROM `orders` ORDER BY `score` DESC");
        queryScript.setScriptType(ScriptType.SQL);
        queryScript.setVariables(List.of());
        queryScript.setSourceId("local-test");

        ExecuteParam executeParam = new ExecuteParam();
        executeParam.setPageInfo(PageInfo.builder().pageNo(1).pageSize(10).build());

        Dataframe result = LocalDB.executeLocalQuery(
                queryScript,
                executeParam,
                Dataframes.of("local-test", dataframe),
                false,
                null
        );

        assertEquals(3, result.getRows().size());
        assertEquals("alice", result.getRows().get(0).get(0));
        assertEquals(100d, result.getRows().get(0).get(1));
        assertEquals("charlie", result.getRows().get(2).get(0));
        assertEquals(3, result.getPageInfo().getTotal());
    }
}

package yubi.data.provider.local;

import yubi.core.base.PageInfo;
import yubi.core.base.consts.ValueType;
import yubi.core.data.provider.Column;
import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.Dataframes;
import yubi.core.data.provider.ExecuteParam;
import yubi.core.data.provider.QueryScript;
import yubi.core.data.provider.ScriptType;
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

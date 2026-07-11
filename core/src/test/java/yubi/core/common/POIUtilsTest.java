package yubi.core.common;

import yubi.core.base.consts.ValueType;
import yubi.core.data.provider.Column;
import yubi.core.data.provider.Dataframe;
import yubi.core.entity.poi.ColumnSetting;
import yubi.core.entity.poi.POISettings;
import yubi.core.entity.poi.format.PoiNumFormat;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class POIUtilsTest {

    @Test
    void shouldWriteAndReadExcelWorkbook() throws Exception {
        Dataframe dataframe = new Dataframe();
        dataframe.setColumns(List.of(
                Column.of(ValueType.STRING, "name"),
                Column.of(ValueType.NUMERIC, "score")
        ));
        dataframe.setRows(List.of(
                List.of("alice", 100),
                List.of("bob", 88)
        ));

        POISettings poiSettings = new POISettings();
        poiSettings.setHeaderRows(Map.of(0, dataframe.getColumns()));
        ColumnSetting nameSetting = new ColumnSetting();
        nameSetting.setIndex(0);
        nameSetting.setNumFormat(new PoiNumFormat());
        nameSetting.setLength("alice".length());
        ColumnSetting scoreSetting = new ColumnSetting();
        scoreSetting.setIndex(1);
        scoreSetting.setNumFormat(new PoiNumFormat());
        scoreSetting.setLength(String.valueOf(100).length());
        poiSettings.setColumnSetting(Map.of(
                0, nameSetting,
                1, scoreSetting
        ));

        Path output = Files.createTempFile("poi-utils-", ".xlsx");
        try (Workbook workbook = POIUtils.createEmpty()) {
            POIUtils.withSheet(workbook, "sheet-1", dataframe, poiSettings);
            POIUtils.save(workbook, output.toString(), true);
        }

        List<List<Object>> rows = POIUtils.loadExcel(output.toString());
        assertEquals(3, rows.size());
        assertEquals(List.of("name", "score"), rows.get(0));
        assertEquals("alice", rows.get(1).get(0));
        assertEquals(100d, rows.get(1).get(1));
        assertEquals("bob", rows.get(2).get(0));
        assertEquals(88d, rows.get(2).get(1));
    }

    @Test
    void shouldCreateSafeUniqueSheetNames() throws Exception {
        try (Workbook workbook = POIUtils.createEmpty()) {
            String first = POIUtils.uniqueSheetName(workbook, "test1_copy");
            workbook.createSheet(first);
            String second = POIUtils.uniqueSheetName(workbook, "test1_copy");
            workbook.createSheet(second);

            assertEquals("test1_copy", first);
            assertEquals("test1_copy (2)", second);
            assertEquals("test1_copy (3)", POIUtils.uniqueSheetName(workbook, "test1_copy"));
            assertEquals("invalid name", POIUtils.uniqueSheetName(workbook, "invalid/name"));
        }
    }
}

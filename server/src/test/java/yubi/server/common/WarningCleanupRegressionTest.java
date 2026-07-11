package yubi.server.common;

import tools.jackson.databind.json.JsonMapper;
import yubi.core.base.consts.FileOwner;
import yubi.core.base.exception.BaseException;
import yubi.core.entity.poi.format.PercentageFormat;
import yubi.server.base.dto.ResponseData;
import yubi.server.base.dto.chart.ChartColumn;
import yubi.server.base.dto.chart.WidgetConfig;
import yubi.server.service.impl.FileServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningCleanupRegressionTest {

    private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void shouldPreserveGenericResponseFactories() {
        ResponseData<String> success = ResponseData.success("value");
        ResponseData<String> failure = ResponseData.failure("failed");

        assertTrue(success.isSuccess());
        assertEquals("value", success.getData());
        assertEquals("failed", failure.getMessage());
    }

    @Test
    void shouldReadJacksonThreeStringValues() {
        ChartColumn column = new ChartColumn();
        column.setFormat(OBJECT_MAPPER.readTree("""
                {"type":"percentage","percentage":{"decimalPlaces":2}}
                """));
        WidgetConfig widgetConfig = new WidgetConfig();
        widgetConfig.setContent(OBJECT_MAPPER.readTree("""
                {"dataChart":{"config":"serialized-config"}}
                """));

        PercentageFormat format = assertInstanceOf(PercentageFormat.class, column.getNumFormat());
        assertEquals("0.00", format.getDecimalPlaces());
        assertEquals("serialized-config", widgetConfig.getChartConfig());
    }

    @Test
    void shouldRejectNonVisualizationFileOwners() {
        FileServiceImpl fileService = new FileServiceImpl();

        assertThrows(BaseException.class,
                () -> fileService.uploadFile(FileOwner.DOWNLOAD, "owner", null, null));
    }
}

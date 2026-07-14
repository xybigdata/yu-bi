package yubi.agent.application;

import org.junit.jupiter.api.Test;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.StructuredValue.TextValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.query.api.DataAssetDetail;
import yubi.query.api.DataAssetSummary;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.api.QueryMetadataToolSchemas;
import yubi.query.api.SearchDataAssetsRequest;
import yubi.query.api.SearchDataAssetsResult;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ValueType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataToolOutputLimitTest {

    private static final QueryExecutionContext CONTEXT = new QueryExecutionContext(
            Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1");

    @Test
    void searchOutputMustIncludeEnvelopeInByteLimit() {
        String huge = "asset".repeat(10_000);
        Metadata metadata = new Metadata();
        metadata.search = new SearchDataAssetsResult(List.of(
                new DataAssetSummary(huge, huge, huge),
                new DataAssetSummary("view-2", "Orders", "Facts")));
        ToolResultLimits limits = new ToolResultLimits(10, 128);

        ToolOutput output = new SearchDataAssetsTool(metadata, limits)
                .execute(object("query", text("orders")), CONTEXT);

        assertTrue(output.size().truncated());
        assertTrue(output.size().returnedBytes() <= limits.maximumBytes());
        assertEquals(StructuredValues.utf8Bytes(output.value()), output.size().returnedBytes());
    }

    @Test
    void describeOutputMustRetainEveryRequiredFieldAtMinimumByteLimits() {
        String huge = "metadata".repeat(10_000);
        for (long maximumBytes : List.of(128L, 256L)) {
            Metadata metadata = new Metadata();
            metadata.detail = new DataAssetDetail(huge, huge, huge,
                    List.of(new DataAssetDetail.FieldDescription(
                            List.of("orders", huge), huge, ValueType.STRING, huge)),
                    List.of(), List.of(), Optional.of(huge));
            ToolResultLimits limits = new ToolResultLimits(10, maximumBytes);

            ToolOutput output = new DescribeDataAssetTool(metadata, limits).execute(
                    object("assetId", text("view-1"), "includeScript", StructuredValue.bool(true)), CONTEXT);

            for (String required : QueryMetadataToolSchemas.describeDataAsset().outputSchema().required()) {
                assertTrue(output.value().values().containsKey(required),
                        () -> maximumBytes + " 字节输出缺少必填字段 " + required);
            }
            assertInstanceOf(TextValue.class, output.value().values().get("id"));
            assertInstanceOf(TextValue.class, output.value().values().get("name"));
            assertInstanceOf(ArrayValue.class, output.value().values().get("fields"));
            assertInstanceOf(ArrayValue.class, output.value().values().get("variables"));
            assertInstanceOf(ArrayValue.class, output.value().values().get("functions"));
            assertTrue(output.size().truncated());
            assertTrue(output.size().returnedBytes() <= limits.maximumBytes());
            assertEquals(StructuredValues.utf8Bytes(output.value()), output.size().returnedBytes());
        }
    }

    private static ObjectValue object(Object... entries) {
        Map<String, StructuredValue> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], (StructuredValue) entries[index + 1]);
        }
        return new ObjectValue(values);
    }

    private static StructuredValue.TextValue text(String value) {
        return StructuredValue.text(value);
    }

    private static final class Metadata implements QueryMetadataUseCase {
        private SearchDataAssetsResult search;
        private DataAssetDetail detail;

        @Override
        public SearchDataAssetsResult search(SearchDataAssetsRequest request, QueryExecutionContext context) {
            return search;
        }

        @Override
        public DataAssetDetail describe(DescribeDataAssetRequest request, QueryExecutionContext context) {
            return detail;
        }
    }
}

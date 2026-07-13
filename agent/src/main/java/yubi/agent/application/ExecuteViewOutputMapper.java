package yubi.agent.application;

import yubi.agent.domain.AgentModels.ResultSize;
import yubi.agent.domain.AgentModels.ToolOutput;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.domain.ToolResultLimits;
import yubi.query.api.QueryResult;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.Page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ExecuteViewOutputMapper {

    private static final long MAXIMUM_METADATA_TEXT_BYTES = 512;
    private static final long MAXIMUM_CELL_TEXT_BYTES = 4_096;

    private final ToolResultLimits limits;

    ExecuteViewOutputMapper(ToolResultLimits limits) {
        this.limits = limits;
    }

    ToolOutput map(QueryResult result, ExecuteViewInput input) {
        int returnLimit = Math.toIntExact(input.returnLimit(limits.maximumItems()));
        Projection id = text(result.id(), metadataTextLimit());
        Projection name = text(result.name(), metadataTextLimit());
        Projection visualizationType = text(result.visualizationType(), metadataTextLimit());
        Projection visualizationId = text(result.visualizationId(), metadataTextLimit());
        Projection page = page(result.page(), input, returnLimit);

        Projection returnedPage = null;
        Projection returnedId = null;
        Projection returnedName = null;
        Projection returnedVisualizationType = null;
        Projection returnedVisualizationId = null;
        List<StructuredValue> returnedColumns = new ArrayList<>();
        List<StructuredValue> returnedRows = new ArrayList<>();
        boolean truncated = id.truncated() || name.truncated() || visualizationType.truncated()
                || visualizationId.truncated() || page.truncated() || hasMore(result, input, returnLimit);

        if (page.value() != null && fits(returnedId, returnedName, returnedVisualizationType,
                returnedVisualizationId, returnedColumns, returnedRows, page)) {
            returnedPage = page;
        } else if (page.value() != null) {
            truncated = true;
        }
        if (id.value() != null && fits(id, returnedName, returnedVisualizationType,
                returnedVisualizationId, returnedColumns, returnedRows, returnedPage)) {
            returnedId = id;
        } else if (id.value() != null) {
            truncated = true;
        }
        if (name.value() != null && fits(returnedId, name, returnedVisualizationType,
                returnedVisualizationId, returnedColumns, returnedRows, returnedPage)) {
            returnedName = name;
        } else if (name.value() != null) {
            truncated = true;
        }
        if (visualizationType.value() != null && fits(returnedId, returnedName, visualizationType,
                returnedVisualizationId, returnedColumns, returnedRows, returnedPage)) {
            returnedVisualizationType = visualizationType;
        } else if (visualizationType.value() != null) {
            truncated = true;
        }
        if (visualizationId.value() != null && fits(returnedId, returnedName, returnedVisualizationType,
                visualizationId, returnedColumns, returnedRows, returnedPage)) {
            returnedVisualizationId = visualizationId;
        } else if (visualizationId.value() != null) {
            truncated = true;
        }

        long observedColumnsBytes = 2;
        boolean acceptingColumns = true;
        for (int index = 0; index < result.columns().size(); index++) {
            Projection column = column(result.columns().get(index));
            observedColumnsBytes += column.observedBytes() + (index == 0 ? 0 : 1);
            truncated |= column.truncated();
            if (!acceptingColumns) {
                continue;
            }
            returnedColumns.add(column.value());
            if (!fits(returnedId, returnedName, returnedVisualizationType,
                    returnedVisualizationId, returnedColumns, returnedRows, returnedPage)) {
                returnedColumns.removeLast();
                acceptingColumns = false;
                truncated = true;
            }
        }

        long observedRowsBytes = 2;
        boolean acceptingRows = true;
        for (int index = 0; index < result.rows().size(); index++) {
            Projection row = row(result.rows().get(index));
            observedRowsBytes += row.observedBytes() + (index == 0 ? 0 : 1);
            truncated |= row.truncated();
            if (!acceptingRows || returnedRows.size() >= returnLimit) {
                acceptingRows = false;
                truncated = true;
                continue;
            }
            returnedRows.add(row.value());
            if (!fits(returnedId, returnedName, returnedVisualizationType,
                    returnedVisualizationId, returnedColumns, returnedRows, returnedPage)) {
                returnedRows.removeLast();
                acceptingRows = false;
                truncated = true;
            }
        }

        ObjectValue output = output(returnedId, returnedName, returnedVisualizationType,
                returnedVisualizationId, returnedColumns, returnedRows, returnedPage);
        long returnedBytes = StructuredValues.utf8Bytes(output);
        long observedBytes = objectBytes(
                "id", size(id),
                "name", size(name),
                "visualizationType", size(visualizationType),
                "visualizationId", size(visualizationId),
                "columns", observedColumnsBytes,
                "rows", observedRowsBytes,
                "page", size(page));
        truncated |= returnedBytes < observedBytes;
        if (returnedBytes > limits.maximumBytes()) {
            throw new IllegalStateException("execute_view 结果超过确定性字节上限");
        }
        return new ToolOutput(output, new ResultSize(result.rows().size(), returnedRows.size(),
                observedBytes, returnedBytes, returnLimit, limits.maximumBytes(), truncated));
    }

    private boolean hasMore(QueryResult result, ExecuteViewInput input, int returnLimit) {
        if (result.rows().size() > returnLimit) {
            return true;
        }
        boolean totalProbe = input.countTotal() || input.pageNo() > 1;
        if (!totalProbe) {
            return false;
        }
        Page internalPage = result.page();
        if (internalPage == null || !internalPage.countTotal()) {
            return result.rows().size() >= returnLimit;
        }
        long offset = saturatedMultiply(input.pageNo() - 1L, returnLimit);
        long observedThrough = saturatedAdd(offset, result.rows().size());
        return internalPage.total() > observedThrough;
    }

    private long saturatedMultiply(long left, long right) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private boolean fits(Projection id,
                         Projection name,
                         Projection visualizationType,
                         Projection visualizationId,
                         List<StructuredValue> columns,
                         List<StructuredValue> rows,
                         Projection page) {
        return StructuredValues.utf8Bytes(output(id, name, visualizationType, visualizationId,
                columns, rows, page)) <= limits.maximumBytes();
    }

    private ObjectValue output(Projection id,
                               Projection name,
                               Projection visualizationType,
                               Projection visualizationId,
                               List<StructuredValue> columns,
                               List<StructuredValue> rows,
                               Projection page) {
        return StructuredValues.object(
                "id", value(id),
                "name", value(name),
                "visualizationType", value(visualizationType),
                "visualizationId", value(visualizationId),
                "columns", StructuredValues.array(columns),
                "rows", StructuredValues.array(rows),
                "page", value(page));
    }

    private Projection column(ColumnMetadata column) {
        List<Projection> names = column.name().stream()
                .map(value -> text(value, metadataTextLimit()))
                .toList();
        return object(
                "name", array(names),
                "valueType", text(column.type().name(), metadataTextLimit()),
                "format", text(column.format(), metadataTextLimit()));
    }

    private Projection row(List<Object> row) {
        return array(row.stream().map(this::cell).toList());
    }

    private Projection cell(Object value) {
        if (value == null) {
            return object("isNull", fixed(StructuredValues.bool(true)));
        }
        StructuredValues.BoundedText text = StructuredValues.boundedCell(value, cellTextLimit());
        Projection projectedText = new Projection(text.value(), text.observedBytes(),
                text.returnedBytes(), text.truncated());
        return object(
                "value", projectedText,
                "isNull", fixed(StructuredValues.bool(false)));
    }

    private Projection page(Page internalPage, ExecuteViewInput input, int returnLimit) {
        long publicTotal = input.countTotal() && internalPage != null ? internalPage.total() : 0;
        return object(
                "pageNo", fixed(StructuredValues.integer(input.pageNo())),
                "pageSize", fixed(StructuredValues.integer(returnLimit)),
                "total", fixed(StructuredValues.integer(publicTotal)),
                "countTotal", fixed(StructuredValues.bool(input.countTotal())));
    }

    private Projection text(String value, long maximumBytes) {
        StructuredValues.BoundedText bounded = StructuredValues.boundedText(value, maximumBytes);
        return new Projection(bounded.value(), bounded.observedBytes(),
                bounded.returnedBytes(), bounded.truncated());
    }

    private Projection fixed(StructuredValue value) {
        long bytes = StructuredValues.utf8Bytes(value);
        return new Projection(value, bytes, bytes, false);
    }

    private Projection array(List<Projection> values) {
        List<StructuredValue> returned = values.stream().map(Projection::value).toList();
        long observedBytes = 2;
        long returnedBytes = 2;
        boolean truncated = false;
        for (int index = 0; index < values.size(); index++) {
            Projection value = values.get(index);
            observedBytes += value.observedBytes() + (index == 0 ? 0 : 1);
            returnedBytes += value.returnedBytes() + (index == 0 ? 0 : 1);
            truncated |= value.truncated();
        }
        return new Projection(StructuredValues.array(returned), observedBytes, returnedBytes, truncated);
    }

    private Projection object(Object... entries) {
        Map<String, StructuredValue> returned = new LinkedHashMap<>();
        long observedBytes = 2;
        long returnedBytes = 2;
        boolean truncated = false;
        int count = 0;
        for (int index = 0; index < entries.length; index += 2) {
            String key = (String) entries[index];
            Projection value = (Projection) entries[index + 1];
            if (value == null || value.value() == null) {
                continue;
            }
            returned.put(key, value.value());
            long overhead = StructuredValues.textUtf8Bytes(key) + 1 + (count++ == 0 ? 0 : 1);
            observedBytes += overhead + value.observedBytes();
            returnedBytes += overhead + value.returnedBytes();
            truncated |= value.truncated();
        }
        return new Projection(new ObjectValue(returned), observedBytes, returnedBytes, truncated);
    }

    private long objectBytes(Object... entries) {
        long bytes = 2;
        int count = 0;
        for (int index = 0; index < entries.length; index += 2) {
            Long valueBytes = (Long) entries[index + 1];
            if (valueBytes == null) {
                continue;
            }
            bytes += StructuredValues.textUtf8Bytes((String) entries[index]) + 1 + valueBytes
                    + (count++ == 0 ? 0 : 1);
        }
        return bytes;
    }

    private StructuredValue value(Projection projection) {
        return projection == null ? null : projection.value();
    }

    private Long size(Projection projection) {
        return projection == null || projection.value() == null ? null : projection.observedBytes();
    }

    private long metadataTextLimit() {
        return Math.max(2, Math.min(MAXIMUM_METADATA_TEXT_BYTES, limits.maximumBytes() / 4));
    }

    private long cellTextLimit() {
        return Math.max(2, Math.min(MAXIMUM_CELL_TEXT_BYTES, limits.maximumBytes() / 2));
    }

    private record Projection(StructuredValue value,
                              long observedBytes,
                              long returnedBytes,
                              boolean truncated) {
    }
}

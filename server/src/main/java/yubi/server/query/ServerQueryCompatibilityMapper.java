package yubi.server.query;

import org.springframework.stereotype.Component;
import yubi.core.base.PageInfo;
import yubi.core.data.provider.Column;
import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.ForeignKey;
import yubi.core.data.provider.ScriptVariable;
import yubi.core.data.provider.SelectColumn;
import yubi.core.data.provider.SingleTypedValue;
import yubi.core.data.provider.sql.AggregateOperator;
import yubi.core.data.provider.sql.FilterOperator;
import yubi.core.data.provider.sql.OrderOperator;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.PreviewQueryCommand;
import yubi.query.api.QueryResult;
import yubi.query.domain.QueryModels.Aggregate;
import yubi.query.domain.QueryModels.AggregateType;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Filter;
import yubi.query.domain.QueryModels.FilterType;
import yubi.query.domain.QueryModels.Order;
import yubi.query.domain.QueryModels.OrderType;
import yubi.query.domain.QueryModels.Page;
import yubi.query.domain.QueryModels.TypedValue;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.Variable;
import yubi.query.domain.QueryModels.VariableType;
import yubi.server.base.params.TestExecuteParam;
import yubi.server.base.params.ViewExecuteParam;

import java.util.Arrays;
import java.util.List;

@Component
public class ServerQueryCompatibilityMapper {

    public ExecuteQueryCommand toCommand(ViewExecuteParam source) {
        return new ExecuteQueryCommand(source.getViewId(),
                source.getKeywords() == null ? null : source.getKeywords().stream().map(Enum::name).toList(),
                mapColumns(source.getColumns()),
                source.getFunctionColumns() == null ? null : source.getFunctionColumns().stream()
                        .map(value -> new yubi.query.domain.QueryModels.FunctionColumn(value.getAlias(), value.getSnippet()))
                        .toList(),
                source.getAggregators() == null ? null : source.getAggregators().stream().map(this::aggregate).toList(),
                source.getFilters() == null ? null : source.getFilters().stream().map(this::filter).toList(),
                source.getGroups() == null ? null : source.getGroups().stream()
                        .map(value -> new yubi.query.domain.QueryModels.Group(value.getAlias(), path(value.getColumnNames(false, null))))
                        .toList(),
                source.getOrders() == null ? null : source.getOrders().stream().map(this::order).toList(),
                page(source.getPageInfo()), source.getParams(), source.isConcurrencyControl(), source.isCache(),
                source.getCacheExpires(), source.isScript());
    }

    public PreviewQueryCommand toCommand(TestExecuteParam source) {
        return new PreviewQueryCommand(source.getSourceId(), source.getScript(),
                source.getScriptType() == null ? null : yubi.query.domain.QueryModels.ScriptType.valueOf(source.getScriptType().name()),
                mapColumns(source.getColumns()),
                source.getVariables() == null ? null : source.getVariables().stream().map(this::variable).toList(),
                source.getSize());
    }

    public Dataframe toDataframe(QueryResult source) {
        Dataframe dataframe = source.id() == null ? Dataframe.empty() : new Dataframe(source.id());
        dataframe.setName(source.name());
        dataframe.setVizType(source.visualizationType());
        dataframe.setVizId(source.visualizationId());
        dataframe.setColumns(source.columns().stream().map(this::column).toList());
        dataframe.setRows(source.rows());
        dataframe.setPageInfo(page(source.page()));
        dataframe.setScript(source.script());
        return dataframe;
    }

    private List<ColumnSelection> mapColumns(List<SelectColumn> values) {
        return values == null ? null : values.stream()
                .map(value -> new ColumnSelection(value.getAlias(), path(value.getColumnNames(false, null))))
                .toList();
    }

    private Aggregate aggregate(AggregateOperator value) {
        return new Aggregate(AggregateType.valueOf(value.getSqlOperator().name()), value.getAlias(),
                path(value.getColumnNames(false, null)));
    }

    private Filter filter(FilterOperator value) {
        List<TypedValue> values = value.getValues() == null ? List.of() : Arrays.stream(value.getValues())
                .map(this::typedValue).toList();
        return new Filter(value.getAggOperator() == null ? null : AggregateType.valueOf(value.getAggOperator().name()),
                FilterType.valueOf(value.getSqlOperator().name()), path(value.getColumnNames(false, null)), values);
    }

    private Order order(OrderOperator value) {
        return new Order(value.getAggOperator() == null ? null : AggregateType.valueOf(value.getAggOperator().name()),
                OrderType.valueOf(value.getOperator().name()), path(value.getColumnNames(false, null)));
    }

    private TypedValue typedValue(SingleTypedValue value) {
        return new TypedValue(value.getValue(), ValueType.valueOf(value.getValueType().name()), value.getFormat());
    }

    private Variable variable(ScriptVariable value) {
        return new Variable(value.getName(), VariableType.valueOf(value.getType().name()),
                ValueType.valueOf(value.getValueType().name()), value.getValues(), value.isExpression(),
                value.isDisabled(), value.getFormat());
    }

    private Page page(PageInfo value) {
        return value == null ? null : new Page(value.getPageNo(), value.getPageSize(), value.getTotal(), value.isCountTotal());
    }

    private PageInfo page(Page value) {
        return value == null ? null : PageInfo.builder().pageNo(value.pageNo()).pageSize(value.pageSize())
                .total(value.total()).countTotal(value.countTotal()).build();
    }

    private Column column(ColumnMetadata value) {
        Column column = Column.of(yubi.core.base.consts.ValueType.valueOf(value.type().name()),
                value.name().toArray(String[]::new));
        column.setFmt(value.format());
        column.setForeignKeys(value.foreignKeys().stream().map(this::foreignKey).toList());
        return column;
    }

    private ForeignKey foreignKey(yubi.query.domain.QueryModels.ForeignKey value) {
        ForeignKey foreignKey = new ForeignKey();
        foreignKey.setDatabase(value.database());
        foreignKey.setTable(value.table());
        foreignKey.setColumn(value.column());
        return foreignKey;
    }

    private List<String> path(String[] values) {
        return values == null ? List.of() : List.of(values);
    }
}

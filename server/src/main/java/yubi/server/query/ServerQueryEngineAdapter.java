package yubi.server.query;

import org.springframework.stereotype.Component;
import yubi.core.base.PageInfo;
import yubi.core.data.provider.Column;
import yubi.core.data.provider.DataProviderManager;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.ExecuteParam;
import yubi.core.data.provider.QueryScript;
import yubi.core.data.provider.ScriptVariable;
import yubi.core.data.provider.SelectColumn;
import yubi.core.data.provider.SingleTypedValue;
import yubi.core.data.provider.sql.AggregateOperator;
import yubi.core.data.provider.sql.FilterOperator;
import yubi.core.data.provider.sql.FunctionColumn;
import yubi.core.data.provider.sql.GroupByOperator;
import yubi.core.data.provider.sql.OrderOperator;
import yubi.core.data.provider.sql.SelectKeyword;
import yubi.core.entity.Source;
import yubi.query.domain.QueryModels.Aggregate;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.EngineResult;
import yubi.query.domain.QueryModels.Filter;
import yubi.query.domain.QueryModels.ForeignKey;
import yubi.query.domain.QueryModels.Order;
import yubi.query.domain.QueryModels.Page;
import yubi.query.domain.QueryModels.Plan;
import yubi.query.domain.QueryModels.SourceReference;
import yubi.query.domain.QueryModels.TypedValue;
import yubi.query.domain.QueryModels.Variable;
import yubi.query.port.QueryEnginePort;
import yubi.server.service.SourceService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ServerQueryEngineAdapter implements QueryEnginePort {

    private final DataProviderManager dataProviderManager;
    private final SourceService sourceService;
    private final ServerSourceConfigMapper sourceConfigMapper;

    public ServerQueryEngineAdapter(DataProviderManager dataProviderManager,
                                    SourceService sourceService,
                                    ServerSourceConfigMapper sourceConfigMapper) {
        this.dataProviderManager = dataProviderManager;
        this.sourceService = sourceService;
        this.sourceConfigMapper = sourceConfigMapper;
    }

    @Override
    public EngineResult execute(Plan plan) throws Exception {
        DataProviderSource source = source(plan.source());
        Dataframe result = dataProviderManager.execute(source, script(plan), execution(plan, source));
        return result(result);
    }

    private DataProviderSource source(SourceReference reference) {
        Source source = sourceService.retrieve(reference.id(), false);
        if (!reference.organizationId().equals(source.getOrgId())) {
            throw new IllegalStateException("数据源组织与已授权快照不一致");
        }
        return sourceConfigMapper.providerSource(source);
    }

    private QueryScript script(Plan plan) {
        return QueryScript.builder()
                .test(plan.script().preview())
                .sourceId(plan.script().sourceId())
                .viewId(plan.script().viewId())
                .script(plan.script().text())
                .scriptType(yubi.core.data.provider.ScriptType.valueOf(plan.script().type().name()))
                .variables(plan.script().variables().stream().map(this::variable).toList())
                .schema(plan.script().schema().entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> column(entry.getValue()), (left, right) -> left, LinkedHashMap::new)))
                .build();
    }

    private ExecuteParam execution(Plan plan, DataProviderSource source) {
        return ExecuteParam.builder()
                .keywords(plan.execution().keywords().stream().map(SelectKeyword::valueOf).toList())
                .columns(plan.execution().columns().stream().map(this::selectColumn).toList())
                .functionColumns(plan.execution().functionColumns().stream().map(value -> {
                    FunctionColumn column = new FunctionColumn();
                    column.setAlias(value.alias());
                    column.setSnippet(value.snippet());
                    return column;
                }).toList())
                .aggregators(plan.execution().aggregators().stream().map(this::aggregate).toList())
                .filters(plan.execution().filters().stream().map(this::filter).toList())
                .groups(plan.execution().groups().stream().map(value -> {
                    GroupByOperator group = new GroupByOperator();
                    group.setAlias(value.alias());
                    group.setColumn(value.column().toArray(String[]::new));
                    return group;
                }).toList())
                .orders(plan.execution().orders().stream().map(this::order).toList())
                .includeColumns(plan.execution().allowedColumns().stream().map(this::selectColumn).collect(Collectors.toSet()))
                .pageInfo(page(plan.execution().page()))
                .serverAggregate(Boolean.TRUE.equals(source.getProperties().get("serverAggregate")))
                .concurrencyOptimize(plan.execution().concurrencyOptimize())
                .cacheEnable(plan.execution().cacheEnabled())
                .cacheExpires(plan.execution().cacheExpires())
                .build();
    }

    private SelectColumn selectColumn(ColumnSelection value) {
        return SelectColumn.of(value.alias(), value.path().toArray(String[]::new));
    }

    private AggregateOperator aggregate(Aggregate value) {
        AggregateOperator operator = new AggregateOperator();
        operator.setSqlOperator(AggregateOperator.SqlOperator.valueOf(value.operator().name()));
        operator.setAlias(value.alias());
        operator.setColumn(value.column().toArray(String[]::new));
        return operator;
    }

    private FilterOperator filter(Filter value) {
        FilterOperator operator = new FilterOperator();
        operator.setAggOperator(value.aggregateOperator() == null ? null
                : AggregateOperator.SqlOperator.valueOf(value.aggregateOperator().name()));
        operator.setSqlOperator(FilterOperator.SqlOperator.valueOf(value.operator().name()));
        operator.setColumn(value.column().toArray(String[]::new));
        operator.setValues(value.values().stream().map(this::typedValue).toArray(SingleTypedValue[]::new));
        return operator;
    }

    private OrderOperator order(Order value) {
        OrderOperator operator = new OrderOperator();
        operator.setAggOperator(value.aggregateOperator() == null ? null
                : AggregateOperator.SqlOperator.valueOf(value.aggregateOperator().name()));
        operator.setOperator(OrderOperator.SqlOperator.valueOf(value.operator().name()));
        operator.setColumn(value.column().toArray(String[]::new));
        return operator;
    }

    private SingleTypedValue typedValue(TypedValue value) {
        SingleTypedValue typed = new SingleTypedValue();
        typed.setValue(value.value());
        typed.setValueType(yubi.core.base.consts.ValueType.valueOf(value.type().name()));
        typed.setFormat(value.format());
        return typed;
    }

    private ScriptVariable variable(Variable value) {
        ScriptVariable variable = new ScriptVariable(value.name(),
                yubi.core.base.consts.VariableTypeEnum.valueOf(value.type().name()),
                yubi.core.base.consts.ValueType.valueOf(value.valueType().name()), value.values(), value.expression());
        variable.setDisabled(value.disabled());
        variable.setFormat(value.format());
        return variable;
    }

    private PageInfo page(Page value) {
        return PageInfo.builder().pageNo(value.pageNo()).pageSize(value.pageSize()).total(value.total())
                .countTotal(value.countTotal()).build();
    }

    private EngineResult result(Dataframe value) {
        Page page = value.getPageInfo() == null ? null : new Page(value.getPageInfo().getPageNo(),
                value.getPageInfo().getPageSize(), value.getPageInfo().getTotal(), value.getPageInfo().isCountTotal());
        return new EngineResult(value.getId(), value.getName(), value.getVizType(), value.getVizId(),
                value.getColumns() == null ? List.of() : value.getColumns().stream().map(this::metadata).toList(),
                value.getRows(), page, value.getScript());
    }

    private ColumnMetadata metadata(Column value) {
        List<ForeignKey> keys = value.getForeignKeys() == null ? List.of() : value.getForeignKeys().stream()
                .map(key -> new ForeignKey(key.getDatabase(), key.getTable(), key.getColumn())).toList();
        return new ColumnMetadata(List.of(value.getName()),
                yubi.query.domain.QueryModels.ValueType.valueOf(value.getType().name()), value.getFmt(), keys);
    }

    private Column column(ColumnMetadata value) {
        Column column = Column.of(yubi.core.base.consts.ValueType.valueOf(value.type().name()),
                value.name().toArray(String[]::new));
        column.setFmt(value.format());
        List<yubi.core.data.provider.ForeignKey> keys = value.foreignKeys().stream().map(key -> {
            yubi.core.data.provider.ForeignKey foreignKey = new yubi.core.data.provider.ForeignKey();
            foreignKey.setDatabase(key.database());
            foreignKey.setTable(key.table());
            foreignKey.setColumn(key.column());
            return foreignKey;
        }).toList();
        column.setForeignKeys(keys);
        return column;
    }
}

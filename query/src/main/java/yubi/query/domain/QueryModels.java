package yubi.query.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Query 边界内部使用的纯 Java 值对象。 */
public final class QueryModels {

    private QueryModels() {
    }

    public enum Channel { AUTHENTICATED, SHARED, SYSTEM }

    public enum ScriptType { SQL, STRUCT }

    public enum ValueType { STRING, NUMERIC, DATE, BOOLEAN, IDENTIFIER, FRAGMENT, SNIPPET, KEYWORD }

    public enum VariableType { QUERY, PERMISSION }

    public enum AggregateType { MIN, AVG, MAX, SUM, COUNT, COUNT_DISTINCT }

    public enum FilterType {
        EQ, NE, GT, LT, GTE, LTE, IN, NOT_IN, LIKE, PREFIX_LIKE, SUFFIX_LIKE,
        NOT_LIKE, PREFIX_NOT_LIKE, SUFFIX_NOT_LIKE, IS_NULL, NOT_NULL, BETWEEN, NOT_BETWEEN
    }

    public enum OrderType { ASC, DESC }

    public enum FailureCategory { VALIDATION, ACCESS_DENIED, DEFINITION, EXECUTION }

    public record Page(long pageNo, long pageSize, long total, boolean countTotal) {
        public static Page request(long pageNo, long pageSize, boolean countTotal) {
            return new Page(pageNo, pageSize, 0, countTotal);
        }
    }

    public record ColumnSelection(String alias, List<String> path) {
        public ColumnSelection {
            path = immutableList(path);
        }
    }

    public record FunctionColumn(String alias, String snippet) {
    }

    public record Aggregate(AggregateType operator, String alias, List<String> column) {
        public Aggregate {
            column = immutableList(column);
        }
    }

    public record TypedValue(Object value, ValueType type, String format) {
    }

    public record Filter(AggregateType aggregateOperator,
                         FilterType operator,
                         List<String> column,
                         List<TypedValue> values) {
        public Filter {
            column = immutableList(column);
            values = immutableList(values);
        }
    }

    public record Group(String alias, List<String> column) {
        public Group {
            column = immutableList(column);
        }
    }

    public record Order(AggregateType aggregateOperator, OrderType operator, List<String> column) {
        public Order {
            column = immutableList(column);
        }
    }

    public record Variable(String name,
                           VariableType type,
                           ValueType valueType,
                           Set<String> values,
                           boolean expression,
                           boolean disabled,
                           String format) {
        public Variable {
            values = values == null ? Set.of() : Set.copyOf(values);
        }

        public Variable withValues(Set<String> newValues) {
            return new Variable(name, type, valueType, newValues, expression, disabled, format);
        }

        public Variable asFragment() {
            return new Variable(name, type, ValueType.FRAGMENT, values, expression, disabled, format);
        }

        public Variable disable() {
            return new Variable(name, type, valueType, values, expression, true, format);
        }
    }

    public record ForeignKey(String database, String table, String column) {
    }

    public record ColumnMetadata(List<String> name,
                                 ValueType type,
                                 String format,
                                 List<ForeignKey> foreignKeys) {
        public ColumnMetadata {
            name = immutableList(name);
            foreignKeys = immutableList(foreignKeys);
        }

        public String key() {
            return String.join(".", name);
        }
    }

    public record SourceDefinition(String id,
                                   String organizationId,
                                   String name,
                                   String parentId) {
    }

    public record SourceReference(String id, String organizationId) {
    }

    public record Definition(String viewId,
                             String organizationId,
                             String viewName,
                             String parentId,
                             String sourceId,
                             String script,
                             ScriptType scriptType,
                             Map<String, ColumnMetadata> schema) {
        public Definition {
            schema = immutableMap(schema);
        }
    }

    public record AccessDecision(boolean organizationOwner,
                                 Set<ColumnSelection> allowedColumns,
                                 boolean scriptVisible) {
        public AccessDecision {
            allowedColumns = allowedColumns == null ? Set.of() : Set.copyOf(allowedColumns);
        }
    }

    public record Script(boolean preview,
                         String sourceId,
                         String viewId,
                         String text,
                         ScriptType type,
                         List<Variable> variables,
                         Map<String, ColumnMetadata> schema) {
        public Script {
            variables = immutableList(variables);
            schema = immutableMap(schema);
        }
    }

    public record Execution(List<String> keywords,
                            List<ColumnSelection> columns,
                            List<FunctionColumn> functionColumns,
                            List<Aggregate> aggregators,
                            List<Filter> filters,
                            List<Group> groups,
                            List<Order> orders,
                            Set<ColumnSelection> allowedColumns,
                            Page page,
                            boolean concurrencyOptimize,
                            boolean cacheEnabled,
                            int cacheExpires) {
        public Execution {
            keywords = immutableList(keywords);
            columns = immutableList(columns);
            functionColumns = immutableList(functionColumns);
            aggregators = immutableList(aggregators);
            filters = immutableList(filters);
            groups = immutableList(groups);
            orders = immutableList(orders);
            allowedColumns = allowedColumns == null ? Set.of() : Set.copyOf(allowedColumns);
        }
    }

    public record Plan(SourceReference source, Script script, Execution execution) {
    }

    /** 受信任的引擎端口传输对象；公开边界由 QueryResult 统一冻结结果集合。 */
    public record EngineResult(String id,
                               String name,
                               String visualizationType,
                               String visualizationId,
                               List<ColumnMetadata> columns,
                               List<List<Object>> rows,
                               Page page,
                               String script) {
    }

    public record AuditEvent(Channel channel,
                             String subjectId,
                             String organizationId,
                             String correlationId,
                             String resourceId,
                             long durationMillis,
                             int rowCount,
                             boolean success,
                             FailureCategory failureCategory) {
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}

package yubi.agent.application;

import yubi.agent.api.ToolInputException;
import yubi.query.api.DataAssetDetail;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.domain.QueryModels.Filter;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

final class ExecuteViewAuthorizationGuard {

    private static final Set<ValueType> SAFE_PARAMETER_TYPES = Set.of(
            ValueType.STRING, ValueType.NUMERIC, ValueType.DATE, ValueType.BOOLEAN);
    private final QueryMetadataUseCase metadataUseCase;

    ExecuteViewAuthorizationGuard(QueryMetadataUseCase metadataUseCase) {
        this.metadataUseCase = metadataUseCase;
    }

    ExecuteViewInput authorize(ExecuteViewInput input, QueryExecutionContext context) {
        DataAssetDetail detail = metadataUseCase.describe(
                new DescribeDataAssetRequest(input.viewId(), false), context);
        if (detail == null || !input.viewId().equals(detail.id())) {
            throw invalid("数据资产详情与 execute_view 不一致");
        }
        Map<List<String>, DataAssetDetail.FieldDescription> allowedFields = detail.fields().stream()
                .filter(field -> field.path() != null && !field.path().isEmpty())
                .collect(Collectors.toUnmodifiableMap(
                        field -> List.copyOf(field.path()), Function.identity(), (left, right) -> left));
        input.columns().forEach(value -> requireAllowed(value.path(), allowedFields));
        input.aggregators().forEach(value -> requireAllowed(value.column(), allowedFields));
        input.filters().forEach(value -> requireAllowedFilter(value, allowedFields));
        input.groups().forEach(value -> requireAllowed(value.column(), allowedFields));
        input.orders().forEach(value -> requireAllowed(value.column(), allowedFields));

        Map<String, DataAssetDetail.VariableDescription> variables = detail.variables().stream()
                .collect(Collectors.toUnmodifiableMap(DataAssetDetail.VariableDescription::name,
                        Function.identity(), (left, right) -> left));
        Map<String, Set<String>> normalizedParameters = new LinkedHashMap<>();
        input.parameters().forEach((name, values) -> {
            DataAssetDetail.VariableDescription variable = requireSafeVariable(variables.get(name));
            if (values == null || values.isEmpty()) {
                throw invalid("Query 变量参数值不能为空");
            }
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            values.forEach(value -> normalized.add(SafeScalarValues.normalize(value, variable.valueType())));
            normalizedParameters.put(name, Collections.unmodifiableSet(normalized));
        });
        variables.values().stream()
                .filter(DataAssetDetail.VariableDescription::required)
                .filter(value -> value.type() == VariableType.QUERY)
                .filter(value -> value.expression() || !SAFE_PARAMETER_TYPES.contains(value.valueType()))
                .findFirst()
                .ifPresent(variable -> {
                    throw new ToolInputException("UNSUPPORTED_VIEW_VARIABLE",
                            "该 View 包含 execute_view V1 无法安全满足的必填 Query 变量");
                });
        variables.values().stream()
                .filter(DataAssetDetail.VariableDescription::required)
                .filter(value -> value.type() == VariableType.QUERY)
                .filter(value -> !value.expression() && SAFE_PARAMETER_TYPES.contains(value.valueType()))
                .map(DataAssetDetail.VariableDescription::name)
                .filter(name -> !normalizedParameters.containsKey(name)
                        || normalizedParameters.get(name).isEmpty())
                .findFirst()
                .ifPresent(name -> {
                    throw invalid("缺少必填 Query 变量参数");
                });
        return input.withParameters(normalizedParameters);
    }

    private DataAssetDetail.FieldDescription requireAllowed(
            List<String> path,
            Map<List<String>, DataAssetDetail.FieldDescription> allowedFields) {
        DataAssetDetail.FieldDescription field = allowedFields.get(path);
        if (field == null) {
            throw new ToolInputException("UNAUTHORIZED_FIELD", "execute_view 包含未授权字段");
        }
        return field;
    }

    private void requireAllowedFilter(
            Filter filter,
            Map<List<String>, DataAssetDetail.FieldDescription> allowedFields) {
        DataAssetDetail.FieldDescription field = requireAllowed(filter.column(), allowedFields);
        if (filter.values().stream().anyMatch(value -> value.type() != field.valueType())) {
            throw invalid("过滤值类型必须与授权字段类型一致");
        }
    }

    private DataAssetDetail.VariableDescription requireSafeVariable(
            DataAssetDetail.VariableDescription variable) {
        if (variable == null || variable.type() != VariableType.QUERY || variable.expression()
                || !SAFE_PARAMETER_TYPES.contains(variable.valueType())) {
            throw new ToolInputException("UNAUTHORIZED_PARAMETER", "execute_view 变量参数不允许使用");
        }
        return variable;
    }

    private ToolInputException invalid(String message) {
        return new ToolInputException("INVALID_TOOL_INPUT", message);
    }
}

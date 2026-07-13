package yubi.query.application;

import yubi.query.api.DataAssetDetail;
import yubi.query.api.DataAssetDetail.FieldDescription;
import yubi.query.api.DataAssetDetail.FunctionDescription;
import yubi.query.api.DataAssetDetail.VariableDescription;
import yubi.query.api.DataAssetSummary;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryMetadataUseCase;
import yubi.query.api.QueryValidationException;
import yubi.query.api.SearchDataAssetsRequest;
import yubi.query.api.SearchDataAssetsResult;
import yubi.query.domain.QueryMetadataModels.AssetReference;
import yubi.query.domain.QueryMetadataModels.FunctionDescriptor;
import yubi.query.domain.QueryMetadataModels.MetadataAccessDecision;
import yubi.query.domain.QueryMetadataModels.MetadataDefinition;
import yubi.query.domain.QueryMetadataModels.VariableDescriptor;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.port.QueryMetadataAccessPolicyPort;
import yubi.query.port.QueryMetadataCatalogPort;
import yubi.query.port.QueryMetadataDefinitionPort;
import yubi.query.port.QueryMetadataFunctionPort;
import yubi.query.port.QueryMetadataVariablePort;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class DefaultQueryMetadataService implements QueryMetadataUseCase {

    private static final int MAX_SEARCH_LIMIT = 100;
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final Comparator<String> TEXT_ORDER =
            Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));

    private final QueryMetadataCatalogPort catalogPort;
    private final QueryMetadataAccessPolicyPort accessPolicyPort;
    private final QueryMetadataDefinitionPort definitionPort;
    private final QueryMetadataVariablePort variablePort;
    private final QueryMetadataFunctionPort functionPort;

    public DefaultQueryMetadataService(QueryMetadataCatalogPort catalogPort,
                                       QueryMetadataAccessPolicyPort accessPolicyPort,
                                       QueryMetadataDefinitionPort definitionPort,
                                       QueryMetadataVariablePort variablePort,
                                       QueryMetadataFunctionPort functionPort) {
        this.catalogPort = catalogPort;
        this.accessPolicyPort = accessPolicyPort;
        this.definitionPort = definitionPort;
        this.variablePort = variablePort;
        this.functionPort = functionPort;
    }

    @Override
    public SearchDataAssetsResult search(SearchDataAssetsRequest request, QueryExecutionContext context) {
        validateContext(context);
        if (request == null) {
            throw new QueryValidationException("资产搜索请求不能为空");
        }
        String query = requireText(request.query(), "搜索词不能为空");
        if (query.length() > 200) {
            throw new QueryValidationException("搜索词长度不能超过 200");
        }
        if (request.limit() != null && (request.limit() < 1 || request.limit() > MAX_SEARCH_LIMIT)) {
            throw new QueryValidationException("搜索数量必须在 1 到 100 之间");
        }
        int limit = request.limit() == null ? DEFAULT_SEARCH_LIMIT : request.limit();
        authorizeContext(context);
        List<AssetReference> candidates = definition(
                () -> catalogPort.listReadable(context), "数据资产目录不可用");
        if (candidates == null) {
            throw new QueryDefinitionException("数据资产目录不可用");
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        Map<String, AssetReference> unique = new LinkedHashMap<>();
        candidates.stream()
                .filter(asset -> validSearchAsset(asset, context.organizationId()))
                .filter(asset -> matches(asset, normalizedQuery))
                .sorted(Comparator.comparing(AssetReference::name, TEXT_ORDER)
                        .thenComparing(AssetReference::id, TEXT_ORDER))
                .forEach(asset -> unique.putIfAbsent(asset.id(), asset));

        List<DataAssetSummary> assets = unique.values().stream()
                .limit(limit)
                .map(asset -> new DataAssetSummary(asset.id(), asset.name(), asset.description()))
                .toList();
        return new SearchDataAssetsResult(assets);
    }

    @Override
    public DataAssetDetail describe(DescribeDataAssetRequest request, QueryExecutionContext context) {
        validateContext(context);
        if (request == null) {
            throw new QueryValidationException("资产详情请求不能为空");
        }
        String assetId = requireText(request.assetId(), "数据资产标识不能为空");
        authorizeContext(context);

        AssetReference asset = definition(() -> catalogPort.find(assetId), "数据资产不存在或不可用");
        if (asset == null) {
            throw new QueryDefinitionException("数据资产不存在或不可用");
        }
        if (!assetId.equals(asset.id()) || blank(asset.organizationId()) || blank(asset.name())
                || blank(asset.sourceId())) {
            throw new QueryDefinitionException("数据资产不存在或不可用");
        }
        if (!context.organizationId().equals(asset.organizationId())) {
            throw new QueryAccessDeniedException("无权访问该数据资产");
        }
        MetadataAccessDecision access = authorize(() -> accessPolicyPort.authorize(asset, context));
        if (access == null) {
            throw new QueryAccessDeniedException("无权访问该数据资产");
        }

        MetadataDefinition definition = definition(
                () -> definitionPort.load(assetId, request.includeScript(), access.scriptVisible()),
                "数据资产详情不可用");
        validateDefinition(asset, definition);
        List<VariableDescriptor> variables = definition(
                () -> variablePort.load(asset.organizationId(), asset.id(), context), "数据资产变量不可用");
        List<FunctionDescriptor> functions = definition(
                () -> functionPort.load(asset.sourceId()), "数据资产函数能力不可用");
        if (variables == null || functions == null) {
            throw new QueryDefinitionException("数据资产详情不可用");
        }

        Optional<String> script = request.includeScript() && access.scriptVisible()
                && definition.script() != null && !definition.script().isBlank()
                ? Optional.of(definition.script()) : Optional.empty();
        return new DataAssetDetail(asset.id(), asset.name(), asset.description(),
                fields(definition.schema(), access), variables(variables), functions(functions), script);
    }

    private void validateContext(QueryExecutionContext context) {
        if (context == null || blank(context.subjectId()) || blank(context.organizationId())
                || blank(context.correlationId()) || context.channel() == null) {
            throw new QueryValidationException("可信查询上下文不完整");
        }
    }

    private void authorizeContext(QueryExecutionContext context) {
        authorize(() -> {
            accessPolicyPort.validateContext(context);
            return Boolean.TRUE;
        });
    }

    private List<FieldDescription> fields(Map<String, ColumnMetadata> schema, MetadataAccessDecision access) {
        Set<String> allowed = access.allowedColumns().stream()
                .map(ColumnSelection::path)
                .filter(this::validPath)
                .map(path -> String.join(".", path))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        boolean wildcard = access.organizationOwner() || allowed.contains("*");
        Map<String, FieldDescription> unique = new LinkedHashMap<>();
        schema.values().stream()
                .filter(column -> column != null && column.type() != null && validPath(column.name()))
                .filter(column -> wildcard || allowed.contains(column.key()))
                .sorted(Comparator.comparing(ColumnMetadata::key, TEXT_ORDER))
                .forEach(column -> unique.putIfAbsent(column.key(), new FieldDescription(
                        column.name(), column.key(), column.type(), column.format())));
        return List.copyOf(unique.values());
    }

    private List<VariableDescription> variables(List<VariableDescriptor> descriptors) {
        Comparator<VariableDescriptor> order = Comparator
                .comparing((VariableDescriptor value) -> value.name().toLowerCase(Locale.ROOT))
                .thenComparing(value -> value.scope() == DataAssetDetail.VariableScope.VIEW ? 0 : 1)
                .thenComparing(VariableDescriptor::name, TEXT_ORDER);
        Map<String, VariableDescriptor> unique = new LinkedHashMap<>();
        descriptors.stream()
                .filter(value -> value != null && !blank(value.name()) && value.type() != null
                        && value.valueType() != null && value.scope() != null)
                .sorted(order)
                .forEach(value -> unique.putIfAbsent(value.name().toLowerCase(Locale.ROOT), value));
        return unique.values().stream()
                .map(value -> new VariableDescription(value.name(), value.label(), value.type(), value.valueType(),
                        value.required(), value.expression(), value.format(), value.scope()))
                .toList();
    }

    private List<FunctionDescription> functions(List<FunctionDescriptor> descriptors) {
        Comparator<FunctionDescriptor> order = Comparator
                .comparing(FunctionDescriptor::name, TEXT_ORDER)
                .thenComparing(FunctionDescriptor::symbol, TEXT_ORDER);
        Map<String, FunctionDescriptor> unique = new LinkedHashMap<>();
        descriptors.stream()
                .filter(value -> value != null && !blank(value.name()) && !blank(value.symbol()))
                .sorted(order)
                .forEach(value -> unique.putIfAbsent(
                        (value.name() + "\u0000" + value.symbol()).toLowerCase(Locale.ROOT), value));
        return unique.values().stream()
                .map(value -> new FunctionDescription(value.name(), value.symbol()))
                .toList();
    }

    private void validateDefinition(AssetReference asset, MetadataDefinition definition) {
        if (definition == null || !asset.id().equals(definition.viewId())
                || !asset.organizationId().equals(definition.organizationId())
                || !asset.sourceId().equals(definition.sourceId()) || definition.schema() == null) {
            throw new QueryDefinitionException("数据资产详情不可用");
        }
    }

    private boolean validSearchAsset(AssetReference asset, String organizationId) {
        return asset != null && !blank(asset.id()) && !blank(asset.name()) && !blank(asset.sourceId())
                && organizationId.equals(asset.organizationId());
    }

    private boolean validPath(List<String> path) {
        return path != null && !path.isEmpty() && path.stream().allMatch(value -> !blank(value));
    }

    private boolean matches(AssetReference asset, String query) {
        return contains(asset.id(), query) || contains(asset.name(), query) || contains(asset.description(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String requireText(String value, String message) {
        if (blank(value)) {
            throw new QueryValidationException(message);
        }
        return value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private <T> T definition(Supplier<T> operation, String message) {
        try {
            return operation.get();
        } catch (RuntimeException exception) {
            throw new QueryDefinitionException(message);
        }
    }

    private <T> T authorize(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RuntimeException exception) {
            throw new QueryAccessDeniedException("无权访问数据资产元数据");
        }
    }
}

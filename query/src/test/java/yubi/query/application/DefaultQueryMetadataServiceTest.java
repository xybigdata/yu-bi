package yubi.query.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yubi.query.api.DataAssetDetail;
import yubi.query.api.DescribeDataAssetRequest;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryValidationException;
import yubi.query.api.SearchDataAssetsRequest;
import yubi.query.domain.QueryMetadataModels.AssetReference;
import yubi.query.domain.QueryMetadataModels.FunctionDescriptor;
import yubi.query.domain.QueryMetadataModels.MetadataAccessDecision;
import yubi.query.domain.QueryMetadataModels.MetadataDefinition;
import yubi.query.domain.QueryMetadataModels.VariableDescriptor;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.VariableType;
import yubi.query.port.QueryMetadataAccessPolicyPort;
import yubi.query.port.QueryMetadataCatalogPort;
import yubi.query.port.QueryMetadataDefinitionPort;
import yubi.query.port.QueryMetadataFunctionPort;
import yubi.query.port.QueryMetadataVariablePort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultQueryMetadataServiceTest {

    private final QueryExecutionContext context = new QueryExecutionContext(
            Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1");
    private final AssetReference asset = new AssetReference(
            "view-1", "org-1", "Orders", "Order facts", "folder-1", "source-1");
    private final MutableCatalog catalog = new MutableCatalog();
    private final MutableAccess access = new MutableAccess();
    private final MutableDefinition definitions = new MutableDefinition();
    private final MutableVariables variables = new MutableVariables();
    private final MutableFunctions functions = new MutableFunctions();
    private DefaultQueryMetadataService service;

    @BeforeEach
    void setUp() {
        catalog.assets = new ArrayList<>(List.of(asset));
        catalog.detail = asset;
        access.decision = new MetadataAccessDecision(true,
                Set.of(new ColumnSelection(null, List.of("*"))), true);
        Map<String, ColumnMetadata> schema = new LinkedHashMap<>();
        schema.put("orders.status", column(ValueType.STRING, "orders", "status"));
        schema.put("orders.amount", column(ValueType.NUMERIC, "orders", "amount"));
        definitions.definition = new MetadataDefinition(
                "view-1", "org-1", "source-1", "select * from orders", schema);
        variables.values = new ArrayList<>();
        functions.values = new ArrayList<>();
        service = new DefaultQueryMetadataService(catalog, access, definitions, variables, functions);
    }

    @Test
    void shouldSearchReadableAssetsWithStableSortingDeduplicationAndIsolation() {
        AssetReference alpha = new AssetReference(
                "view-2", "org-1", "alpha orders", "daily orders", null, "source-2");
        AssetReference crossOrganization = new AssetReference(
                "view-x", "org-2", "orders secret", "must not appear", null, "source-x");
        AssetReference folder = new AssetReference(
                "folder", "org-1", "orders folder", null, null, null);
        catalog.assets = new ArrayList<>(List.of(asset, crossOrganization, alpha, asset, folder));

        var result = service.search(new SearchDataAssetsRequest("orders", 10), context);

        assertEquals(List.of("view-2", "view-1"), result.assets().stream().map(value -> value.id()).toList());
        assertThrows(UnsupportedOperationException.class, () -> result.assets().clear());
        catalog.assets.clear();
        assertEquals(2, result.assets().size());
        assertFalse(result.toString().contains("select *"));
    }

    @Test
    void shouldRejectEmptyAndIllegalRequestsBeforePorts() {
        assertThrows(QueryValidationException.class, () -> service.search(null, context));
        assertThrows(QueryValidationException.class,
                () -> service.search(new SearchDataAssetsRequest(" ", 10), context));
        assertThrows(QueryValidationException.class,
                () -> service.search(new SearchDataAssetsRequest("orders", 0), context));
        assertThrows(QueryValidationException.class,
                () -> service.search(new SearchDataAssetsRequest("orders", 101), context));
        assertThrows(QueryValidationException.class, () -> service.describe(null, context));
        assertThrows(QueryValidationException.class,
                () -> service.describe(new DescribeDataAssetRequest(" ", false), context));
        assertThrows(QueryValidationException.class,
                () -> service.search(new SearchDataAssetsRequest("orders", 10),
                        new QueryExecutionContext(Channel.AUTHENTICATED, "user-1", null, "correlation-1")));
        assertEquals(0, access.contextChecks);

        assertEquals(1, service.search(new SearchDataAssetsRequest("orders", null), context).assets().size());
    }

    @Test
    void shouldDescribeOwnerFieldsVariablesFunctionsAndOptionalScriptDeterministically() {
        variables.values = new ArrayList<>(List.of(
                variable("STATUS", DataAssetDetail.VariableScope.ORGANIZATION, false),
                variable("status", DataAssetDetail.VariableScope.VIEW, true),
                variable("DATE_FROM", DataAssetDetail.VariableScope.VIEW, true)));
        functions.values = new ArrayList<>(List.of(
                new FunctionDescriptor("SUM", "SUM"),
                new FunctionDescriptor("ABS", "ABS"),
                new FunctionDescriptor("SUM", "SUM")));

        DataAssetDetail detail = service.describe(new DescribeDataAssetRequest("view-1", true), context);

        assertEquals(List.of("orders.amount", "orders.status"),
                detail.fields().stream().map(DataAssetDetail.FieldDescription::name).toList());
        assertEquals(List.of("DATE_FROM", "status"),
                detail.variables().stream().map(DataAssetDetail.VariableDescription::name).toList());
        assertEquals(DataAssetDetail.VariableScope.VIEW, detail.variables().get(1).scope());
        assertEquals(List.of("ABS", "SUM"),
                detail.functions().stream().map(DataAssetDetail.FunctionDescription::name).toList());
        assertEquals("select * from orders", detail.script().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> detail.fields().clear());
        assertThrows(UnsupportedOperationException.class, () -> detail.fields().getFirst().path().clear());
        variables.values.clear();
        functions.values.clear();
        assertEquals(2, detail.variables().size());
        assertEquals(2, detail.functions().size());
    }

    @Test
    void shouldCropNonOwnerFieldsAndNeverReturnUnauthorizedScript() {
        access.decision = new MetadataAccessDecision(false,
                Set.of(new ColumnSelection(null, List.of("orders", "status"))), false);

        DataAssetDetail detail = service.describe(new DescribeDataAssetRequest("view-1", true), context);

        assertEquals(List.of("orders.status"),
                detail.fields().stream().map(DataAssetDetail.FieldDescription::name).toList());
        assertTrue(detail.script().isEmpty());
        assertFalse(detail.toString().contains("select * from orders"));
    }

    @Test
    void shouldFailClosedBeforeLoadingSensitiveMetadataForMissingCrossOrgOrDeniedAssets() {
        catalog.detail = null;
        assertThrows(QueryDefinitionException.class,
                () -> service.describe(new DescribeDataAssetRequest("missing", true), context));
        assertEquals(0, access.authorizations);
        assertEquals(0, definitions.loads);

        catalog.detail = new AssetReference("view-x", "org-2", "secret", null, null, "source-x");
        assertThrows(QueryAccessDeniedException.class,
                () -> service.describe(new DescribeDataAssetRequest("view-x", true), context));
        assertEquals(0, access.authorizations);
        assertEquals(0, definitions.loads);

        catalog.detail = asset;
        access.failure = new SecurityException("READ denied");
        assertThrows(QueryAccessDeniedException.class,
                () -> service.describe(new DescribeDataAssetRequest("view-1", true), context));
        assertEquals(0, definitions.loads);
        assertEquals(0, variables.loads);
        assertEquals(0, functions.loads);
    }

    @Test
    void shouldClassifyAndSanitizeEveryPortFailure() {
        catalog.failure = new IllegalStateException("jdbc:secret password=hunter2");
        QueryDefinitionException catalogFailure = assertThrows(QueryDefinitionException.class,
                () -> service.search(new SearchDataAssetsRequest("orders", 10), context));
        assertNull(catalogFailure.getCause());
        assertFalse(catalogFailure.toString().contains("hunter2"));

        catalog.failure = null;
        definitions.failure = new IllegalStateException("config={password:hunter2}");
        QueryDefinitionException definitionFailure = assertThrows(QueryDefinitionException.class,
                () -> service.describe(new DescribeDataAssetRequest("view-1", true), context));
        assertNull(definitionFailure.getCause());

        definitions.failure = null;
        variables.failure = new IllegalStateException("permission value secret-row");
        assertThrows(QueryDefinitionException.class,
                () -> service.describe(new DescribeDataAssetRequest("view-1", true), context));

        variables.failure = null;
        functions.failure = new IllegalStateException("source config secret-source");
        QueryDefinitionException functionFailure = assertThrows(QueryDefinitionException.class,
                () -> service.describe(new DescribeDataAssetRequest("view-1", true), context));
        assertFalse(functionFailure.toString().contains("secret-source"));
    }

    @Test
    void shouldRebuildPublicQueryExceptionsFromEveryMetadataPortWithoutSensitiveDetails() {
        RuntimeException secretCause = new IllegalStateException("jdbc:mysql://secret password=hunter2");
        catalog.failure = new QueryDefinitionException("source config secret-catalog", secretCause);
        assertSanitizedDefinition("数据资产目录不可用",
                () -> service.search(new SearchDataAssetsRequest("orders", 10), context));

        catalog.failure = null;
        access.authorizationFailure = new QueryAccessDeniedException(
                "permission adapter secret-access", secretCause);
        assertSanitizedAccess(() -> service.describe(new DescribeDataAssetRequest("view-1", true), context));

        access.authorizationFailure = null;
        definitions.failure = new QueryDefinitionException("decrypted config secret-definition", secretCause);
        assertSanitizedDefinition("数据资产详情不可用",
                () -> service.describe(new DescribeDataAssetRequest("view-1", true), context));

        definitions.failure = null;
        variables.failure = new QueryDefinitionException("variable value secret-variable", secretCause);
        assertSanitizedDefinition("数据资产变量不可用",
                () -> service.describe(new DescribeDataAssetRequest("view-1", true), context));

        variables.failure = null;
        functions.failure = new QueryDefinitionException("source password secret-function", secretCause);
        assertSanitizedDefinition("数据资产函数能力不可用",
                () -> service.describe(new DescribeDataAssetRequest("view-1", true), context));
    }

    private void assertSanitizedDefinition(String message, org.junit.jupiter.api.function.Executable operation) {
        QueryDefinitionException failure = assertThrows(QueryDefinitionException.class, operation);
        assertEquals(message, failure.getMessage());
        assertNull(failure.getCause());
        assertFalse(failure.toString().contains("secret"));
        assertFalse(failure.toString().contains("hunter2"));
        assertFalse(failure.toString().contains("jdbc:"));
    }

    private void assertSanitizedAccess(org.junit.jupiter.api.function.Executable operation) {
        QueryAccessDeniedException failure = assertThrows(QueryAccessDeniedException.class, operation);
        assertEquals("无权访问数据资产元数据", failure.getMessage());
        assertNull(failure.getCause());
        assertFalse(failure.toString().contains("secret"));
        assertFalse(failure.toString().contains("hunter2"));
        assertFalse(failure.toString().contains("jdbc:"));
    }

    private ColumnMetadata column(ValueType type, String... path) {
        return new ColumnMetadata(List.of(path), type, null, List.of());
    }

    private VariableDescriptor variable(String name, DataAssetDetail.VariableScope scope, boolean required) {
        return new VariableDescriptor(name, null, VariableType.QUERY, ValueType.STRING,
                required, false, null, scope);
    }

    private static final class MutableCatalog implements QueryMetadataCatalogPort {
        private List<AssetReference> assets;
        private AssetReference detail;
        private RuntimeException failure;

        @Override
        public List<AssetReference> listReadable(QueryExecutionContext context) {
            if (failure != null) throw failure;
            return assets;
        }

        @Override
        public AssetReference find(String assetId) {
            if (failure != null) throw failure;
            return detail;
        }
    }

    private static final class MutableAccess implements QueryMetadataAccessPolicyPort {
        private MetadataAccessDecision decision;
        private RuntimeException failure;
        private RuntimeException authorizationFailure;
        private int contextChecks;
        private int authorizations;

        @Override
        public void validateContext(QueryExecutionContext context) {
            contextChecks++;
            if (failure != null) throw failure;
        }

        @Override
        public MetadataAccessDecision authorize(AssetReference asset, QueryExecutionContext context) {
            authorizations++;
            if (authorizationFailure != null) throw authorizationFailure;
            return decision;
        }
    }

    private static final class MutableDefinition implements QueryMetadataDefinitionPort {
        private MetadataDefinition definition;
        private RuntimeException failure;
        private int loads;

        @Override
        public MetadataDefinition load(String assetId, boolean includeScript, boolean scriptVisible) {
            loads++;
            if (failure != null) throw failure;
            return new MetadataDefinition(definition.viewId(), definition.organizationId(), definition.sourceId(),
                    includeScript && scriptVisible ? definition.script() : null, definition.schema());
        }
    }

    private static final class MutableVariables implements QueryMetadataVariablePort {
        private List<VariableDescriptor> values;
        private RuntimeException failure;
        private int loads;

        @Override
        public List<VariableDescriptor> load(String organizationId,
                                             String assetId,
                                             QueryExecutionContext context) {
            loads++;
            if (failure != null) throw failure;
            return values;
        }
    }

    private static final class MutableFunctions implements QueryMetadataFunctionPort {
        private List<FunctionDescriptor> values;
        private RuntimeException failure;
        private int loads;

        @Override
        public List<FunctionDescriptor> load(String sourceId) {
            loads++;
            if (failure != null) throw failure;
            return values;
        }
    }
}

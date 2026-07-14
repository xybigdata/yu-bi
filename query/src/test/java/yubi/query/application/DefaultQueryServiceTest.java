package yubi.query.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import yubi.query.api.ExecuteQueryCommand;
import yubi.query.api.PreviewQueryCommand;
import yubi.query.api.QueryExecutionContext;
import yubi.query.api.QueryExecutionException;
import yubi.query.api.QueryAccessDeniedException;
import yubi.query.api.QueryDefinitionException;
import yubi.query.api.QueryResult;
import yubi.query.domain.QueryModels.AccessDecision;
import yubi.query.domain.QueryModels.AuditEvent;
import yubi.query.domain.QueryModels.Channel;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.ColumnSelection;
import yubi.query.domain.QueryModels.Definition;
import yubi.query.domain.QueryModels.EngineResult;
import yubi.query.domain.QueryModels.Page;
import yubi.query.domain.QueryModels.Plan;
import yubi.query.domain.QueryModels.ScriptType;
import yubi.query.domain.QueryModels.SourceDefinition;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.domain.QueryModels.Variable;
import yubi.query.domain.QueryModels.VariableType;
import yubi.query.port.QueryAccessPolicyPort;
import yubi.query.port.QueryAuditPort;
import yubi.query.port.QueryDefinitionPort;
import yubi.query.port.QueryEnginePort;
import yubi.query.port.QueryVariablePort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultQueryServiceTest {

    private SourceDefinition source;
    private Definition definition;
    private QueryExecutionContext context;
    private CapturingEngine engine;
    private List<AuditEvent> auditEvents;
    private boolean owner;
    private boolean scriptVisible;
    private DefaultQueryService service;

    @BeforeEach
    void setUp() {
        source = new SourceDefinition("source-1", "org-1", "orders", null);
        ColumnMetadata amount = new ColumnMetadata(List.of("orders", "amount"), ValueType.NUMERIC, null, null);
        definition = new Definition("view-1", "org-1", "orders-view", null, "source-1",
                "select amount from orders", ScriptType.SQL, Map.of(amount.key(), amount));
        context = new QueryExecutionContext(Channel.AUTHENTICATED, "user-1", "org-1", "correlation-1");
        engine = new CapturingEngine();
        auditEvents = new ArrayList<>();
        owner = false;
        scriptVisible = true;

        QueryDefinitionPort definitions = new QueryDefinitionPort() {
            @Override
            public Definition load(String viewId) {
                return definition;
            }

            @Override
            public SourceDefinition loadSource(String sourceId) {
                return source;
            }
        };
        QueryAccessPolicyPort access = new QueryAccessPolicyPort() {
            @Override
            public AccessDecision authorize(Definition ignored, QueryExecutionContext ignoredContext) {
                return new AccessDecision(owner,
                        owner ? Set.of(column("*")) : Set.of(column("orders", "amount")), scriptVisible);
            }

            @Override
            public boolean authorizePreview(SourceDefinition ignored, QueryExecutionContext ignoredContext) {
                return owner;
            }
        };
        QueryVariablePort variables = new QueryVariablePort() {
            @Override
            public List<Variable> loadForView(String viewId, String organizationId, QueryExecutionContext ignored) {
                return List.of(permission("region", Set.of("east")), query("status", Set.of("default"), true));
            }

            @Override
            public List<Variable> loadForSource(String organizationId, QueryExecutionContext ignored) {
                return List.of(permission("region", Set.of("east")));
            }
        };
        service = new DefaultQueryService(definitions, access, variables, engine, auditEvents::add);
    }

    @Test
    void shouldOrchestrateVariablesPageProviderAndVisibleScript() {
        QueryResult result = service.execute(command(new Page(0, (long) Integer.MAX_VALUE + 10, 0, true), true), context);

        Plan plan = engine.plan;
        assertEquals(1, plan.execution().page().pageNo());
        assertEquals(Integer.MAX_VALUE, plan.execution().page().pageSize());
        assertTrue(plan.execution().page().countTotal());
        assertEquals(Set.of("orders.amount"), plan.execution().allowedColumns().stream()
                .map(value -> String.join(".", value.path())).collect(java.util.stream.Collectors.toSet()));
        Map<String, Variable> variables = plan.script().variables().stream()
                .collect(java.util.stream.Collectors.toMap(Variable::name, value -> value));
        assertEquals(Set.of("paid"), variables.get("status").values());
        assertEquals(ValueType.STRING, variables.get("status").valueType());
        assertFalse(variables.get("region").disabled());
        assertEquals("provider script", result.script());
        assertEquals(1, auditEvents.size());
        assertTrue(auditEvents.getFirst().success());
        assertEquals(1, auditEvents.getFirst().rowCount());
    }

    @Test
    void shouldDisablePermissionVariablesAndUseWildcardForOwner() {
        owner = true;

        service.execute(command(null, false), context);

        assertTrue(engine.plan.script().variables().stream()
                .filter(value -> value.type() == VariableType.PERMISSION)
                .allMatch(Variable::disabled));
        assertEquals(List.of("*"), engine.plan.execution().allowedColumns().iterator().next().path());
    }

    @Test
    void shouldHideScriptWhenNotRequestedOrNotAuthorized() {
        assertNull(service.execute(command(null, false), context).script());
        scriptVisible = false;
        assertNull(service.execute(command(null, true), context).script());
    }

    @Test
    void shouldAuthorizeEmptyQueryBeforeReturningWithoutVariablesOrEngine() {
        AtomicInteger definitions = new AtomicInteger();
        AtomicInteger authorizations = new AtomicInteger();
        AtomicInteger variables = new AtomicInteger();
        AtomicInteger engineCalls = new AtomicInteger();
        QueryDefinitionPort definitionPort = new QueryDefinitionPort() {
            public Definition load(String ignored) { definitions.incrementAndGet(); return definition; }
            public SourceDefinition loadSource(String ignored) { return source; }
        };
        QueryAccessPolicyPort deny = new QueryAccessPolicyPort() {
            public AccessDecision authorize(Definition ignored, QueryExecutionContext ignoredContext) {
                authorizations.incrementAndGet();
                throw new SecurityException("read denied");
            }
            public boolean authorizePreview(SourceDefinition ignored, QueryExecutionContext ignoredContext) { return false; }
        };
        QueryVariablePort variablePort = new QueryVariablePort() {
            public List<Variable> loadForView(String viewId, String org, QueryExecutionContext ignored) {
                variables.incrementAndGet();
                return List.of();
            }
            public List<Variable> loadForSource(String org, QueryExecutionContext ignored) { return List.of(); }
        };
        DefaultQueryService denied = new DefaultQueryService(definitionPort, deny, variablePort,
                plan -> { engineCalls.incrementAndGet(); return null; }, event -> { });

        assertThrows(QueryAccessDeniedException.class, () -> denied.execute(emptyCommand(), context));
        assertEquals(1, definitions.get());
        assertEquals(1, authorizations.get());
        assertEquals(0, variables.get());
        assertEquals(0, engineCalls.get());
    }

    @Test
    void shouldBindSharedOrganizationBeforeReturningEmptyQuery() {
        AtomicInteger variables = new AtomicInteger();
        AtomicInteger engineCalls = new AtomicInteger();
        QueryVariablePort variablePort = new QueryVariablePort() {
            public List<Variable> loadForView(String viewId, String org, QueryExecutionContext ignored) {
                variables.incrementAndGet();
                return List.of();
            }
            public List<Variable> loadForSource(String org, QueryExecutionContext ignored) { return List.of(); }
        };
        DefaultQueryService shared = new DefaultQueryService(
                new QueryDefinitionPort() {
                    public Definition load(String ignored) { return definition; }
                    public SourceDefinition loadSource(String ignored) { return source; }
                },
                accessPort(),
                variablePort,
                plan -> { engineCalls.incrementAndGet(); return null; },
                event -> { }
        );
        QueryExecutionContext validShared = new QueryExecutionContext(Channel.SHARED, "visitor", "org-1", "shared");
        assertTrue(shared.execute(emptyCommand(), validShared).rows().isEmpty());
        QueryExecutionContext driftedShared = new QueryExecutionContext(Channel.SHARED, "visitor", "org-2", "shared");
        assertThrows(QueryAccessDeniedException.class, () -> shared.execute(emptyCommand(), driftedShared));
        assertEquals(0, variables.get());
        assertEquals(0, engineCalls.get());
    }

    @Test
    void shouldBuildPreviewPlanAndNormalizeExpressionVariable() {
        PreviewQueryCommand command = new PreviewQueryCommand("source-1", "select * from orders", ScriptType.SQL,
                List.of(column("orders", "amount")), List.of(
                        query("fragment", Set.of("amount > 10"), true),
                        new Variable("preview_policy", VariableType.PERMISSION, ValueType.STRING,
                                Set.of("region = 'east'"), true, false, null)), 0);

        service.preview(command, context);

        assertTrue(engine.plan.script().preview());
        assertEquals(1, engine.plan.execution().page().pageSize());
        assertFalse(engine.plan.execution().cacheEnabled());
        Variable fragment = engine.plan.script().variables().stream()
                .filter(value -> value.name().equals("fragment")).findFirst().orElseThrow();
        assertEquals(ValueType.FRAGMENT, fragment.valueType());
        Variable previewPolicy = engine.plan.script().variables().stream()
                .filter(value -> value.name().equals("preview_policy")).findFirst().orElseThrow();
        assertEquals(ValueType.FRAGMENT, previewPolicy.valueType());
    }

    @Test
    void shouldClassifyEngineFailureAndPreserveCause() {
        Exception providerFailure = new Exception("provider unavailable");
        engine.failure = providerFailure;

        QueryExecutionException thrown = assertThrows(QueryExecutionException.class,
                () -> service.execute(command(null, false), context));

        assertSame(providerFailure, thrown.getCause());
        assertFalse(auditEvents.getFirst().success());
        assertEquals(yubi.query.domain.QueryModels.FailureCategory.EXECUTION,
                auditEvents.getFirst().failureCategory());
    }

    @Test
    void shouldClassifyDefinitionAndAccessFailuresWithOriginalCauses() {
        IllegalStateException definitionFailure = new IllegalStateException("definition unavailable");
        QueryDefinitionPort brokenDefinitions = new QueryDefinitionPort() {
            public Definition load(String id) { throw definitionFailure; }
            public SourceDefinition loadSource(String id) { throw definitionFailure; }
        };
        DefaultQueryService definitionService = new DefaultQueryService(brokenDefinitions,
                accessPort(), emptyVariables(), engine, event -> { });
        QueryDefinitionException definitionThrown = assertThrows(QueryDefinitionException.class,
                () -> definitionService.execute(command(null, false), context));
        assertSame(definitionFailure, definitionThrown.getCause());

        SecurityException accessFailure = new SecurityException("access denied");
        QueryAccessPolicyPort brokenAccess = new QueryAccessPolicyPort() {
            public AccessDecision authorize(Definition ignored, QueryExecutionContext ignoredContext) {
                throw accessFailure;
            }
            public boolean authorizePreview(SourceDefinition ignored, QueryExecutionContext ignoredContext) {
                throw accessFailure;
            }
        };
        QueryDefinitionPort definitions = new QueryDefinitionPort() {
            public Definition load(String id) { return definition; }
            public SourceDefinition loadSource(String id) { return source; }
        };
        DefaultQueryService accessService = new DefaultQueryService(definitions,
                brokenAccess, emptyVariables(), engine, event -> { });
        QueryAccessDeniedException accessThrown = assertThrows(QueryAccessDeniedException.class,
                () -> accessService.execute(command(null, false), context));
        assertSame(accessFailure, accessThrown.getCause());
    }

    @Test
    void shouldClassifyNullPortResultsAndAuditThemAsFailures() {
        QueryDefinitionPort definitions = new QueryDefinitionPort() {
            public Definition load(String id) { return definition; }
            public SourceDefinition loadSource(String id) { return source; }
        };
        List<AuditEvent> nullAccessAudits = new ArrayList<>();
        QueryAccessPolicyPort nullAccess = new QueryAccessPolicyPort() {
            public AccessDecision authorize(Definition ignored, QueryExecutionContext ignoredContext) { return null; }
            public boolean authorizePreview(SourceDefinition ignored, QueryExecutionContext ignoredContext) { return false; }
        };
        DefaultQueryService accessService = new DefaultQueryService(
                definitions, nullAccess, emptyVariables(), engine, nullAccessAudits::add);

        QueryAccessDeniedException accessFailure = assertThrows(QueryAccessDeniedException.class,
                () -> accessService.execute(command(null, false), context));

        assertTrue(accessFailure.getCause() instanceof IllegalStateException);
        assertFalse(nullAccessAudits.getFirst().success());
        assertEquals(yubi.query.domain.QueryModels.FailureCategory.ACCESS_DENIED,
                nullAccessAudits.getFirst().failureCategory());

        List<AuditEvent> nullEngineAudits = new ArrayList<>();
        DefaultQueryService engineService = copyWith(plan -> null, nullEngineAudits::add);
        QueryExecutionException engineFailure = assertThrows(QueryExecutionException.class,
                () -> engineService.execute(command(null, false), context));
        assertTrue(engineFailure.getCause() instanceof IllegalStateException);
        assertFalse(nullEngineAudits.getFirst().success());
    }

    @Test
    void shouldClassifyMalformedResultAndPropagateErrorWithoutMarkingSuccess() {
        List<AuditEvent> malformedAudits = new ArrayList<>();
        QueryEnginePort malformedEngine = plan -> new EngineResult(null, null, null, null,
                List.of(), List.of(), plan.execution().page(), null);
        DefaultQueryService malformedService = copyWith(malformedEngine, malformedAudits::add);

        QueryExecutionException malformedFailure = assertThrows(QueryExecutionException.class,
                () -> malformedService.execute(command(null, false), context));

        assertTrue(malformedFailure.getCause() instanceof IllegalStateException);
        assertFalse(malformedAudits.getFirst().success());

        List<AuditEvent> errorAudits = new ArrayList<>();
        AssertionError error = new AssertionError("fatal engine error");
        QueryEnginePort errorEngine = plan -> { throw error; };
        DefaultQueryService errorService = copyWith(errorEngine, errorAudits::add);

        assertSame(error, assertThrows(AssertionError.class,
                () -> errorService.execute(command(null, false), context)));
        assertFalse(errorAudits.getFirst().success());
        assertEquals(yubi.query.domain.QueryModels.FailureCategory.EXECUTION,
                errorAudits.getFirst().failureCategory());
    }

    @Test
    void shouldIgnoreAuditFailureForSuccessAndOriginalFailure() {
        QueryAuditPort brokenAudit = event -> {
            throw new IllegalStateException("audit unavailable");
        };
        DefaultQueryService withBrokenAudit = copyWith(engine, brokenAudit);
        assertEquals("result-1", withBrokenAudit.execute(command(null, false), context).id());

        Exception providerFailure = new Exception("provider unavailable");
        engine.failure = providerFailure;
        QueryExecutionException thrown = assertThrows(QueryExecutionException.class,
                () -> withBrokenAudit.execute(command(null, false), context));
        assertSame(providerFailure, thrown.getCause());
    }

    private DefaultQueryService copyWith(QueryEnginePort queryEngine, QueryAuditPort audit) {
        QueryDefinitionPort definitions = new QueryDefinitionPort() {
            public Definition load(String id) { return definition; }
            public SourceDefinition loadSource(String id) { return source; }
        };
        QueryAccessPolicyPort access = new QueryAccessPolicyPort() {
            public AccessDecision authorize(Definition ignored, QueryExecutionContext ignoredContext) {
                return new AccessDecision(false, Set.of(column("*")), true);
            }
            public boolean authorizePreview(SourceDefinition ignored, QueryExecutionContext ignoredContext) { return false; }
        };
        QueryVariablePort variables = new QueryVariablePort() {
            public List<Variable> loadForView(String id, String org, QueryExecutionContext ctx) { return List.of(); }
            public List<Variable> loadForSource(String org, QueryExecutionContext ctx) { return List.of(); }
        };
        return new DefaultQueryService(definitions, access, variables, queryEngine, audit);
    }

    private QueryAccessPolicyPort accessPort() {
        return new QueryAccessPolicyPort() {
            public AccessDecision authorize(Definition ignored, QueryExecutionContext ignoredContext) {
                return new AccessDecision(false, Set.of(column("*")), true);
            }
            public boolean authorizePreview(SourceDefinition ignored, QueryExecutionContext ignoredContext) { return false; }
        };
    }

    private QueryVariablePort emptyVariables() {
        return new QueryVariablePort() {
            public List<Variable> loadForView(String id, String org, QueryExecutionContext ctx) { return List.of(); }
            public List<Variable> loadForSource(String org, QueryExecutionContext ctx) { return List.of(); }
        };
    }

    private ExecuteQueryCommand command(Page page, boolean includeScript) {
        return new ExecuteQueryCommand("view-1", List.of(), List.of(column("orders", "amount")), List.of(),
                List.of(), List.of(), List.of(), List.of(), page, Map.of("status", Set.of("paid")),
                false, false, 0, includeScript);
    }

    private ExecuteQueryCommand emptyCommand() {
        return new ExecuteQueryCommand("view-1", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), null, Map.of(), false, false, 0, false);
    }

    private static ColumnSelection column(String... names) {
        return new ColumnSelection(null, List.of(names));
    }

    private static Variable permission(String name, Set<String> values) {
        return new Variable(name, VariableType.PERMISSION, ValueType.STRING, values, false, false, null);
    }

    private static Variable query(String name, Set<String> values, boolean expression) {
        return new Variable(name, VariableType.QUERY, ValueType.STRING, values, expression, false, null);
    }

    private static final class CapturingEngine implements QueryEnginePort {
        private Plan plan;
        private Exception failure;

        @Override
        public EngineResult execute(Plan plan) throws Exception {
            this.plan = plan;
            if (failure != null) {
                throw failure;
            }
            return new EngineResult("result-1", null, null, null, List.of(), List.of(List.of(1)),
                    plan.execution().page(), "provider script");
        }
    }
}

package yubi.agent.application;

import org.junit.jupiter.api.Test;
import yubi.agent.api.AgentExecutionContext;
import yubi.agent.api.ControlledWriteException;
import yubi.agent.api.ToolInputException;
import yubi.agent.domain.ControlledWriteModels.ControlledWriteOperation;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteAction;
import yubi.agent.domain.ControlledWriteModels.WriteApprovalScope;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEvent;
import yubi.agent.domain.ControlledWriteModels.WriteAuditEventType;
import yubi.agent.domain.ControlledWriteModels.WriteExecutionResult;
import yubi.agent.domain.ControlledWriteModels.WriteFailureCategory;
import yubi.agent.domain.ControlledWriteModels.WriteIdempotencyScope;
import yubi.agent.domain.ControlledWriteModels.WriteOperationState;
import yubi.agent.domain.ControlledWriteModels.WriteOperationView;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.ControlledWriteModels.WriteProposalCommand;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.port.VisualizationWritePort;
import yubi.agent.port.WriteOperationStorePort;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultControlledWriteUseCaseTest {

    @Test
    void proposalMustOnlyPersistPendingOperationAndSafeAudit() {
        Harness harness = harness();
        ObjectValue arguments = object(
                "name", text("  经营概览-sensitive-name  "),
                "viewId", text("view-1"),
                "parentId", text("folder-1"),
                "description", text("sensitive-description"));

        WriteOperationView result = harness.useCase.propose(
                new WriteProposalCommand("create_chart", arguments, "raw-idempotency-secret"),
                harness.context);

        assertEquals(WriteOperationState.PENDING, result.state());
        assertEquals(1, harness.business.prepareCalls.get());
        assertEquals(0, harness.business.executeCalls.get());
        assertEquals(1, harness.store.size());
        ControlledWriteOperation stored = harness.store.operation(result.approvalId());
        assertEquals("经营概览-sensitive-name",
                ((StructuredValue.TextValue) stored.canonicalParameters().values().get("name")).value());
        assertEquals(harness.context.sessionId(), stored.context().sessionId());
        assertEquals(64, stored.parametersDigest().length());
        assertEquals(64, stored.preparedDigest().length());
        assertEquals(64, stored.idempotencyKeyDigest().length());
        assertFalse(stored.toString().contains("sensitive-name"));
        assertFalse(harness.audits.toString().contains("sensitive-name"));
        assertFalse(harness.audits.toString().contains("sensitive-description"));
        assertFalse(harness.audits.toString().contains("raw-idempotency-secret"));
        assertEquals(WriteAuditEventType.PROPOSED, harness.audits.getFirst().eventType());

        assertEquals(List.of(result.approvalId()), harness.useCase.listOperations(harness.context)
                .stream().map(WriteOperationView::approvalId).toList());
        assertTrue(harness.useCase.listOperations(context("user-1", "org-1", "session-2",
                "request-2", "correlation-2")).isEmpty());
    }

    @Test
    void unknownFieldsAndHighRiskToolsMustFailBeforeBusinessCapability() {
        Harness harness = harness();
        List<String> forbiddenFields = List.of("sql", "script", "sourceId", "status", "permissions",
                "config", "userId", "organizationId", "delete", "publish", "shareToken");
        for (String forbidden : forbiddenFields) {
            ObjectValue arguments = with(createArguments("图表"), forbidden, text("forbidden-secret"));
            assertThrows(ToolInputException.class, () -> harness.useCase.propose(
                    new WriteProposalCommand("create_chart", arguments, "key-" + forbidden), harness.context));
        }
        for (String tool : List.of("delete_dashboard", "publish_chart", "share_dashboard",
                "change_permission", "execute_sql")) {
            ControlledWriteException exception = assertThrows(ControlledWriteException.class,
                    () -> harness.useCase.propose(new WriteProposalCommand(tool,
                            object(), "key-" + tool), harness.context));
            assertEquals(ControlledWriteException.Code.UNKNOWN_WRITE_TOOL, exception.code());
        }
        assertEquals(0, harness.business.prepareCalls.get());
        assertEquals(0, harness.business.executeCalls.get());
        assertEquals(0, harness.store.size());
    }

    @Test
    void equivalentRequestMustReplayAndDifferentParametersMustConflict() {
        Harness harness = harness();
        WriteOperationView first = harness.useCase.propose(new WriteProposalCommand(
                "create_chart", createArguments("  销售趋势  "), "same-key"), harness.context);
        WriteOperationView replay = harness.useCase.propose(new WriteProposalCommand(
                "create_chart", object("name", text("销售趋势"), "viewId", text("view-1"),
                "parentId", StructuredValue.NullValue.INSTANCE), " same-key "), harness.context);

        assertEquals(first.approvalId(), replay.approvalId());
        assertTrue(replay.replayed());
        assertEquals(1, harness.business.prepareCalls.get());
        assertEquals(1, harness.store.size());

        ControlledWriteException pendingOtherSession = assertThrows(ControlledWriteException.class,
                () -> harness.useCase.propose(new WriteProposalCommand(
                                "create_chart", createArguments("销售趋势"), "same-key"),
                        context("user-1", "org-1", "session-2", "request-2", "correlation-2")));
        assertEquals(ControlledWriteException.Code.APPROVAL_SCOPE_MISMATCH,
                pendingOtherSession.code());
        assertEquals(1, harness.business.prepareCalls.get());

        ControlledWriteException conflict = assertThrows(ControlledWriteException.class,
                () -> harness.useCase.propose(new WriteProposalCommand("create_chart",
                        createArguments("另一图表"), "same-key"), harness.context));
        assertEquals(ControlledWriteException.Code.IDEMPOTENCY_CONFLICT, conflict.code());
        assertEquals(1, harness.business.prepareCalls.get());

        WriteOperationView otherSubject = harness.useCase.propose(new WriteProposalCommand(
                        "create_chart", createArguments("另一图表"), "same-key"),
                context("user-2", "org-1", "session-2", "request-2", "correlation-2"));
        assertNotEquals(first.approvalId(), otherSubject.approvalId());
        assertEquals(2, harness.store.size());
    }

    @Test
    void approvalMustExecuteOnceFromStoredPreparationAndAuditCurrentRequest() {
        Harness harness = harness();
        WriteOperationView proposal = harness.useCase.propose(new WriteProposalCommand(
                "rename_dashboard", renameArguments("新名称"), "rename-key"), harness.context);
        AgentExecutionContext approvalContext = context("user-1", "org-1", "session-1",
                "approval-request", "approval-correlation");

        WriteOperationView approved = harness.useCase.approve(proposal.approvalId(), approvalContext);
        WriteOperationView replay = harness.useCase.approve(proposal.approvalId(),
                context("user-1", "org-1", "session-1", "retry-request", "retry-correlation"));
        WriteOperationView terminalReconnect = harness.useCase.propose(new WriteProposalCommand(
                        "rename_dashboard", renameArguments("新名称"), "rename-key"),
                context("user-1", "org-1", "session-2", "reconnect-request", "reconnect-correlation"));

        assertEquals(WriteOperationState.SUCCEEDED, approved.state());
        assertEquals("dashboard-1", approved.resourceId());
        assertEquals(1, harness.business.executeCalls.get());
        assertTrue(replay.replayed());
        assertEquals(proposal.approvalId(), terminalReconnect.approvalId());
        assertEquals(WriteOperationState.SUCCEEDED, terminalReconnect.state());
        assertTrue(terminalReconnect.replayed());
        WriteAuditEvent success = harness.audits.stream()
                .filter(event -> event.eventType() == WriteAuditEventType.SUCCEEDED)
                .findFirst().orElseThrow();
        assertEquals("approval-request", success.requestId());
        assertEquals("approval-correlation", success.correlationId());
        assertEquals(proposal.changeId(), success.changeId());
    }

    @Test
    void rejectedOrExpiredApprovalMustNeverExecute() {
        Harness rejected = harness();
        WriteOperationView proposal = rejected.useCase.propose(new WriteProposalCommand(
                "rename_dashboard", renameArguments("拒绝名称"), "reject-key"), rejected.context);
        assertEquals(WriteOperationState.REJECTED,
                rejected.useCase.reject(proposal.approvalId(), rejected.context).state());
        WriteOperationView afterReject = rejected.useCase.approve(proposal.approvalId(), rejected.context);
        assertEquals(WriteOperationState.REJECTED, afterReject.state());
        assertTrue(afterReject.replayed());
        assertEquals(0, rejected.business.executeCalls.get());

        Harness expired = harness();
        WriteOperationView expiring = expired.useCase.propose(new WriteProposalCommand(
                "create_chart", createArguments("过期图表"), "expire-key"), expired.context);
        expired.clock.advance(Duration.ofMinutes(5));
        WriteOperationView afterExpiry = expired.useCase.approve(expiring.approvalId(), expired.context);
        assertEquals(WriteOperationState.EXPIRED, afterExpiry.state());
        assertEquals(0, expired.business.executeCalls.get());
    }

    @Test
    void listingWorkspaceMustPersistAndAuditExpiredApprovals() {
        Harness harness = harness();
        WriteOperationView proposal = harness.useCase.propose(new WriteProposalCommand(
                "create_chart", createArguments("列表过期图表"), "list-expire-key"), harness.context);
        harness.clock.advance(Duration.ofMinutes(5));

        WriteOperationView expired = harness.useCase.listOperations(harness.context).stream()
                .filter(operation -> operation.approvalId().equals(proposal.approvalId()))
                .findFirst().orElseThrow();

        assertEquals(WriteOperationState.EXPIRED, expired.state());
        assertEquals(WriteOperationState.EXPIRED,
                harness.store.operation(proposal.approvalId()).state());
        assertEquals(1, harness.audits.stream()
                .filter(event -> event.eventType() == WriteAuditEventType.EXPIRED)
                .count());
        assertEquals(0, harness.business.executeCalls.get());

        harness.useCase.listOperations(harness.context);
        assertEquals(1, harness.audits.stream()
                .filter(event -> event.eventType() == WriteAuditEventType.EXPIRED)
                .count());
    }

    @Test
    void idempotentReplayMustMaterializeExpiredApprovalBeforeReturning() {
        Harness harness = harness();
        WriteProposalCommand command = new WriteProposalCommand(
                "create_chart", createArguments("重放过期图表"), "replay-expire-key");
        WriteOperationView proposal = harness.useCase.propose(command, harness.context);
        harness.clock.advance(Duration.ofMinutes(5));

        WriteOperationView replay = harness.useCase.propose(command,
                context("user-1", "org-1", "session-2", "replay-request", "replay-correlation"));

        assertEquals(proposal.approvalId(), replay.approvalId());
        assertEquals(WriteOperationState.EXPIRED, replay.state());
        assertTrue(replay.replayed());
        assertEquals(WriteOperationState.EXPIRED,
                harness.store.operation(proposal.approvalId()).state());
        assertEquals(1, harness.audits.stream()
                .filter(event -> event.eventType() == WriteAuditEventType.EXPIRED)
                .count());
        assertEquals(1, harness.business.prepareCalls.get());
        assertEquals(0, harness.business.executeCalls.get());
    }

    @Test
    void businessPreparationMayOnlyAddBoundedAuthoritativePreviewDetails() {
        Harness harness = harness();
        harness.business.enrichPreview = true;

        WriteOperationView proposal = harness.useCase.propose(new WriteProposalCommand(
                "rename_dashboard", renameArguments("新名称"), "authoritative-preview-key"),
                harness.context);

        assertTrue(proposal.preview().fields().contains(new PreviewField("当前名称", "旧名称")));
        assertTrue(proposal.preview().impacts().contains("同步更新当前仪表盘导航名称"));

        Harness invalid = harness();
        invalid.business.replacePreview = true;
        ControlledWriteException failure = assertThrows(ControlledWriteException.class,
                () -> invalid.useCase.propose(new WriteProposalCommand(
                        "rename_dashboard", renameArguments("新名称"), "invalid-preview-key"), invalid.context));
        assertEquals(ControlledWriteException.Code.INVALID_BUSINESS_PREPARATION, failure.code());
        assertEquals(0, invalid.store.size());
    }

    @Test
    void identityOrganizationSessionAndApprovalIdTamperingMustFailBeforeExecute() {
        Harness harness = harness();
        WriteOperationView proposal = harness.useCase.propose(new WriteProposalCommand(
                "create_chart", createArguments("作用域图表"), "scope-key"), harness.context);
        List<AgentExecutionContext> mismatches = List.of(
                context("user-2", "org-1", "session-1", "r", "c"),
                context("user-1", "org-2", "session-1", "r", "c"),
                context("user-1", "org-1", "session-2", "r", "c"));
        for (AgentExecutionContext mismatch : mismatches) {
            ControlledWriteException exception = assertThrows(ControlledWriteException.class,
                    () -> harness.useCase.approve(proposal.approvalId(), mismatch));
            assertEquals(ControlledWriteException.Code.APPROVAL_SCOPE_MISMATCH, exception.code());
        }
        ControlledWriteException unknown = assertThrows(ControlledWriteException.class,
                () -> harness.useCase.approve("tampered-approval-id", harness.context));
        assertEquals(ControlledWriteException.Code.APPROVAL_NOT_FOUND, unknown.code());
        assertEquals(0, harness.business.executeCalls.get());
    }

    @Test
    void preparedStateOrPreviewTamperingMustFailIntegrityCheck() {
        Harness harness = harness();
        WriteOperationView proposal = harness.useCase.propose(new WriteProposalCommand(
                "rename_dashboard", renameArguments("安全名称"), "tamper-key"), harness.context);
        ControlledWriteOperation original = harness.store.operation(proposal.approvalId());
        PreparedWrite tamperedPrepared = new PreparedWrite(original.preparedWrite().action(),
                "tampered-state-fingerprint", original.preparedWrite().preview());
        harness.store.tamper(proposal.approvalId(), operation -> copy(operation,
                operation.canonicalParameters(), operation.parametersDigest(), operation.preparedDigest(),
                tamperedPrepared));

        ControlledWriteException exception = assertThrows(ControlledWriteException.class,
                () -> harness.useCase.approve(proposal.approvalId(), harness.context));
        assertEquals(ControlledWriteException.Code.APPROVAL_TAMPERED, exception.code());
        assertEquals(0, harness.business.executeCalls.get());

        WritePreview tamperedPreview = new WritePreview("篡改预览", "DASHBOARD", "dashboard-1",
                original.preparedWrite().preview().fields(), List.of("篡改影响"));
        harness.store.tamper(proposal.approvalId(), operation -> copy(original,
                original.canonicalParameters(), original.parametersDigest(), original.preparedDigest(),
                new PreparedWrite(original.preparedWrite().action(),
                        original.preparedWrite().expectedStateDigest(), tamperedPreview)));
        assertThrows(ControlledWriteException.class,
                () -> harness.useCase.approve(proposal.approvalId(), harness.context));
        assertEquals(0, harness.business.executeCalls.get());

        ObjectValue tamperedParameters = object("dashboardId", text("dashboard-1"),
                "newName", text("篡改名称"));
        harness.store.tamper(proposal.approvalId(), operation -> copy(original,
                tamperedParameters, original.parametersDigest(), original.preparedDigest(),
                original.preparedWrite()));
        assertThrows(ControlledWriteException.class,
                () -> harness.useCase.approve(proposal.approvalId(), harness.context));
        assertEquals(0, harness.business.executeCalls.get());
    }

    @Test
    void revokedPermissionMustPropagateAndFailureAuditMustBeFinite() {
        Harness harness = harness();
        WriteOperationView proposal = harness.useCase.propose(new WriteProposalCommand(
                "rename_dashboard", renameArguments("撤权名称"), "revoked-key"), harness.context);
        RevokedException revoked = new RevokedException("raw-revoked-sensitive-detail");
        harness.business.executeFailure = revoked;

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> harness.useCase.approve(proposal.approvalId(), harness.context));
        assertSame(revoked, actual);
        assertEquals(WriteOperationState.PENDING,
                harness.store.operation(proposal.approvalId()).state());

        AgentExecutionContext failureContext = context("user-1", "org-1", "session-1",
                "failure-request", "failure-correlation");
        WriteOperationView failed = harness.useCase.markFailed(proposal.approvalId(),
                WriteFailureCategory.ACCESS_DENIED, failureContext);
        assertEquals(WriteOperationState.FAILED, failed.state());
        WriteAuditEvent event = harness.audits.stream()
                .filter(value -> value.eventType() == WriteAuditEventType.FAILED)
                .findFirst().orElseThrow();
        assertEquals(WriteFailureCategory.ACCESS_DENIED, event.failure());
        assertEquals("failure-request", event.requestId());
        assertFalse(harness.audits.toString().contains("raw-revoked-sensitive-detail"));
    }

    @Test
    void concurrentEquivalentProposalsMustCreateOnePersistentOperation() throws Exception {
        Harness harness = harness();
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<java.util.concurrent.Callable<WriteOperationView>> tasks = new ArrayList<>();
            for (int index = 0; index < 24; index++) {
                tasks.add(() -> harness.useCase.propose(new WriteProposalCommand(
                        "create_chart", createArguments("并发图表"), "concurrent-key"), harness.context));
            }
            List<WriteOperationView> results = new ArrayList<>();
            for (var future : executor.invokeAll(tasks)) {
                results.add(future.get());
            }
            assertEquals(1, results.stream().map(WriteOperationView::approvalId).distinct().count());
        }
        assertEquals(1, harness.store.size());
        assertEquals(0, harness.business.executeCalls.get());
    }

    @Test
    void workspaceListMustBeBoundedSortedAndRejectStoreScopeLeak() {
        Harness harness = harness();
        WriteOperationView first = harness.useCase.propose(new WriteProposalCommand(
                "create_chart", createArguments("第一图表"), "list-key-1"), harness.context);
        harness.clock.advance(Duration.ofSeconds(1));
        WriteOperationView second = harness.useCase.propose(new WriteProposalCommand(
                "rename_dashboard", renameArguments("第二名称"), "list-key-2"), harness.context);

        assertEquals(List.of(second.approvalId(), first.approvalId()), harness.useCase
                .listOperations(harness.context).stream().map(WriteOperationView::approvalId).toList());
        assertTrue(harness.useCase.listOperations(context("user-1", "org-1", "other-session",
                "r", "c")).isEmpty());

        harness.store.leakAllOnList = true;
        ControlledWriteException exception = assertThrows(ControlledWriteException.class,
                () -> harness.useCase.listOperations(context("user-1", "org-1", "other-session",
                        "r", "c")));
        assertEquals(ControlledWriteException.Code.APPROVAL_TAMPERED, exception.code());
    }

    private Harness harness() {
        InMemoryStore store = new InMemoryStore();
        RecordingBusiness business = new RecordingBusiness();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-12T12:00:00Z"));
        List<WriteAuditEvent> audits = new CopyOnWriteArrayList<>();
        AtomicInteger ids = new AtomicInteger();
        DefaultControlledWriteUseCase useCase = new DefaultControlledWriteUseCase(
                new DefaultWriteProposalToolRegistry(List.of(
                        new CreateChartWriteProposalTool(), new RenameDashboardWriteProposalTool())),
                business, store, audits::add, () -> "server-id-" + ids.incrementAndGet(),
                clock::now, Duration.ofMinutes(5));
        AgentExecutionContext context = context("user-1", "org-1", "session-1",
                "request-1", "correlation-1");
        return new Harness(useCase, store, business, clock, audits, context);
    }

    private static AgentExecutionContext context(String subject,
                                                 String organization,
                                                 String session,
                                                 String request,
                                                 String correlation) {
        return new AgentExecutionContext(session, request,
                new QueryExecutionContext(Channel.AUTHENTICATED, subject, organization, correlation));
    }

    private static ObjectValue createArguments(String name) {
        return object("name", text(name), "viewId", text("view-1"));
    }

    private static ObjectValue renameArguments(String name) {
        return object("dashboardId", text("dashboard-1"), "newName", text(name));
    }

    private static ObjectValue object(Object... entries) {
        Map<String, StructuredValue> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            values.put((String) entries[index], (StructuredValue) entries[index + 1]);
        }
        return new ObjectValue(values);
    }

    private static ObjectValue with(ObjectValue source, String name, StructuredValue value) {
        Map<String, StructuredValue> values = new LinkedHashMap<>(source.values());
        values.put(name, value);
        return new ObjectValue(values);
    }

    private static StructuredValue.TextValue text(String value) {
        return StructuredValue.text(value);
    }

    private static ControlledWriteOperation copy(ControlledWriteOperation operation,
                                                   ObjectValue parameters,
                                                   String parametersDigest,
                                                   String preparedDigest,
                                                   PreparedWrite prepared) {
        return new ControlledWriteOperation(operation.approvalId(), operation.changeId(), operation.context(),
                operation.toolName(), parameters, parametersDigest, preparedDigest,
                operation.idempotencyKeyDigest(), prepared, operation.createdAt(), operation.expiresAt(),
                operation.state(), operation.resourceId(), operation.failure(), operation.completedAt());
    }

    private record Harness(DefaultControlledWriteUseCase useCase,
                           InMemoryStore store,
                           RecordingBusiness business,
                           MutableClock clock,
                           List<WriteAuditEvent> audits,
                           AgentExecutionContext context) {
    }

    private static final class MutableClock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant current) {
            this.current = new AtomicReference<>(current);
        }

        private Instant now() {
            return current.get();
        }

        private void advance(Duration duration) {
            current.updateAndGet(value -> value.plus(duration));
        }
    }

    private static final class RecordingBusiness implements VisualizationWritePort {
        private final AtomicInteger prepareCalls = new AtomicInteger();
        private final AtomicInteger executeCalls = new AtomicInteger();
        private volatile RuntimeException executeFailure;
        private volatile boolean enrichPreview;
        private volatile boolean replacePreview;

        @Override
        public PreparedWrite prepare(WriteAction action,
                                     WritePreview safePreview,
                                     TrustedWriteContext context) {
            prepareCalls.incrementAndGet();
            WritePreview preview = safePreview;
            if (enrichPreview) {
                List<PreviewField> fields = new ArrayList<>(safePreview.fields());
                fields.add(new PreviewField("当前名称", "旧名称"));
                List<String> impacts = new ArrayList<>(safePreview.impacts());
                impacts.add("同步更新当前仪表盘导航名称");
                preview = new WritePreview(safePreview.title(), safePreview.resourceType(),
                        safePreview.targetId(), fields, impacts);
            }
            if (replacePreview) {
                preview = new WritePreview("替换后的不可信标题", safePreview.resourceType(),
                        safePreview.targetId(), safePreview.fields(), safePreview.impacts());
            }
            return new PreparedWrite(action, "state-" + action.toolName(), preview);
        }

        @Override
        public WriteExecutionResult execute(PreparedWrite operation, TrustedWriteContext context) {
            executeCalls.incrementAndGet();
            if (executeFailure != null) {
                throw executeFailure;
            }
            if (operation.action() instanceof yubi.agent.domain.ControlledWriteModels.RenameDashboardAction rename) {
                return new WriteExecutionResult(rename.dashboardId());
            }
            return new WriteExecutionResult("created-chart-1");
        }
    }

    private static final class InMemoryStore implements WriteOperationStorePort {
        private final Map<String, ControlledWriteOperation> byApproval = new ConcurrentHashMap<>();
        private final Map<WriteIdempotencyScope, String> byIdempotency = new ConcurrentHashMap<>();
        private volatile boolean leakAllOnList;

        @Override
        public Optional<ControlledWriteOperation> findByIdempotency(WriteIdempotencyScope scope) {
            String approvalId = byIdempotency.get(scope);
            return approvalId == null ? Optional.empty() : Optional.ofNullable(byApproval.get(approvalId));
        }

        @Override
        public List<ControlledWriteOperation> listByScope(WriteApprovalScope scope, int maximumItems) {
            return byApproval.values().stream()
                    .filter(operation -> leakAllOnList || operation.approvalScope().equals(scope))
                    .limit(maximumItems)
                    .toList();
        }

        @Override
        public synchronized CreateResult createPending(ControlledWriteOperation operation) {
            String existingId = byIdempotency.get(operation.idempotencyScope());
            if (existingId != null) {
                return new CreateResult(byApproval.get(existingId), false);
            }
            byApproval.put(operation.approvalId(), operation);
            byIdempotency.put(operation.idempotencyScope(), operation.approvalId());
            return new CreateResult(operation, true);
        }

        @Override
        public Optional<ControlledWriteOperation> lockByApprovalId(String approvalId) {
            return Optional.ofNullable(byApproval.get(approvalId));
        }

        @Override
        public synchronized void saveLocked(ControlledWriteOperation previous,
                                            ControlledWriteOperation updated) {
            if (!byApproval.replace(previous.approvalId(), previous, updated)) {
                throw new IllegalStateException("状态竞争");
            }
        }

        private ControlledWriteOperation operation(String approvalId) {
            return byApproval.get(approvalId);
        }

        private int size() {
            return byApproval.size();
        }

        private void tamper(String approvalId, UnaryOperator<ControlledWriteOperation> mutation) {
            byApproval.computeIfPresent(approvalId, (ignored, operation) -> mutation.apply(operation));
        }
    }

    private static final class RevokedException extends RuntimeException {
        private RevokedException(String message) {
            super(message);
        }
    }
}

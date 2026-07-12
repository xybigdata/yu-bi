package yubi.visualization.write.application;

import org.junit.jupiter.api.Test;
import yubi.visualization.write.api.BusinessResourceChange;
import yubi.visualization.write.api.PreparedRenameDashboard;
import yubi.visualization.write.api.RenameDashboardCommand;
import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.api.VisualizationWriteException;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.DashboardTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ParentTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.RenamedDashboard;
import yubi.visualization.write.domain.VisualizationWriteModels.ViewTarget;
import yubi.visualization.write.port.DashboardWritePort;
import yubi.visualization.write.port.VisualizationWriteAuthorizationPort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.ACCESS_DENIED;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.CONFLICT;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.EXECUTION;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.STALE;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.VALIDATION;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.DASHBOARD_NAME_CONFLICT;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.DASHBOARD_STATE_STALE;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_PREPARED_RENAME_DASHBOARD;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_RENAME_DASHBOARD_COMMAND;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.RENAME_DASHBOARD_ACCESS_DENIED;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.RENAME_DASHBOARD_EXECUTION_FAILED;
import static yubi.visualization.write.domain.VisualizationWriteModels.ChangeType.RENAMED;
import static yubi.visualization.write.domain.VisualizationWriteModels.ResourceType.DASHBOARD;

class DefaultRenameDashboardServiceTest {

    @Test
    void shouldPrepareFingerprintAndExecuteOnlyThroughAtomicRename() {
        Fixture fixture = fixture();

        PreparedRenameDashboard prepared = fixture.service.prepare(
                new RenameDashboardCommand("dashboard-1", "  新名称  "), context("prepare", "prepare-correlation"));

        assertEquals(new RenameDashboardCommand("dashboard-1", "新名称"), prepared.command());
        assertEquals("dashboard-fingerprint-1", prepared.stateFingerprint());
        assertEquals(List.of("dashboardId", "currentName", "newName"),
                prepared.preview().safeParameters().keySet().stream().toList());
        assertEquals("旧名称", prepared.preview().safeParameters().get("currentName"));

        BusinessResourceChange change = fixture.service.execute(prepared,
                context("approve", "approve-correlation"));

        assertEquals(DASHBOARD, change.resourceType());
        assertEquals(RENAMED, change.changeType());
        assertEquals("dashboard-1", change.resourceId());
        assertEquals("新名称", change.resourceName());
        assertEquals("dashboard-fingerprint-1", change.beforeStateFingerprint());
        assertEquals("dashboard-fingerprint-2", change.afterStateFingerprint());
        assertEquals(1, fixture.dashboard.findCalls);
        assertEquals(1, fixture.dashboard.nameCheckCalls);
        assertEquals(1, fixture.authorization.renameCalls);
        assertEquals(1, fixture.dashboard.renameCalls);
        assertEquals("dashboard-fingerprint-1", fixture.dashboard.expectedFingerprint);
        assertEquals("approve", fixture.dashboard.lastContext.requestId());
    }

    @Test
    void shouldRejectInvalidAndNoOpRenameBeforeMutation() {
        Fixture fixture = fixture();
        List<RenameDashboardCommand> invalid = List.of(
                new RenameDashboardCommand(" ", "new"),
                new RenameDashboardCommand("dashboard 1", "new"),
                new RenameDashboardCommand("dashboard-1", " "),
                new RenameDashboardCommand("dashboard-1", "x".repeat(256)));
        for (RenameDashboardCommand command : invalid) {
            assertFailure(() -> fixture.service.prepare(command, context("request", "correlation")),
                    INVALID_RENAME_DASHBOARD_COMMAND, VALIDATION);
        }
        assertFailure(() -> fixture.service.prepare(new RenameDashboardCommand("dashboard-1", "旧名称"),
                        context("request", "correlation")), INVALID_RENAME_DASHBOARD_COMMAND, VALIDATION);
        assertEquals(0, fixture.dashboard.renameCalls);
    }

    @Test
    void shouldRejectPreparedCommandReplayedByDifferentTrustedScope() {
        Fixture fixture = fixture();
        PreparedRenameDashboard prepared = fixture.service.prepare(command(), context("request", "correlation"));
        int callsAfterPrepare = fixture.dashboard.totalCalls();

        List<VisualizationWriteContext> mismatches = List.of(
                new VisualizationWriteContext("subject-2", "org-1", "session-1", "r", "c"),
                new VisualizationWriteContext("subject-1", "org-2", "session-1", "r", "c"),
                new VisualizationWriteContext("subject-1", "org-1", "session-2", "r", "c"));
        for (VisualizationWriteContext mismatch : mismatches) {
            assertFailure(() -> fixture.service.execute(prepared, mismatch),
                    INVALID_PREPARED_RENAME_DASHBOARD, VALIDATION);
        }
        assertEquals(callsAfterPrepare, fixture.dashboard.totalCalls());
        assertEquals(0, fixture.dashboard.renameCalls);
    }

    @Test
    void shouldMapAtomicStaleResultWithoutPreLockReads() {
        Fixture fixture = fixture();
        PreparedRenameDashboard prepared = fixture.service.prepare(command(), context("request", "correlation"));
        fixture.dashboard.outcome = RenameDashboardOutcome.stale();

        assertFailure(() -> fixture.service.execute(prepared, context("approve", "approve-correlation")),
                DASHBOARD_STATE_STALE, STALE);
        assertEquals(1, fixture.authorization.renameCalls);
        assertEquals(1, fixture.dashboard.findCalls);
        assertEquals(1, fixture.dashboard.renameCalls);
    }

    @Test
    void shouldRejectRevokedPermissionBeforeStaleCheckAndMutation() {
        Fixture fixture = fixture();
        PreparedRenameDashboard prepared = fixture.service.prepare(command(), context("request", "correlation"));
        fixture.dashboard.outcome = RenameDashboardOutcome.accessDenied();

        assertFailure(() -> fixture.service.execute(prepared, context("approve", "approve-correlation")),
                RENAME_DASHBOARD_ACCESS_DENIED, ACCESS_DENIED);
        assertEquals(1, fixture.authorization.renameCalls);
        assertEquals(1, fixture.dashboard.renameCalls);
    }

    @Test
    void shouldRejectOrganizationMismatchWithoutCallingAuthorizationOrMutation() {
        Fixture fixture = fixture();
        fixture.dashboard.target = Optional.of(new DashboardTarget(
                "dashboard-1", "org-2", "旧名称", "folder-1", "dashboard-fingerprint-1"));

        assertFailure(() -> fixture.service.prepare(command(), context("request", "correlation")),
                RENAME_DASHBOARD_ACCESS_DENIED, ACCESS_DENIED);
        assertEquals(0, fixture.authorization.renameCalls);
        assertEquals(0, fixture.dashboard.renameCalls);
    }

    @Test
    void shouldRejectNameConflictsAtPrepareExecutionAndFinalMutationRace() {
        Fixture prepareConflict = fixture();
        prepareConflict.dashboard.nameExists = true;
        assertFailure(() -> prepareConflict.service.prepare(command(), context("request", "correlation")),
                DASHBOARD_NAME_CONFLICT, CONFLICT);

        Fixture raceConflict = fixture();
        PreparedRenameDashboard racePrepared = raceConflict.service.prepare(command(),
                context("request", "correlation"));
        raceConflict.dashboard.outcome = RenameDashboardOutcome.conflict();
        assertFailure(() -> raceConflict.service.execute(racePrepared, context("approve", "approve-correlation")),
                DASHBOARD_NAME_CONFLICT, CONFLICT);
        assertEquals(1, raceConflict.dashboard.renameCalls);
    }

    @Test
    void shouldMapFinalAtomicStaleOutcomeWithoutReturningSuccess() {
        Fixture fixture = fixture();
        PreparedRenameDashboard prepared = fixture.service.prepare(command(), context("request", "correlation"));
        fixture.dashboard.outcome = RenameDashboardOutcome.stale();

        assertFailure(() -> fixture.service.execute(prepared, context("approve", "approve-correlation")),
                DASHBOARD_STATE_STALE, STALE);
        assertEquals(1, fixture.dashboard.renameCalls);
    }

    @Test
    void shouldSanitizeReadAuthorizationAndMutationPortFailures() {
        String canary = "jdbc:secret token=raw password=value";

        Fixture readFailure = fixture();
        readFailure.dashboard.failure = new IllegalArgumentException(canary);
        assertSanitized(() -> readFailure.service.prepare(command(), context("request", "correlation")),
                canary);

        Fixture authorizationFailure = fixture();
        authorizationFailure.authorization.failure = new IllegalStateException(canary);
        assertSanitized(() -> authorizationFailure.service.prepare(command(), context("request", "correlation")),
                canary);

        Fixture mutationFailure = fixture();
        PreparedRenameDashboard prepared = mutationFailure.service.prepare(command(),
                context("request", "correlation"));
        mutationFailure.dashboard.failOnlyMutation = new IllegalStateException(canary);
        assertSanitized(() -> mutationFailure.service.execute(prepared, context("approve", "approve-correlation")),
                canary);
    }

    @Test
    void shouldRejectMalformedRenameResultAsStableExecutionFailure() {
        Fixture fixture = fixture();
        PreparedRenameDashboard prepared = fixture.service.prepare(command(), context("request", "correlation"));
        fixture.dashboard.outcome = RenameDashboardOutcome.renamed(new RenamedDashboard(
                "dashboard-1", "旧名称", "错误名称", "dashboard-fingerprint-2"));

        assertFailure(() -> fixture.service.execute(prepared, context("approve", "approve-correlation")),
                RENAME_DASHBOARD_EXECUTION_FAILED, EXECUTION);
    }

    private Fixture fixture() {
        FakeDashboardPort dashboard = new FakeDashboardPort();
        FakeAuthorizationPort authorization = new FakeAuthorizationPort();
        return new Fixture(new DefaultRenameDashboardService(dashboard, authorization), dashboard, authorization);
    }

    private RenameDashboardCommand command() {
        return new RenameDashboardCommand("dashboard-1", "新名称");
    }

    private VisualizationWriteContext context(String requestId, String correlationId) {
        return new VisualizationWriteContext("subject-1", "org-1", "session-1", requestId, correlationId);
    }

    private void assertFailure(org.junit.jupiter.api.function.Executable operation,
                               yubi.visualization.write.api.VisualizationWriteFailureCode code,
                               yubi.visualization.write.api.VisualizationWriteFailureCategory category) {
        VisualizationWriteException failure = assertThrows(VisualizationWriteException.class, operation);
        assertEquals(code, failure.code());
        assertEquals(category, failure.category());
        assertNull(failure.getCause());
    }

    private void assertSanitized(org.junit.jupiter.api.function.Executable operation, String canary) {
        VisualizationWriteException failure = assertThrows(VisualizationWriteException.class, operation);
        assertEquals(RENAME_DASHBOARD_EXECUTION_FAILED, failure.code());
        assertEquals(EXECUTION, failure.category());
        assertNull(failure.getCause());
        assertFalse(failure.getMessage().contains(canary));
        assertFalse(failure.toString().contains(canary));
    }

    private record Fixture(DefaultRenameDashboardService service,
                           FakeDashboardPort dashboard,
                           FakeAuthorizationPort authorization) {
    }

    private static final class FakeAuthorizationPort implements VisualizationWriteAuthorizationPort {
        private boolean renameAllowed = true;
        private int renameCalls;
        private RuntimeException failure;

        @Override
        public boolean canCreateChart(VisualizationWriteContext context, ViewTarget view, ParentTarget parent) {
            return false;
        }

        @Override
        public boolean canRenameDashboard(VisualizationWriteContext context, DashboardTarget dashboard) {
            renameCalls++;
            if (failure != null) {
                throw failure;
            }
            return renameAllowed;
        }
    }

    private static final class FakeDashboardPort implements DashboardWritePort {
        private Optional<DashboardTarget> target = Optional.of(new DashboardTarget(
                "dashboard-1", "org-1", "旧名称", "folder-1", "dashboard-fingerprint-1"));
        private boolean nameExists;
        private RenameDashboardOutcome outcome = RenameDashboardOutcome.renamed(new RenamedDashboard(
                "dashboard-1", "旧名称", "新名称", "dashboard-fingerprint-2"));
        private RuntimeException failure;
        private RuntimeException failOnlyMutation;
        private int findCalls;
        private int nameCheckCalls;
        private int renameCalls;
        private String expectedFingerprint;
        private VisualizationWriteContext lastContext;

        @Override
        public Optional<DashboardTarget> findDashboard(String dashboardId) {
            findCalls++;
            failIfConfigured();
            return target;
        }

        @Override
        public boolean dashboardNameExists(String organizationId,
                                           String parentId,
                                           String name,
                                           String excludedDashboardId) {
            nameCheckCalls++;
            failIfConfigured();
            return nameExists;
        }

        @Override
        public RenameDashboardOutcome renameDashboard(RenameDashboardDraft draft,
                                                      String expectedStateFingerprint,
                                                      VisualizationWriteContext context) {
            renameCalls++;
            expectedFingerprint = expectedStateFingerprint;
            lastContext = context;
            if (failOnlyMutation != null) {
                throw failOnlyMutation;
            }
            failIfConfigured();
            return outcome;
        }

        private int totalCalls() {
            return findCalls + nameCheckCalls + renameCalls;
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}

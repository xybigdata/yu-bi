package yubi.visualization.write.application;

import org.junit.jupiter.api.Test;
import yubi.visualization.write.api.BusinessResourceChange;
import yubi.visualization.write.api.CreateChartCommand;
import yubi.visualization.write.api.PreparedCreateChart;
import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.api.VisualizationWriteException;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.CreatedChart;
import yubi.visualization.write.domain.VisualizationWriteModels.DashboardTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ParentTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ViewTarget;
import yubi.visualization.write.port.ChartWritePort;
import yubi.visualization.write.port.VisualizationWriteAuthorizationPort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.ACCESS_DENIED;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.CONFLICT;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.EXECUTION;
import static yubi.visualization.write.api.VisualizationWriteFailureCategory.VALIDATION;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.CHART_NAME_CONFLICT;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.CREATE_CHART_ACCESS_DENIED;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.CREATE_CHART_EXECUTION_FAILED;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_CREATE_CHART_COMMAND;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_PREPARED_CREATE_CHART;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_TRUSTED_CONTEXT;
import static yubi.visualization.write.domain.VisualizationWriteModels.ChangeType.CREATED;
import static yubi.visualization.write.domain.VisualizationWriteModels.ResourceType.CHART;

class DefaultCreateChartServiceTest {

    @Test
    void shouldPreparePreviewAndExecuteNormalizedCommandOnlyThroughAtomicPort() {
        Fixture fixture = fixture();
        VisualizationWriteContext preparedContext = context("request-prepare", "correlation-prepare");
        CreateChartCommand input = new CreateChartCommand(
                "  订单概览  ", "view-1", "folder-1", "  展示订单指标  ");

        PreparedCreateChart prepared = fixture.service.prepare(input, preparedContext);

        assertEquals(new CreateChartCommand("订单概览", "view-1", "folder-1", "展示订单指标"),
                prepared.command());
        assertEquals("创建图表", prepared.preview().title());
        assertEquals(List.of("name", "viewId", "parentId", "description"),
                prepared.preview().safeParameters().keySet().stream().toList());
        assertEquals("subject-1", prepared.binding().subjectId());
        assertEquals("request-prepare", prepared.binding().originatingRequestId());
        assertThrows(UnsupportedOperationException.class,
                () -> prepared.preview().safeParameters().put("status", "PUBLISHED"));
        assertThrows(UnsupportedOperationException.class, () -> prepared.preview().impacts().clear());

        BusinessResourceChange change = fixture.service.execute(prepared,
                context("request-approve", "correlation-approve"));

        assertEquals(CHART, change.resourceType());
        assertEquals(CREATED, change.changeType());
        assertEquals("chart-1", change.resourceId());
        assertEquals("订单概览", change.resourceName());
        assertNull(change.beforeStateFingerprint());
        assertEquals("chart-fingerprint-1", change.afterStateFingerprint());
        assertEquals(1, fixture.chart.findViewCalls);
        assertEquals(1, fixture.chart.findParentCalls);
        assertEquals(1, fixture.chart.nameCheckCalls);
        assertEquals(1, fixture.authorization.createCalls);
        assertEquals(1, fixture.chart.createCalls);
        assertEquals(prepared.command().name(), fixture.chart.lastDraft.name());
        assertEquals("subject-1", fixture.chart.lastContext.subjectId());
        assertEquals("request-approve", fixture.chart.lastContext.requestId());
    }

    @Test
    void shouldRejectInvalidCommandsAndTrustedContextBeforeCallingPorts() {
        Fixture fixture = fixture();
        List<CreateChartCommand> invalid = List.of(
                new CreateChartCommand(" ", "view-1", null, null),
                new CreateChartCommand("chart", " ", null, null),
                new CreateChartCommand("chart", "view 1", null, null),
                new CreateChartCommand("x".repeat(256), "view-1", null, null));

        for (CreateChartCommand command : invalid) {
            assertFailure(() -> fixture.service.prepare(command, context("request", "correlation")),
                    INVALID_CREATE_CHART_COMMAND, VALIDATION);
        }
        assertFailure(() -> fixture.service.prepare(null, context("request", "correlation")),
                INVALID_CREATE_CHART_COMMAND, VALIDATION);
        assertFailure(() -> fixture.service.prepare(new CreateChartCommand("chart", "view-1", null, null),
                        new VisualizationWriteContext("subject-1", " ", "session-1", "request", "correlation")),
                INVALID_TRUSTED_CONTEXT, VALIDATION);
        assertEquals(0, fixture.chart.totalCalls());
        assertEquals(0, fixture.authorization.createCalls);
    }

    @Test
    void shouldBindPreparedCommandToSubjectOrganizationAndSession() {
        Fixture fixture = fixture();
        PreparedCreateChart prepared = fixture.service.prepare(command(), context("request", "correlation"));
        int callsAfterPrepare = fixture.chart.totalCalls();

        List<VisualizationWriteContext> mismatches = List.of(
                new VisualizationWriteContext("subject-2", "org-1", "session-1", "r2", "c2"),
                new VisualizationWriteContext("subject-1", "org-2", "session-1", "r2", "c2"),
                new VisualizationWriteContext("subject-1", "org-1", "session-2", "r2", "c2"));
        for (VisualizationWriteContext mismatch : mismatches) {
            assertFailure(() -> fixture.service.execute(prepared, mismatch),
                    INVALID_PREPARED_CREATE_CHART, VALIDATION);
        }
        assertEquals(callsAfterPrepare, fixture.chart.totalCalls());
        assertEquals(0, fixture.chart.createCalls);
    }

    @Test
    void shouldRecheckOrganizationAndPermissionAtExecutionWithoutSideEffect() {
        Fixture fixture = fixture();
        PreparedCreateChart prepared = fixture.service.prepare(command(), context("request", "correlation"));

        fixture.chart.outcome = CreateChartOutcome.accessDenied();
        assertFailure(() -> fixture.service.execute(prepared, context("approve", "approve-correlation")),
                CREATE_CHART_ACCESS_DENIED, ACCESS_DENIED);
        assertEquals(1, fixture.chart.findViewCalls);
        assertEquals(1, fixture.authorization.createCalls);
        assertEquals(1, fixture.chart.createCalls);

        Fixture crossOrganization = fixture();
        crossOrganization.chart.view = Optional.of(new ViewTarget("view-1", "org-2"));
        assertFailure(() -> crossOrganization.service.prepare(command(), context("request", "correlation")),
                CREATE_CHART_ACCESS_DENIED, ACCESS_DENIED);
        assertEquals(0, crossOrganization.authorization.createCalls);
        assertEquals(0, crossOrganization.chart.createCalls);
    }

    @Test
    void shouldRejectNameConflictsAtPrepareExecutionAndAtomicMutation() {
        Fixture prepareConflict = fixture();
        prepareConflict.chart.nameExists = true;
        assertFailure(() -> prepareConflict.service.prepare(command(), context("request", "correlation")),
                CHART_NAME_CONFLICT, CONFLICT);
        assertEquals(0, prepareConflict.chart.createCalls);

        Fixture raceConflict = fixture();
        PreparedCreateChart racePrepared = raceConflict.service.prepare(command(), context("request", "correlation"));
        raceConflict.chart.outcome = CreateChartOutcome.conflict();
        assertFailure(() -> raceConflict.service.execute(racePrepared, context("approve", "approve-correlation")),
                CHART_NAME_CONFLICT, CONFLICT);
        assertEquals(1, raceConflict.chart.createCalls);
    }

    @Test
    void shouldSanitizeLookupAuthorizationAndMutationFailures() {
        String canary = "jdbc:mysql://secret password=raw";

        Fixture lookupFailure = fixture();
        lookupFailure.chart.failure = new IllegalStateException(canary);
        assertSanitized(() -> lookupFailure.service.prepare(command(), context("request", "correlation")),
                CREATE_CHART_EXECUTION_FAILED, canary);

        Fixture authorizationFailure = fixture();
        authorizationFailure.authorization.failure = new IllegalStateException(canary);
        assertSanitized(() -> authorizationFailure.service.prepare(command(), context("request", "correlation")),
                CREATE_CHART_EXECUTION_FAILED, canary);

        Fixture mutationFailure = fixture();
        PreparedCreateChart prepared = mutationFailure.service.prepare(command(), context("request", "correlation"));
        mutationFailure.chart.failOnlyMutation = new IllegalStateException(canary);
        assertSanitized(() -> mutationFailure.service.execute(prepared, context("approve", "approve-correlation")),
                CREATE_CHART_EXECUTION_FAILED, canary);
        assertEquals(1, mutationFailure.chart.createCalls);
    }

    @Test
    void shouldRejectMalformedPortResultAsStableExecutionFailure() {
        Fixture fixture = fixture();
        PreparedCreateChart prepared = fixture.service.prepare(command(), context("request", "correlation"));
        fixture.chart.outcome = CreateChartOutcome.created(new CreatedChart("chart-1", "wrong-name", "fp"));

        assertFailure(() -> fixture.service.execute(prepared, context("approve", "approve-correlation")),
                CREATE_CHART_EXECUTION_FAILED, EXECUTION);
    }

    private Fixture fixture() {
        FakeChartPort chart = new FakeChartPort();
        FakeAuthorizationPort authorization = new FakeAuthorizationPort();
        return new Fixture(new DefaultCreateChartService(chart, authorization), chart, authorization);
    }

    private CreateChartCommand command() {
        return new CreateChartCommand("订单概览", "view-1", "folder-1", "展示订单指标");
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

    private void assertSanitized(org.junit.jupiter.api.function.Executable operation,
                                 yubi.visualization.write.api.VisualizationWriteFailureCode code,
                                 String canary) {
        VisualizationWriteException failure = assertThrows(VisualizationWriteException.class, operation);
        assertEquals(code, failure.code());
        assertEquals(EXECUTION, failure.category());
        assertNull(failure.getCause());
        assertFalse(failure.getMessage().contains(canary));
        assertFalse(failure.toString().contains(canary));
    }

    private record Fixture(DefaultCreateChartService service,
                           FakeChartPort chart,
                           FakeAuthorizationPort authorization) {
    }

    private static final class FakeAuthorizationPort implements VisualizationWriteAuthorizationPort {
        private boolean createAllowed = true;
        private int createCalls;
        private RuntimeException failure;

        @Override
        public boolean canCreateChart(VisualizationWriteContext context, ViewTarget view, ParentTarget parent) {
            createCalls++;
            if (failure != null) {
                throw failure;
            }
            return createAllowed;
        }

        @Override
        public boolean canRenameDashboard(VisualizationWriteContext context, DashboardTarget dashboard) {
            return false;
        }
    }

    private static final class FakeChartPort implements ChartWritePort {
        private Optional<ViewTarget> view = Optional.of(new ViewTarget("view-1", "org-1"));
        private Optional<ParentTarget> parent = Optional.of(new ParentTarget("folder-1", "org-1"));
        private boolean nameExists;
        private CreateChartOutcome outcome = CreateChartOutcome.created(
                new CreatedChart("chart-1", "订单概览", "chart-fingerprint-1"));
        private RuntimeException failure;
        private RuntimeException failOnlyMutation;
        private int findViewCalls;
        private int findParentCalls;
        private int nameCheckCalls;
        private int createCalls;
        private CreateChartDraft lastDraft;
        private VisualizationWriteContext lastContext;

        @Override
        public Optional<ViewTarget> findView(String viewId) {
            findViewCalls++;
            failIfConfigured();
            return view;
        }

        @Override
        public Optional<ParentTarget> findParent(String parentId) {
            findParentCalls++;
            failIfConfigured();
            return parent;
        }

        @Override
        public boolean chartNameExists(String organizationId, String parentId, String name) {
            nameCheckCalls++;
            failIfConfigured();
            return nameExists;
        }

        @Override
        public CreateChartOutcome createChart(CreateChartDraft draft, VisualizationWriteContext context) {
            createCalls++;
            lastDraft = draft;
            lastContext = context;
            if (failOnlyMutation != null) {
                throw failOnlyMutation;
            }
            failIfConfigured();
            return outcome;
        }

        private int totalCalls() {
            return findViewCalls + findParentCalls + nameCheckCalls + createCalls;
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}

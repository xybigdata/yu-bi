package yubi.visualization.write.application;

import yubi.visualization.write.api.BusinessResourceChange;
import yubi.visualization.write.api.PreparedRenameDashboard;
import yubi.visualization.write.api.RenameDashboardCommand;
import yubi.visualization.write.api.RenameDashboardUseCase;
import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.api.WritePreview;
import yubi.visualization.write.domain.VisualizationWriteModels.DashboardTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardStatus;
import yubi.visualization.write.domain.VisualizationWriteModels.RenamedDashboard;
import yubi.visualization.write.port.DashboardWritePort;
import yubi.visualization.write.port.VisualizationWriteAuthorizationPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static yubi.visualization.write.api.VisualizationWriteFailureCode.DASHBOARD_NAME_CONFLICT;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.DASHBOARD_STATE_STALE;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_PREPARED_RENAME_DASHBOARD;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_RENAME_DASHBOARD_COMMAND;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.RENAME_DASHBOARD_ACCESS_DENIED;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.RENAME_DASHBOARD_EXECUTION_FAILED;
import static yubi.visualization.write.application.WriteApplicationSupport.failure;
import static yubi.visualization.write.domain.VisualizationWriteModels.ChangeType.RENAMED;
import static yubi.visualization.write.domain.VisualizationWriteModels.ResourceType.DASHBOARD;

public final class DefaultRenameDashboardService implements RenameDashboardUseCase {

    private final DashboardWritePort dashboardPort;
    private final VisualizationWriteAuthorizationPort authorizationPort;

    public DefaultRenameDashboardService(DashboardWritePort dashboardPort,
                                         VisualizationWriteAuthorizationPort authorizationPort) {
        this.dashboardPort = java.util.Objects.requireNonNull(dashboardPort, "dashboardPort");
        this.authorizationPort = java.util.Objects.requireNonNull(authorizationPort, "authorizationPort");
    }

    @Override
    public PreparedRenameDashboard prepare(RenameDashboardCommand command,
                                             VisualizationWriteContext context) {
        VisualizationWriteContext trusted = WriteApplicationSupport.requireContext(context);
        RenameDashboardCommand normalized = WriteApplicationSupport.normalize(command);
        DashboardTarget dashboard = loadAndAuthorize(normalized, trusted);
        if (normalized.newName().equals(dashboard.name())) {
            throw failure(INVALID_RENAME_DASHBOARD_COMMAND);
        }
        ensureNameAvailable(normalized, dashboard, trusted);
        return new PreparedRenameDashboard(normalized, preview(normalized, dashboard),
                WriteApplicationSupport.binding(trusted), dashboard.stateFingerprint());
    }

    @Override
    public BusinessResourceChange execute(PreparedRenameDashboard prepared,
                                          VisualizationWriteContext context) {
        VisualizationWriteContext trusted = WriteApplicationSupport.requireContext(context);
        RenameDashboardCommand command = validatePrepared(prepared, trusted);

        RenameDashboardOutcome outcome = WriteApplicationSupport.portCall(
                () -> dashboardPort.renameDashboard(new RenameDashboardDraft(command.dashboardId(),
                                command.newName()), prepared.stateFingerprint(), trusted),
                RENAME_DASHBOARD_EXECUTION_FAILED);
        if (outcome.status() == RenameDashboardStatus.ACCESS_DENIED) {
            throw failure(RENAME_DASHBOARD_ACCESS_DENIED);
        }
        if (outcome.status() == RenameDashboardStatus.STALE) {
            throw failure(DASHBOARD_STATE_STALE);
        }
        if (outcome.status() == RenameDashboardStatus.CONFLICT) {
            throw failure(DASHBOARD_NAME_CONFLICT);
        }
        RenamedDashboard changed = outcome.dashboard();
        if (!validResult(changed, command, prepared.stateFingerprint())) {
            throw failure(RENAME_DASHBOARD_EXECUTION_FAILED);
        }
        return new BusinessResourceChange(DASHBOARD, changed.id(), changed.currentName(), RENAMED,
                prepared.stateFingerprint(), changed.stateFingerprint());
    }

    private RenameDashboardCommand validatePrepared(PreparedRenameDashboard prepared,
                                                     VisualizationWriteContext context) {
        if (prepared == null || prepared.command() == null
                || !WriteApplicationSupport.bindingMatches(prepared.binding(), context)
                || !WriteApplicationSupport.validFingerprint(prepared.stateFingerprint())) {
            throw failure(INVALID_PREPARED_RENAME_DASHBOARD);
        }
        RenameDashboardCommand normalized;
        try {
            normalized = WriteApplicationSupport.normalize(prepared.command());
        } catch (RuntimeException ignored) {
            throw failure(INVALID_PREPARED_RENAME_DASHBOARD);
        }
        if (!normalized.equals(prepared.command())) {
            throw failure(INVALID_PREPARED_RENAME_DASHBOARD);
        }
        return normalized;
    }

    private DashboardTarget loadAndAuthorize(RenameDashboardCommand command,
                                             VisualizationWriteContext context) {
        Optional<DashboardTarget> dashboardLookup = WriteApplicationSupport.portCall(
                () -> dashboardPort.findDashboard(command.dashboardId()), RENAME_DASHBOARD_EXECUTION_FAILED);
        DashboardTarget dashboard = dashboardLookup.orElseThrow(() -> failure(RENAME_DASHBOARD_ACCESS_DENIED));
        if (!validDashboard(dashboard, command, context)) {
            throw failure(RENAME_DASHBOARD_ACCESS_DENIED);
        }
        boolean allowed = WriteApplicationSupport.portCall(
                () -> authorizationPort.canRenameDashboard(context, dashboard),
                RENAME_DASHBOARD_EXECUTION_FAILED);
        if (!allowed) {
            throw failure(RENAME_DASHBOARD_ACCESS_DENIED);
        }
        return dashboard;
    }

    private void ensureNameAvailable(RenameDashboardCommand command,
                                     DashboardTarget dashboard,
                                     VisualizationWriteContext context) {
        boolean exists = WriteApplicationSupport.portCall(
                () -> dashboardPort.dashboardNameExists(context.organizationId(), dashboard.parentId(),
                        command.newName(), command.dashboardId()), RENAME_DASHBOARD_EXECUTION_FAILED);
        if (exists) {
            throw failure(DASHBOARD_NAME_CONFLICT);
        }
    }

    private boolean validDashboard(DashboardTarget dashboard,
                                   RenameDashboardCommand command,
                                   VisualizationWriteContext context) {
        return dashboard != null && command.dashboardId().equals(dashboard.id())
                && context.organizationId().equals(dashboard.organizationId())
                && WriteApplicationSupport.validName(dashboard.name())
                && (dashboard.parentId() == null || WriteApplicationSupport.validIdentifier(dashboard.parentId()))
                && WriteApplicationSupport.validFingerprint(dashboard.stateFingerprint());
    }

    private boolean validResult(RenamedDashboard changed,
                                RenameDashboardCommand command,
                                String expectedFingerprint) {
        return changed != null
                && command.dashboardId().equals(changed.id())
                && WriteApplicationSupport.validName(changed.previousName())
                && !changed.previousName().equals(changed.currentName())
                && command.newName().equals(changed.currentName())
                && WriteApplicationSupport.validFingerprint(changed.stateFingerprint())
                && !expectedFingerprint.equals(changed.stateFingerprint());
    }

    private WritePreview preview(RenameDashboardCommand command, DashboardTarget dashboard) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        parameters.put("dashboardId", command.dashboardId());
        parameters.put("currentName", dashboard.name());
        parameters.put("newName", command.newName());
        return new WritePreview("重命名仪表盘", "修改一个已有仪表盘的名称", parameters,
                List.of("仅修改仪表盘名称", "不会删除组件、发布、分享或修改权限"));
    }
}

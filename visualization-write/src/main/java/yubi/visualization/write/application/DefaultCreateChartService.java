package yubi.visualization.write.application;

import yubi.visualization.write.api.BusinessResourceChange;
import yubi.visualization.write.api.CreateChartCommand;
import yubi.visualization.write.api.CreateChartUseCase;
import yubi.visualization.write.api.PreparedCreateChart;
import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.api.WritePreview;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartStatus;
import yubi.visualization.write.domain.VisualizationWriteModels.CreatedChart;
import yubi.visualization.write.domain.VisualizationWriteModels.ParentTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ViewTarget;
import yubi.visualization.write.port.ChartWritePort;
import yubi.visualization.write.port.VisualizationWriteAuthorizationPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static yubi.visualization.write.api.VisualizationWriteFailureCode.CHART_NAME_CONFLICT;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.CREATE_CHART_ACCESS_DENIED;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.CREATE_CHART_EXECUTION_FAILED;
import static yubi.visualization.write.api.VisualizationWriteFailureCode.INVALID_PREPARED_CREATE_CHART;
import static yubi.visualization.write.application.WriteApplicationSupport.failure;
import static yubi.visualization.write.domain.VisualizationWriteModels.ChangeType.CREATED;
import static yubi.visualization.write.domain.VisualizationWriteModels.ResourceType.CHART;

public final class DefaultCreateChartService implements CreateChartUseCase {

    private final ChartWritePort chartPort;
    private final VisualizationWriteAuthorizationPort authorizationPort;

    public DefaultCreateChartService(ChartWritePort chartPort,
                                     VisualizationWriteAuthorizationPort authorizationPort) {
        this.chartPort = java.util.Objects.requireNonNull(chartPort, "chartPort");
        this.authorizationPort = java.util.Objects.requireNonNull(authorizationPort, "authorizationPort");
    }

    @Override
    public PreparedCreateChart prepare(CreateChartCommand command, VisualizationWriteContext context) {
        VisualizationWriteContext trusted = WriteApplicationSupport.requireContext(context);
        CreateChartCommand normalized = WriteApplicationSupport.normalize(command);
        authorize(normalized, trusted);
        ensureNameAvailable(normalized, trusted);
        return new PreparedCreateChart(normalized, preview(normalized), WriteApplicationSupport.binding(trusted));
    }

    @Override
    public BusinessResourceChange execute(PreparedCreateChart prepared, VisualizationWriteContext context) {
        VisualizationWriteContext trusted = WriteApplicationSupport.requireContext(context);
        CreateChartCommand command = validatePrepared(prepared, trusted);

        CreateChartOutcome outcome = WriteApplicationSupport.portCall(
                () -> chartPort.createChart(new CreateChartDraft(command.name(), command.viewId(),
                        command.parentId(), command.description()), trusted), CREATE_CHART_EXECUTION_FAILED);
        if (outcome.status() == CreateChartStatus.ACCESS_DENIED) {
            throw failure(CREATE_CHART_ACCESS_DENIED);
        }
        if (outcome.status() == CreateChartStatus.CONFLICT) {
            throw failure(CHART_NAME_CONFLICT);
        }
        CreatedChart chart = outcome.chart();
        if (chart == null || !WriteApplicationSupport.validIdentifier(chart.id())
                || !command.name().equals(chart.name())
                || !WriteApplicationSupport.validFingerprint(chart.stateFingerprint())) {
            throw failure(CREATE_CHART_EXECUTION_FAILED);
        }
        return new BusinessResourceChange(CHART, chart.id(), chart.name(), CREATED,
                null, chart.stateFingerprint());
    }

    private CreateChartCommand validatePrepared(PreparedCreateChart prepared, VisualizationWriteContext context) {
        if (prepared == null || prepared.command() == null
                || !WriteApplicationSupport.bindingMatches(prepared.binding(), context)) {
            throw failure(INVALID_PREPARED_CREATE_CHART);
        }
        CreateChartCommand normalized;
        try {
            normalized = WriteApplicationSupport.normalize(prepared.command());
        } catch (RuntimeException ignored) {
            throw failure(INVALID_PREPARED_CREATE_CHART);
        }
        if (!normalized.equals(prepared.command())) {
            throw failure(INVALID_PREPARED_CREATE_CHART);
        }
        return normalized;
    }

    private void authorize(CreateChartCommand command, VisualizationWriteContext context) {
        Optional<ViewTarget> viewLookup = WriteApplicationSupport.portCall(
                () -> chartPort.findView(command.viewId()), CREATE_CHART_EXECUTION_FAILED);
        ViewTarget view = viewLookup.orElseThrow(() -> failure(CREATE_CHART_ACCESS_DENIED));
        if (!validView(view, command, context)) {
            throw failure(CREATE_CHART_ACCESS_DENIED);
        }

        ParentTarget parent = null;
        if (command.parentId() != null) {
            Optional<ParentTarget> parentLookup = WriteApplicationSupport.portCall(
                    () -> chartPort.findParent(command.parentId()), CREATE_CHART_EXECUTION_FAILED);
            parent = parentLookup.orElseThrow(() -> failure(CREATE_CHART_ACCESS_DENIED));
            if (!validParent(parent, command, context)) {
                throw failure(CREATE_CHART_ACCESS_DENIED);
            }
        }
        ParentTarget authorizedParent = parent;
        boolean allowed = WriteApplicationSupport.portCall(
                () -> authorizationPort.canCreateChart(context, view, authorizedParent),
                CREATE_CHART_EXECUTION_FAILED);
        if (!allowed) {
            throw failure(CREATE_CHART_ACCESS_DENIED);
        }
    }

    private void ensureNameAvailable(CreateChartCommand command, VisualizationWriteContext context) {
        boolean exists = WriteApplicationSupport.portCall(
                () -> chartPort.chartNameExists(context.organizationId(), command.parentId(), command.name()),
                CREATE_CHART_EXECUTION_FAILED);
        if (exists) {
            throw failure(CHART_NAME_CONFLICT);
        }
    }

    private boolean validView(ViewTarget view, CreateChartCommand command, VisualizationWriteContext context) {
        return view != null && command.viewId().equals(view.id())
                && context.organizationId().equals(view.organizationId());
    }

    private boolean validParent(ParentTarget parent, CreateChartCommand command,
                                VisualizationWriteContext context) {
        return parent != null && command.parentId().equals(parent.id())
                && context.organizationId().equals(parent.organizationId());
    }

    private WritePreview preview(CreateChartCommand command) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        parameters.put("name", command.name());
        parameters.put("viewId", command.viewId());
        if (command.parentId() != null) {
            parameters.put("parentId", command.parentId());
        }
        if (command.description() != null) {
            parameters.put("description", command.description());
        }
        return new WritePreview("创建图表", "创建一个基于已有授权 View 的未发布图表", parameters,
                List.of("创建一个图表资源", "不会发布、分享或修改权限"));
    }
}

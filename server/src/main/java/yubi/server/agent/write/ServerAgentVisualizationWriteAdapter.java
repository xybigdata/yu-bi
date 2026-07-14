package yubi.server.agent.write;

import yubi.agent.domain.ControlledWriteModels.CreateChartAction;
import yubi.agent.domain.ControlledWriteModels.PreparedWrite;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.RenameDashboardAction;
import yubi.agent.domain.ControlledWriteModels.TrustedWriteContext;
import yubi.agent.domain.ControlledWriteModels.WriteAction;
import yubi.agent.domain.ControlledWriteModels.WriteExecutionResult;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.port.VisualizationWritePort;
import yubi.visualization.write.api.CreateChartCommand;
import yubi.visualization.write.api.CreateChartUseCase;
import yubi.visualization.write.api.PreparedCreateChart;
import yubi.visualization.write.api.PreparedRenameDashboard;
import yubi.visualization.write.api.PreparedWriteBinding;
import yubi.visualization.write.api.RenameDashboardCommand;
import yubi.visualization.write.api.RenameDashboardUseCase;
import yubi.visualization.write.api.VisualizationWriteContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

public final class ServerAgentVisualizationWriteAdapter implements VisualizationWritePort {

    private final CreateChartUseCase createChart;
    private final RenameDashboardUseCase renameDashboard;

    public ServerAgentVisualizationWriteAdapter(CreateChartUseCase createChart,
                                                RenameDashboardUseCase renameDashboard) {
        this.createChart = createChart;
        this.renameDashboard = renameDashboard;
    }

    @Override
    public PreparedWrite prepare(WriteAction action,
                                 WritePreview safePreview,
                                 TrustedWriteContext context) {
        VisualizationWriteContext trusted = context(context);
        if (action instanceof CreateChartAction create) {
            var prepared = createChart.prepare(command(create), trusted);
            return new PreparedWrite(action, digest(prepared.command().name(), prepared.command().viewId(),
                    prepared.command().parentId(), prepared.command().description()),
                    enriched(safePreview, prepared.preview(), null));
        }
        RenameDashboardAction rename = (RenameDashboardAction) action;
        var prepared = renameDashboard.prepare(command(rename), trusted);
        return new PreparedWrite(action, prepared.stateFingerprint(),
                enriched(safePreview, prepared.preview(),
                        prepared.preview().safeParameters().get("currentName")));
    }

    @Override
    public WriteExecutionResult execute(PreparedWrite operation, TrustedWriteContext context) {
        VisualizationWriteContext trusted = context(context);
        if (operation.action() instanceof CreateChartAction create) {
            // action 与 prepared 摘要已由 Agent 状态机校验；执行期业务读取只能在原子端口拿锁后发生。
            PreparedCreateChart prepared = new PreparedCreateChart(command(create),
                    preview(operation.preview()), binding(context));
            return new WriteExecutionResult(createChart.execute(prepared, trusted).resourceId());
        }
        RenameDashboardAction rename = (RenameDashboardAction) operation.action();
        PreparedRenameDashboard prepared = new PreparedRenameDashboard(command(rename),
                preview(operation.preview()), binding(context), operation.expectedStateDigest());
        return new WriteExecutionResult(renameDashboard.execute(prepared, trusted).resourceId());
    }

    private CreateChartCommand command(CreateChartAction action) {
        return new CreateChartCommand(action.name(), action.viewId(), action.parentId(), action.description());
    }

    private RenameDashboardCommand command(RenameDashboardAction action) {
        return new RenameDashboardCommand(action.dashboardId(), action.newName());
    }

    private VisualizationWriteContext context(TrustedWriteContext context) {
        return new VisualizationWriteContext(context.subjectId(), context.organizationId(), context.sessionId(),
                context.requestId(), context.correlationId());
    }

    private PreparedWriteBinding binding(TrustedWriteContext context) {
        return new PreparedWriteBinding(context.subjectId(), context.organizationId(), context.sessionId(),
                context.requestId(), context.correlationId());
    }

    private yubi.visualization.write.api.WritePreview preview(WritePreview preview) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        preview.fields().forEach(field -> parameters.put(field.label(), field.value()));
        return new yubi.visualization.write.api.WritePreview(preview.title(), preview.title(),
                parameters, preview.impacts());
    }

    private WritePreview enriched(WritePreview safePreview,
                                  yubi.visualization.write.api.WritePreview businessPreview,
                                  String currentName) {
        if (businessPreview == null) {
            throw new IllegalStateException("受控业务预览无效");
        }
        List<PreviewField> fields = new ArrayList<>(safePreview.fields());
        if (currentName != null) {
            if (currentName.isBlank() || currentName.length() > 255) {
                throw new IllegalStateException("受控业务预览无效");
            }
            fields.add(new PreviewField("当前名称", currentName));
        }
        List<String> impacts = new ArrayList<>(safePreview.impacts());
        for (String impact : businessPreview.impacts()) {
            if (impact != null && !impact.isBlank() && !impacts.contains(impact)) {
                impacts.add(impact);
            }
        }
        return new WritePreview(safePreview.title(), safePreview.resourceType(), safePreview.targetId(),
                List.copyOf(fields), List.copyOf(impacts));
    }

    private String digest(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object value : values) {
                byte[] bytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用");
        }
    }
}

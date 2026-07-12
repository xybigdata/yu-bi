package yubi.agent.application;

import yubi.agent.api.WriteToolSchema;
import yubi.agent.api.WriteToolSchemas;
import yubi.agent.domain.ControlledWriteModels.NormalizedWriteProposal;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.RenameDashboardAction;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.port.WriteProposalTool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static yubi.agent.domain.StructuredValue.text;

public final class RenameDashboardWriteProposalTool implements WriteProposalTool {

    private static final Set<String> FIELDS = Set.of("dashboardId", "newName");

    @Override
    public WriteToolSchema schema() {
        return WriteToolSchemas.renameDashboard();
    }

    @Override
    public NormalizedWriteProposal normalize(ObjectValue arguments) {
        ToolArgumentReader reader = new ToolArgumentReader(arguments).exact(FIELDS);
        String dashboardId = WriteArgumentRules.identifier(reader.requiredText("dashboardId", 128));
        String newName = WriteArgumentRules.normalizedName(reader.requiredText("newName", 255));
        RenameDashboardAction action = new RenameDashboardAction(dashboardId, newName);

        LinkedHashMap<String, yubi.agent.domain.StructuredValue> values = new LinkedHashMap<>();
        values.put("dashboardId", text(dashboardId));
        values.put("newName", text(newName));
        WritePreview preview = new WritePreview("重命名仪表盘", "DASHBOARD", dashboardId,
                List.of(new PreviewField("仪表盘", dashboardId),
                        new PreviewField("新名称", newName)),
                List.of("同步更新仪表盘及其导航名称"));
        return new NormalizedWriteProposal(action, new ObjectValue(values), preview);
    }
}

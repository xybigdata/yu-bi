package yubi.agent.application;

import yubi.agent.api.WriteToolSchema;
import yubi.agent.api.WriteToolSchemas;
import yubi.agent.domain.ControlledWriteModels.CreateChartAction;
import yubi.agent.domain.ControlledWriteModels.NormalizedWriteProposal;
import yubi.agent.domain.ControlledWriteModels.PreviewField;
import yubi.agent.domain.ControlledWriteModels.WritePreview;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.agent.port.WriteProposalTool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static yubi.agent.domain.StructuredValue.text;

public final class CreateChartWriteProposalTool implements WriteProposalTool {

    private static final Set<String> FIELDS = Set.of("name", "viewId", "parentId", "description");

    @Override
    public WriteToolSchema schema() {
        return WriteToolSchemas.createChart();
    }

    @Override
    public NormalizedWriteProposal normalize(ObjectValue arguments) {
        ToolArgumentReader reader = new ToolArgumentReader(arguments).exact(FIELDS);
        String name = WriteArgumentRules.normalizedName(reader.requiredText("name", 255));
        String viewId = WriteArgumentRules.identifier(reader.requiredText("viewId", 128));
        String parentId = WriteArgumentRules.optionalIdentifier(reader.optionalText("parentId", 128));
        String description = WriteArgumentRules.optionalDescription(reader.optionalText("description", 255));

        CreateChartAction action = new CreateChartAction(name, viewId, parentId, description);
        LinkedHashMap<String, yubi.agent.domain.StructuredValue> values = new LinkedHashMap<>();
        values.put("name", text(name));
        values.put("viewId", text(viewId));
        if (parentId != null) {
            values.put("parentId", text(parentId));
        }
        if (description != null) {
            values.put("description", text(description));
        }

        List<PreviewField> previewFields = new ArrayList<>();
        previewFields.add(new PreviewField("图表名称", name));
        previewFields.add(new PreviewField("数据视图", viewId));
        if (parentId != null) {
            previewFields.add(new PreviewField("目标文件夹", parentId));
        }
        if (description != null) {
            previewFields.add(new PreviewField("图表说明", description));
        }
        WritePreview preview = new WritePreview("创建图表", "CHART", viewId,
                previewFields, List.of("创建一个未发布图表草稿"));
        return new NormalizedWriteProposal(action, new ObjectValue(values), preview);
    }
}

package yubi.agent.application;

import yubi.agent.api.WriteToolSchema;
import yubi.agent.api.WriteToolSchemas;
import yubi.agent.port.WriteProposalTool;
import yubi.agent.port.WriteProposalToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultWriteProposalToolRegistry implements WriteProposalToolRegistry {

    public static final List<String> TOOL_NAMES = List.of("create_chart", "rename_dashboard");

    private final Map<String, WriteProposalTool> tools;
    private final List<WriteToolSchema> schemas;

    public DefaultWriteProposalToolRegistry(List<WriteProposalTool> tools) {
        Map<String, WriteProposalTool> byName = new LinkedHashMap<>();
        if (tools != null) {
            for (WriteProposalTool tool : tools) {
                if (tool == null || tool.schema() == null
                        || tool.schema().effect() != WriteToolSchema.Effect.WRITE
                        || !tool.schema().requiresApproval()
                        || !TOOL_NAMES.contains(tool.schema().name())
                        || !expectedSchema(tool.schema().name()).equals(tool.schema())
                        || !expectedType(tool.schema().name()).equals(tool.getClass())
                        || byName.putIfAbsent(tool.schema().name(), tool) != null) {
                    throw new IllegalArgumentException("受控写工具注册表包含非法工具");
                }
            }
        }
        if (!byName.keySet().equals(new java.util.LinkedHashSet<>(TOOL_NAMES))) {
            throw new IllegalArgumentException("受控写工具注册表必须且只能包含首批两项工具");
        }
        LinkedHashMap<String, WriteProposalTool> ordered = new LinkedHashMap<>();
        TOOL_NAMES.forEach(name -> ordered.put(name, byName.get(name)));
        this.tools = java.util.Collections.unmodifiableMap(ordered);
        this.schemas = TOOL_NAMES.stream().map(name -> this.tools.get(name).schema()).toList();
    }

    @Override
    public List<WriteToolSchema> schemas() {
        return schemas;
    }

    @Override
    public Optional<WriteProposalTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    private WriteToolSchema expectedSchema(String name) {
        return "create_chart".equals(name)
                ? WriteToolSchemas.createChart() : WriteToolSchemas.renameDashboard();
    }

    private Class<? extends WriteProposalTool> expectedType(String name) {
        return "create_chart".equals(name)
                ? CreateChartWriteProposalTool.class : RenameDashboardWriteProposalTool.class;
    }
}

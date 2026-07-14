package yubi.agent.application;

import yubi.agent.port.ReadOnlyTool;
import yubi.agent.port.ReadOnlyToolRegistry;
import yubi.query.api.MetadataToolSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultReadOnlyToolRegistry implements ReadOnlyToolRegistry {

    public static final List<String> TOOL_NAMES = List.of(
            "search_data_assets", "describe_data_asset", "execute_view");

    private final Map<String, ReadOnlyTool> tools;
    private final List<MetadataToolSchema> schemas;

    public DefaultReadOnlyToolRegistry(List<ReadOnlyTool> tools) {
        Map<String, ReadOnlyTool> byName = new LinkedHashMap<>();
        if (tools != null) {
            for (ReadOnlyTool tool : tools) {
                if (tool == null || tool.schema() == null || !tool.schema().readOnly()
                        || !TOOL_NAMES.contains(tool.schema().name())
                        || byName.putIfAbsent(tool.schema().name(), tool) != null) {
                    throw new IllegalArgumentException("只读工具注册表包含非法工具");
                }
            }
        }
        if (!byName.keySet().equals(new java.util.LinkedHashSet<>(TOOL_NAMES))) {
            throw new IllegalArgumentException("只读工具注册表必须且只能包含 V1 三项工具");
        }
        LinkedHashMap<String, ReadOnlyTool> ordered = new LinkedHashMap<>();
        TOOL_NAMES.forEach(name -> ordered.put(name, byName.get(name)));
        this.tools = java.util.Collections.unmodifiableMap(ordered);
        this.schemas = TOOL_NAMES.stream().map(name -> this.tools.get(name).schema()).toList();
    }

    @Override
    public List<MetadataToolSchema> schemas() {
        return schemas;
    }

    @Override
    public Optional<ReadOnlyTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }
}

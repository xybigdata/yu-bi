package yubi.server.query;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.entity.Source;
import yubi.core.entity.View;
import yubi.core.mappers.ext.SourceMapperExt;
import yubi.query.domain.QueryModels.ColumnMetadata;
import yubi.query.domain.QueryModels.Definition;
import yubi.query.domain.QueryModels.ScriptType;
import yubi.query.domain.QueryModels.SourceDefinition;
import yubi.query.domain.QueryModels.ValueType;
import yubi.query.port.QueryDefinitionPort;
import yubi.server.service.ViewService;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class ServerQueryDefinitionAdapter implements QueryDefinitionPort {

    private final ViewService viewService;
    private final SourceMapperExt sourceMapper;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public ServerQueryDefinitionAdapter(ViewService viewService,
                                        SourceMapperExt sourceMapper) {
        this.viewService = viewService;
        this.sourceMapper = sourceMapper;
    }

    @Override
    public Definition load(String viewId) {
        View view = viewService.retrieve(viewId, false);
        ScriptType scriptType = view.getType() == null ? ScriptType.SQL : ScriptType.valueOf(view.getType());
        return new Definition(view.getId(), view.getOrgId(), view.getName(), view.getParentId(),
                view.getSourceId(), view.getScript(), scriptType, parseSchema(view.getModel()));
    }

    @Override
    public SourceDefinition loadSource(String sourceId) {
        Source source = sourceMapper.selectQueryAccessProjection(sourceId);
        if (source == null) {
            return null;
        }
        return new SourceDefinition(source.getId(), source.getOrgId(), source.getName(), source.getParentId());
    }

    private Map<String, ColumnMetadata> parseSchema(String model) {
        Map<String, ColumnMetadata> schema = new LinkedHashMap<>();
        if (StringUtils.isBlank(model)) {
            return schema;
        }
        try {
            JsonNode root = objectMapper.readTree(model);
            JsonNode columns = root.get("columns");
            if (columns != null && columns.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> iterator = columns.properties().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    String[] names = parseColumnNames(entry.getValue(), entry.getKey());
                    ColumnMetadata column = column(names, entry.getValue().path("type").asString());
                    schema.put(column.key(), column);
                }
            } else if (root.has("hierarchy") && root.get("hierarchy").isObject()) {
                Iterator<Map.Entry<String, JsonNode>> iterator = root.get("hierarchy").properties().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    JsonNode children = entry.getValue().get("children");
                    if (children != null && children.isArray() && !children.isEmpty()) {
                        for (JsonNode child : children) {
                            String name = child.path("name").asString();
                            schema.put(name, column(name.split("\\."), child.path("type").asString()));
                        }
                    } else {
                        schema.put(entry.getKey(), column(entry.getKey().split("\\."),
                                entry.getValue().path("type").asString()));
                    }
                }
            } else {
                Iterator<Map.Entry<String, JsonNode>> iterator = root.properties().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    schema.put(entry.getKey(), column(new String[]{entry.getKey()},
                            entry.getValue().path("type").asString()));
                }
            }
        } catch (Exception ex) {
            log.error("View model projection failed");
        }
        return schema;
    }

    private ColumnMetadata column(String[] names, String type) {
        return new ColumnMetadata(java.util.List.of(names), ValueType.valueOf(type), null, null);
    }

    private String[] parseColumnNames(JsonNode item, String fallbackName) {
        JsonNode nameNode = item.get("name");
        if (nameNode == null || nameNode.isNull()) {
            return new String[]{fallbackName};
        }
        if (nameNode.isArray()) {
            if (nameNode.size() == 1 && nameNode.get(0).isString()) {
                String value = nameNode.get(0).asString();
                try {
                    JsonNode nested = objectMapper.readTree(value);
                    if (nested.isArray()) {
                        return objectMapper.convertValue(nested, String[].class);
                    }
                } catch (JacksonException ignored) {
                    // 历史数据既可能保存 JSON 数组字符串，也可能保存普通列名。
                }
                return new String[]{value};
            }
            return objectMapper.convertValue(nameNode, String[].class);
        }
        return new String[]{nameNode.asString(fallbackName)};
    }
}

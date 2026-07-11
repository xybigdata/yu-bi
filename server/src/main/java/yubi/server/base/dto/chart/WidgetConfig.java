package yubi.server.base.dto.chart;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import lombok.Data;

@Data
public class WidgetConfig {

    private JsonNode content = JsonNodeFactory.instance.objectNode();

    public String getChartConfig(){
        if (content != null && content.has("dataChart")){
            JsonNode configNode = content.path("dataChart").path("config");
            if (!configNode.isMissingNode() && !configNode.isNull()){
                return configNode.asString("");
            }
        }
        return "";
    }
}

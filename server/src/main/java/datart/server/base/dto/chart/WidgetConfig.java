package datart.server.base.dto.chart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Data;

@Data
public class WidgetConfig {

    private JsonNode content = JsonNodeFactory.instance.objectNode();

    public String getChartConfig(){
        if (content != null && content.has("dataChart")){
            JsonNode configNode = content.path("dataChart").path("config");
            if (!configNode.isMissingNode() && !configNode.isNull()){
                return configNode.asText("");
            }
        }
        return "";
    }
}

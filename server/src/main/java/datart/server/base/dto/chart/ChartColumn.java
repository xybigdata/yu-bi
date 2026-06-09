package datart.server.base.dto.chart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import datart.core.entity.poi.format.CurrencyFormat;
import datart.core.entity.poi.format.NumericFormat;
import datart.core.entity.poi.format.PercentageFormat;
import datart.core.entity.poi.format.PoiNumFormat;
import datart.core.entity.poi.format.ScientificNotationFormat;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChartColumn {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String uid = "";

    private String colName = "";

    private String category = "";

    private String type = "";

    private String label = "";

    private ChartColumn.Alias alias = new ChartColumn.Alias();

    private String desc = "";

    private boolean isGroup;

    private String aggregate = "";

    private List<ChartColumn> children = new ArrayList<>();

    private JsonNode format;

    private int leafNum = 0;

    private int deepNum = 1;

    public void setChildren(List<ChartColumn> children) {
        this.children = children;
        this.leafNum = calLeafNum();
        this.deepNum = calDeepNum();
    }

    public String getDisplayName() {
        return isGroup ? label :
                StringUtils.isNotBlank(aggregate) ? aggregate+"("+colName+")" : colName;
    }

    public PoiNumFormat getNumFormat(){
        PoiNumFormat numFormat = new PoiNumFormat();
        if (format == null || !format.hasNonNull("type")){
            return numFormat;
        }
        String type = format.path("type").asText();
        JsonNode formatNode = format.get(type);
        if (formatNode == null || formatNode.isNull()) {
            return numFormat;
        }
        switch (type) {
            case NumericFormat.type:
                numFormat = OBJECT_MAPPER.convertValue(formatNode, NumericFormat.class);
                break;
            case CurrencyFormat.type:
                numFormat = OBJECT_MAPPER.convertValue(formatNode, CurrencyFormat.class);
                break;
            case PercentageFormat.type:
                numFormat = OBJECT_MAPPER.convertValue(formatNode, PercentageFormat.class);
                break;
            case ScientificNotationFormat.type:
                numFormat = OBJECT_MAPPER.convertValue(formatNode, ScientificNotationFormat.class);
                break;
            default:
                break;
        }
        return numFormat;
    }

    public List<ChartColumn> getLeafNodes(){
        List<ChartColumn> leafNodes = new ArrayList<>();
        if (this.leafNum == 0 && !this.isGroup){
            leafNodes.add(this);
        }
        for (ChartColumn child : children) {
            leafNodes.addAll(child.getLeafNodes());
        }
        return leafNodes;
    }

    private int calLeafNum() {
        int num = 0;
        for (ChartColumn child : children) {
            if (child.getLeafNum()==0) {
                num++;
            } else {
                num += child.getLeafNum();
            }
        }
        return num;
    }

    private int calDeepNum() {
        int num = 0;
        for (ChartColumn child : children) {
            if (child.getDeepNum()>num){
                num = child.getDeepNum();
            }
        }
        return num+1;
    }

    @Data
    public class Alias {
        private String name;
    }

}

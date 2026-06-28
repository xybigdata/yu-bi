package yubi.server.base.dto;

import yubi.core.entity.Datachart;
import yubi.core.entity.Variable;
import yubi.core.entity.View;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DatachartDetailList {

    private List<Datachart> datacharts;

    private List<View> views;

    private Map<String, List<Variable>> viewVariables;

    private List<Variable> orgVariables;

}

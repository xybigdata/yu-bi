package yubi.server.base.dto;

import yubi.core.entity.Datachart;
import yubi.core.entity.Variable;
import yubi.core.entity.View;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatachartDetail extends Datachart {

    private String parentId;

    private Double index;

    private View view;

    private List<Variable> queryVariables;

    private boolean download;

}

package yubi.server.base.dto;

import yubi.core.entity.Dashboard;
import yubi.core.entity.Datachart;
import yubi.core.entity.Variable;
import yubi.core.entity.View;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DashboardDetail extends Dashboard {

    private String parentId;

    private Double index;

    private String config;

    private List<WidgetDetail> widgets;

    private List<View> views;

    private List<Datachart> datacharts;

    private List<Variable> queryVariables;

    private boolean download;
}
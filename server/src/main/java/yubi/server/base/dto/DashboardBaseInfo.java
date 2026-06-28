package yubi.server.base.dto;

import yubi.core.entity.Dashboard;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class DashboardBaseInfo {

    private String id;

    private String name;

    private String portalId;

    private String parentId;

    private Boolean isFolder;

    private Double index;

    public DashboardBaseInfo(Dashboard dashboard) {
        BeanUtils.copyProperties(dashboard, this);
    }

    public DashboardBaseInfo() {
    }
}
package yubi.server.service;

import yubi.core.entity.Dashboard;
import yubi.core.entity.Folder;
import yubi.core.mappers.ext.DashboardMapperExt;
import yubi.server.base.dto.DashboardBaseInfo;
import yubi.server.base.dto.DashboardDetail;
import yubi.server.base.params.DashboardCreateParam;
import yubi.server.base.transfer.model.DashboardResourceModel;
import yubi.server.base.transfer.model.DashboardTemplateModel;

import java.io.IOException;
import java.util.List;

public interface DashboardService extends VizCRUDService<Dashboard, DashboardMapperExt>, ResourceTransferService<Folder, DashboardResourceModel, DashboardTemplateModel,Folder> {

    List<DashboardBaseInfo> listDashboard(String orgId);

    Folder createWithFolder(DashboardCreateParam createParam);

    DashboardDetail getDashboardDetail(String dashboardId);

    Folder copyDashboard(DashboardCreateParam dashboard) throws IOException;

}
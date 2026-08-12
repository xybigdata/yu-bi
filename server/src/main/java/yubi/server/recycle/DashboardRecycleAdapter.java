package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.core.entity.Dashboard;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.security.base.ResourceType;
import yubi.server.service.DashboardService;
import yubi.server.service.FolderService;
import yubi.server.service.VizService;

@Component
class DashboardRecycleAdapter extends VizRecycleAdapter<Dashboard> {

    DashboardRecycleAdapter(DashboardService service,
                            VizService vizService,
                            FolderService folderService,
                            FolderMapperExt folderMapper,
                            ShareMapperExt shareMapper,
                            RecycleDependencyResolver dependencyResolver) {
        super(RecycleResourceType.DASHBOARD, ResourceType.DASHBOARD,
                service, vizService, folderService, folderMapper, shareMapper,
                Dashboard::getName, Dashboard::getOrgId, dependencyResolver);
    }
}

package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.core.entity.Datachart;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.security.base.ResourceType;
import yubi.server.service.DatachartService;
import yubi.server.service.FolderService;
import yubi.server.service.VizService;

@Component
class DatachartRecycleAdapter extends VizRecycleAdapter<Datachart> {

    DatachartRecycleAdapter(DatachartService service,
                            VizService vizService,
                            FolderService folderService,
                            FolderMapperExt folderMapper,
                            ShareMapperExt shareMapper,
                            RecycleDependencyResolver dependencyResolver) {
        super(RecycleResourceType.DATACHART, ResourceType.DATACHART,
                service, vizService, folderService, folderMapper, shareMapper,
                Datachart::getName, Datachart::getOrgId, dependencyResolver);
    }
}

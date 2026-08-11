package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.core.entity.View;
import yubi.core.mappers.ext.ViewMapperExt;
import yubi.server.service.ViewService;

import java.util.List;

@Component
final class ViewRecycleAdapter extends TreeCrudRecycleAdapter<View> {

    private final ViewService service;
    private final ViewMapperExt mapper;

    ViewRecycleAdapter(ViewService service,
                       ViewMapperExt mapper,
                       RecycleDependencyResolver dependencyResolver) {
        super(RecycleResourceType.VIEW, service, View::getName, View::getOrgId,
                View::getParentId, View::getIndex, View::getIsFolder,
                dependencyResolver);
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected List<View> listActive(String organizationId) {
        return mapper.listByOrgId(organizationId);
    }

    @Override
    protected void unarchive(RecycleNodeSnapshot node) {
        service.unarchive(node.id(), node.name(), node.parentId(), node.index());
    }
}

package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.core.entity.Source;
import yubi.core.mappers.ext.SourceMapperExt;
import yubi.server.service.SourceService;

import java.util.List;

@Component
final class SourceRecycleAdapter extends TreeCrudRecycleAdapter<Source> {

    private final SourceService service;
    private final SourceMapperExt mapper;

    SourceRecycleAdapter(SourceService service,
                         SourceMapperExt mapper,
                         RecycleDependencyResolver dependencyResolver) {
        super(RecycleResourceType.SOURCE, service, Source::getName, Source::getOrgId,
                Source::getParentId, Source::getIndex, Source::getIsFolder,
                dependencyResolver);
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected List<Source> listActive(String organizationId) {
        return mapper.listByOrg(organizationId, true);
    }

    @Override
    protected void unarchive(RecycleNodeSnapshot node) {
        service.unarchive(node.id(), node.name(), node.parentId(), node.index());
    }
}

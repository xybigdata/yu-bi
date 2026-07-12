package yubi.server.query;

import org.springframework.stereotype.Component;
import yubi.core.entity.View;
import yubi.core.mappers.ext.ViewMapperExt;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryMetadataModels.AssetReference;
import yubi.query.port.QueryMetadataCatalogPort;
import yubi.server.service.ViewService;

import java.util.List;

@Component
public class ServerQueryMetadataCatalogAdapter implements QueryMetadataCatalogPort {

    private final ViewService viewService;
    private final ViewMapperExt viewMapper;

    public ServerQueryMetadataCatalogAdapter(ViewService viewService, ViewMapperExt viewMapper) {
        this.viewService = viewService;
        this.viewMapper = viewMapper;
    }

    @Override
    public List<AssetReference> listReadable(QueryExecutionContext context) {
        return viewService.getViews(context.organizationId()).stream()
                .filter(view -> !Boolean.TRUE.equals(view.getIsFolder()))
                .filter(view -> view.getSourceId() != null && !view.getSourceId().isBlank())
                .map(this::project)
                .toList();
    }

    @Override
    public AssetReference find(String assetId) {
        View view = viewMapper.selectMetadataProjection(assetId);
        return view == null || Boolean.TRUE.equals(view.getIsFolder()) || view.getSourceId() == null
                ? null : project(view);
    }

    private AssetReference project(View view) {
        return new AssetReference(view.getId(), view.getOrgId(), view.getName(), view.getDescription(),
                view.getParentId(), view.getSourceId());
    }
}

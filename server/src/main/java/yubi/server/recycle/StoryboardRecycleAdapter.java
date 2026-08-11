package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.core.entity.Share;
import yubi.core.entity.Storyboard;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.StoryboardMapperExt;
import yubi.server.service.StoryboardService;

import java.time.Clock;
import java.util.Date;
import java.util.List;

@Component
final class StoryboardRecycleAdapter extends TreeCrudRecycleAdapter<Storyboard> {

    private final StoryboardService service;
    private final StoryboardMapperExt mapper;
    private final ShareMapperExt shareMapper;
    private final Clock clock;

    StoryboardRecycleAdapter(StoryboardService service,
                             StoryboardMapperExt mapper,
                             ShareMapperExt shareMapper,
                             RecycleDependencyResolver dependencyResolver) {
        super(RecycleResourceType.STORYBOARD, service, Storyboard::getName, Storyboard::getOrgId,
                Storyboard::getParentId, Storyboard::getIndex, Storyboard::getIsFolder,
                dependencyResolver);
        this.service = service;
        this.mapper = mapper;
        this.shareMapper = shareMapper;
        this.clock = Clock.systemDefaultZone();
    }

    @Override
    protected List<Storyboard> listActive(String organizationId) {
        return mapper.selectByOrg(organizationId);
    }

    @Override
    protected RecycleItemPreflight extraPreflight(Storyboard storyboard) {
        if (!Boolean.TRUE.equals(storyboard.getIsFolder()) && hasActiveShare(storyboard.getId())) {
            return new RecycleItemPreflight(
                    storyboard.getId(), RecycleItemStatus.BLOCKED,
                    "存在有效分享链接，请先取消分享", List.of());
        }
        return RecycleItemPreflight.ready(storyboard.getId());
    }

    @Override
    protected void unarchive(RecycleNodeSnapshot node) {
        service.unarchive(node.id(), node.name(), node.parentId(), node.index());
    }

    private boolean hasActiveShare(String vizId) {
        Date now = Date.from(clock.instant());
        return shareMapper.selectByViz(vizId).stream()
                .map(Share::getExpiryDate)
                .anyMatch(expiry -> expiry == null || expiry.after(now));
    }
}

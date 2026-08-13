package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.core.entity.Schedule;
import yubi.core.mappers.ext.ScheduleMapperExt;
import yubi.server.service.ScheduleService;

import java.util.List;

@Component
final class ScheduleRecycleAdapter extends TreeCrudRecycleAdapter<Schedule> {

    private final ScheduleService service;
    private final ScheduleMapperExt mapper;

    ScheduleRecycleAdapter(ScheduleService service,
                           ScheduleMapperExt mapper,
                           RecycleDependencyResolver dependencyResolver) {
        super(RecycleResourceType.SCHEDULE, service, Schedule::getName, Schedule::getOrgId,
                Schedule::getParentId, Schedule::getIndex, Schedule::getIsFolder,
                dependencyResolver);
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    protected List<Schedule> listActive(String organizationId) {
        return mapper.selectByOrg(organizationId);
    }

    @Override
    protected RecycleItemPreflight extraPreflight(Schedule schedule) {
        if (Boolean.TRUE.equals(schedule.getActive())) {
            return new RecycleItemPreflight(
                    schedule.getId(), RecycleItemStatus.REQUIRES_STOP,
                    "定时任务正在运行，请先停止", List.of());
        }
        return RecycleItemPreflight.ready(schedule.getId());
    }

    @Override
    protected void unarchive(RecycleNodeSnapshot node) {
        service.unarchive(node.id(), node.name(), node.parentId(), node.index());
    }
}

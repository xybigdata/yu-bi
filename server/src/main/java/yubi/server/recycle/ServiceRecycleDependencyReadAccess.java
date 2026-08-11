package yubi.server.recycle;

import org.springframework.stereotype.Component;
import yubi.server.service.DashboardService;
import yubi.server.service.DatachartService;
import yubi.server.service.ScheduleService;
import yubi.server.service.SourceService;
import yubi.server.service.StoryboardService;
import yubi.server.service.ViewService;

@Component
final class ServiceRecycleDependencyReadAccess implements RecycleDependencyReadAccess {

    private final SourceService sourceService;
    private final ViewService viewService;
    private final ScheduleService scheduleService;
    private final DatachartService datachartService;
    private final DashboardService dashboardService;
    private final StoryboardService storyboardService;

    ServiceRecycleDependencyReadAccess(SourceService sourceService,
                                       ViewService viewService,
                                       ScheduleService scheduleService,
                                       DatachartService datachartService,
                                       DashboardService dashboardService,
                                       StoryboardService storyboardService) {
        this.sourceService = sourceService;
        this.viewService = viewService;
        this.scheduleService = scheduleService;
        this.datachartService = datachartService;
        this.dashboardService = dashboardService;
        this.storyboardService = storyboardService;
    }

    @Override
    public boolean canRead(RecycleAccess access,
                           RecycleResourceType resourceType,
                           String resourceId) {
        try {
            return switch (resourceType) {
                case SOURCE -> access.organizationId().equals(
                        sourceService.retrieve(resourceId).getOrgId());
                case VIEW -> access.organizationId().equals(
                        viewService.retrieve(resourceId).getOrgId());
                case SCHEDULE -> access.organizationId().equals(
                        scheduleService.retrieve(resourceId).getOrgId());
                case DATACHART -> access.organizationId().equals(
                        datachartService.retrieve(resourceId).getOrgId());
                case DASHBOARD -> access.organizationId().equals(
                        dashboardService.retrieve(resourceId).getOrgId());
                case STORYBOARD -> access.organizationId().equals(
                        storyboardService.retrieve(resourceId).getOrgId());
            };
        } catch (RuntimeException exception) {
            return false;
        }
    }
}

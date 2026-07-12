package yubi.visualization.write.domain;

import java.util.Objects;

public final class VisualizationWriteModels {

    private VisualizationWriteModels() {
    }

    public enum ResourceType { CHART, DASHBOARD }

    public enum ChangeType { CREATED, RENAMED }

    public record ViewTarget(String id, String organizationId) {
    }

    public record ParentTarget(String id, String organizationId) {
    }

    public record DashboardTarget(String id,
                                  String organizationId,
                                  String name,
                                  String parentId,
                                  String stateFingerprint) {
    }

    public record CreateChartDraft(String name,
                                   String viewId,
                                   String parentId,
                                   String description) {
    }

    public record RenameDashboardDraft(String dashboardId, String newName) {
    }

    public record CreatedChart(String id, String name, String stateFingerprint) {
    }

    public enum CreateChartStatus { CREATED, CONFLICT, ACCESS_DENIED }

    public record CreateChartOutcome(CreateChartStatus status, CreatedChart chart) {
        public CreateChartOutcome {
            Objects.requireNonNull(status, "status");
            if ((status == CreateChartStatus.CREATED) != (chart != null)) {
                throw new IllegalArgumentException("创建图表结果与状态不一致");
            }
        }

        public static CreateChartOutcome created(CreatedChart chart) {
            return new CreateChartOutcome(CreateChartStatus.CREATED, Objects.requireNonNull(chart, "chart"));
        }

        public static CreateChartOutcome conflict() {
            return new CreateChartOutcome(CreateChartStatus.CONFLICT, null);
        }

        public static CreateChartOutcome accessDenied() {
            return new CreateChartOutcome(CreateChartStatus.ACCESS_DENIED, null);
        }
    }

    public record RenamedDashboard(String id,
                                   String previousName,
                                   String currentName,
                                   String stateFingerprint) {
    }

    public enum RenameDashboardStatus { RENAMED, CONFLICT, STALE, ACCESS_DENIED }

    public record RenameDashboardOutcome(RenameDashboardStatus status, RenamedDashboard dashboard) {
        public RenameDashboardOutcome {
            Objects.requireNonNull(status, "status");
            if ((status == RenameDashboardStatus.RENAMED) != (dashboard != null)) {
                throw new IllegalArgumentException("重命名仪表盘结果与状态不一致");
            }
        }

        public static RenameDashboardOutcome renamed(RenamedDashboard dashboard) {
            return new RenameDashboardOutcome(RenameDashboardStatus.RENAMED,
                    Objects.requireNonNull(dashboard, "dashboard"));
        }

        public static RenameDashboardOutcome conflict() {
            return new RenameDashboardOutcome(RenameDashboardStatus.CONFLICT, null);
        }

        public static RenameDashboardOutcome stale() {
            return new RenameDashboardOutcome(RenameDashboardStatus.STALE, null);
        }

        public static RenameDashboardOutcome accessDenied() {
            return new RenameDashboardOutcome(RenameDashboardStatus.ACCESS_DENIED, null);
        }
    }
}

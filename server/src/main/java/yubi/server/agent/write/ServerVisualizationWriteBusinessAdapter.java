package yubi.server.agent.write;

import org.springframework.stereotype.Component;
import yubi.core.base.consts.Const;
import yubi.core.base.exception.ParamException;
import yubi.core.entity.Dashboard;
import yubi.core.entity.Folder;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.security.base.ResourceType;
import yubi.security.manager.PermissionDataCache;
import yubi.server.base.params.DatachartCreateParam;
import yubi.server.base.params.FolderUpdateParam;
import yubi.server.service.DashboardService;
import yubi.server.service.FolderService;
import yubi.server.service.OrgService;
import yubi.server.service.ViewService;
import yubi.server.service.VizService;
import yubi.security.manager.YuBiSecurityManager;
import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.CreatedChart;
import yubi.visualization.write.domain.VisualizationWriteModels.DashboardTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ParentTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardOutcome;
import yubi.visualization.write.domain.VisualizationWriteModels.RenamedDashboard;
import yubi.visualization.write.domain.VisualizationWriteModels.ViewTarget;
import yubi.visualization.write.port.ChartWritePort;
import yubi.visualization.write.port.DashboardWritePort;
import yubi.visualization.write.port.VisualizationWriteAuthorizationPort;
import yubi.server.agent.write.ServerVisualizationWriteCurrentReadGateway.LockedCreateTarget;
import yubi.server.agent.write.ServerVisualizationWriteCurrentReadGateway.LockedDashboardTarget;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public final class ServerVisualizationWriteBusinessAdapter implements ChartWritePort,
        DashboardWritePort, VisualizationWriteAuthorizationPort {

    static final String DEFAULT_CHART_GRAPH = "mingxi-table";
    static final String DEFAULT_CHART_CONFIG = """
            {"aggregation":false,"chartConfig":{"datas":[],"styles":[],"settings":[],"interactions":[]},"chartGraphId":"mingxi-table","computedFields":[]}
            """.trim();

    private final ViewService viewService;
    private final FolderService folderService;
    private final DashboardService dashboardService;
    private final VizService vizService;
    private final OrgService orgService;
    private final YuBiSecurityManager securityManager;
    private final PermissionDataCache permissionDataCache;
    private final ServerVisualizationWriteCurrentReadGateway currentReads;

    public ServerVisualizationWriteBusinessAdapter(ViewService viewService,
                                                   FolderService folderService,
                                                   DashboardService dashboardService,
                                                   VizService vizService,
                                                   OrgService orgService,
                                                   YuBiSecurityManager securityManager,
                                                   PermissionDataCache permissionDataCache,
                                                   ServerVisualizationWriteCurrentReadGateway currentReads) {
        this.viewService = viewService;
        this.folderService = folderService;
        this.dashboardService = dashboardService;
        this.vizService = vizService;
        this.orgService = orgService;
        this.securityManager = securityManager;
        this.permissionDataCache = permissionDataCache;
        this.currentReads = currentReads;
    }

    @Override
    public Optional<ViewTarget> findView(String viewId) {
        try {
            View view = viewService.retrieve(viewId, false);
            if (view == null || !active(view.getStatus())) {
                return Optional.empty();
            }
            return Optional.of(new ViewTarget(view.getId(), view.getOrgId()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ParentTarget> findParent(String parentId) {
        try {
            Folder folder = folderService.retrieve(parentId, false);
            if (folder == null || !ResourceType.FOLDER.name().equals(folder.getRelType())) {
                return Optional.empty();
            }
            return Optional.of(new ParentTarget(folder.getId(), folder.getOrgId()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean chartNameExists(String organizationId, String parentId, String name) {
        return nameExists(organizationId, parentId, name);
    }

    @Override
    public boolean canCreateChart(VisualizationWriteContext context, ViewTarget view, ParentTarget parent) {
        try {
            if (!trusted(context) || view == null
                    || !context.organizationId().equals(view.organizationId())) {
                return false;
            }
            View authorizedView = viewService.retrieve(view.id(), false);
            if (!context.organizationId().equals(authorizedView.getOrgId()) || !active(authorizedView.getStatus())) {
                return false;
            }
            viewService.requirePermission(authorizedView, Const.READ);
            Folder destination;
            if (parent == null) {
                destination = new Folder();
                destination.setOrgId(context.organizationId());
                destination.setRelType(ResourceType.DATACHART.name());
                destination.setName("agent-chart-destination");
            } else {
                if (!context.organizationId().equals(parent.organizationId())) {
                    return false;
                }
                destination = folderService.retrieve(parent.id(), false);
                if (!ResourceType.FOLDER.name().equals(destination.getRelType())
                        || !context.organizationId().equals(destination.getOrgId())) {
                    return false;
                }
            }
            folderService.requirePermission(destination, Const.CREATE);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public CreateChartOutcome createChart(CreateChartDraft draft, VisualizationWriteContext context) {
        Optional<LockedCreateTarget> locked = currentReads.lockCreateTarget(
                context.organizationId(), draft.viewId(), draft.parentId());
        if (locked.isEmpty() || !currentReads.lockTrustedAuthorization(
                context.organizationId(), context.subjectId())) {
            return CreateChartOutcome.accessDenied();
        }
        permissionDataCache.clear();
        if (!canCreateChartCurrent(context, locked.get())) {
            return CreateChartOutcome.accessDenied();
        }
        if (currentReads.nameExists(context.organizationId(), draft.parentId(), draft.name())) {
            return CreateChartOutcome.conflict();
        }

        DatachartCreateParam parameter = new DatachartCreateParam();
        parameter.setName(draft.name());
        parameter.setDescription(draft.description());
        parameter.setViewId(draft.viewId());
        parameter.setParentId(draft.parentId());
        parameter.setOrgId(context.organizationId());
        parameter.setConfig(DEFAULT_CHART_CONFIG);
        parameter.setAvatar(DEFAULT_CHART_GRAPH);
        parameter.setIndex(0D);
        parameter.setStatus((short) Const.DATA_STATUS_ACTIVE);
        parameter.setPermissions(List.of());
        try {
            Folder created = vizService.createDatachart(parameter);
            if (!validCreatedChartFolder(created, draft, context)) {
                throw new IllegalStateException("受控图表创建结果无效");
            }
            return CreateChartOutcome.created(new CreatedChart(created.getRelId(), draft.name(),
                    fingerprint("CHART", created.getRelId(), context.organizationId(), draft.viewId(),
                            draft.parentId(), draft.name())));
        } catch (ParamException exception) {
            return CreateChartOutcome.conflict();
        }
    }

    @Override
    public Optional<DashboardTarget> findDashboard(String dashboardId) {
        try {
            Dashboard dashboard = dashboardService.retrieve(dashboardId, false);
            Folder folder = folderService.getVizFolder(dashboardId, ResourceType.DASHBOARD.name());
            if (!validDashboardFolder(dashboard, folder) || !active(dashboard.getStatus())) {
                return Optional.empty();
            }
            return Optional.of(target(dashboard, folder));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean canRenameDashboard(VisualizationWriteContext context, DashboardTarget dashboard) {
        try {
            if (!trusted(context) || dashboard == null
                    || !context.organizationId().equals(dashboard.organizationId())) {
                return false;
            }
            Dashboard current = dashboardService.retrieve(dashboard.id(), false);
            Folder folder = folderService.getVizFolder(dashboard.id(), ResourceType.DASHBOARD.name());
            if (!validDashboardFolder(current, folder) || !active(current.getStatus())
                    || !context.organizationId().equals(current.getOrgId())) {
                return false;
            }
            folderService.requirePermission(folder, Const.MANAGE);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public boolean dashboardNameExists(String organizationId,
                                       String parentId,
                                       String name,
                                       String excludedDashboardId) {
        return nameExists(organizationId, parentId, name);
    }

    @Override
    public RenameDashboardOutcome renameDashboard(RenameDashboardDraft draft,
                                                  String expectedStateFingerprint,
                                                  VisualizationWriteContext context) {
        Optional<LockedDashboardTarget> locked = currentReads.lockDashboardTarget(draft.dashboardId());
        if (locked.isEmpty() || !currentReads.lockTrustedAuthorization(
                context.organizationId(), context.subjectId())) {
            return RenameDashboardOutcome.accessDenied();
        }
        permissionDataCache.clear();
        Dashboard dashboard = locked.get().dashboard();
        Folder folder = locked.get().folder();
        if (!validDashboardFolder(dashboard, folder) || !active(dashboard.getStatus())) {
            return RenameDashboardOutcome.accessDenied();
        }
        DashboardTarget current = target(dashboard, folder);
        if (!canRenameDashboardCurrent(context, current, folder)) {
            return RenameDashboardOutcome.accessDenied();
        }
        if (!Objects.equals(expectedStateFingerprint, current.stateFingerprint())) {
            return RenameDashboardOutcome.stale();
        }
        if (currentReads.nameExists(context.organizationId(), folder.getParentId(), draft.newName())) {
            return RenameDashboardOutcome.conflict();
        }

        FolderUpdateParam parameter = new FolderUpdateParam();
        parameter.setId(folder.getId());
        parameter.setName(draft.newName());
        parameter.setParentId(folder.getParentId());
        parameter.setIndex(folder.getIndex());
        try {
            if (!folderService.update(parameter)) {
                throw new IllegalStateException("受控仪表盘重命名未更新目标");
            }
        } catch (ParamException exception) {
            return RenameDashboardOutcome.conflict();
        }
        Dashboard updatedDashboard = dashboardService.retrieve(draft.dashboardId(), false);
        Folder updatedFolder = folderService.getVizFolder(draft.dashboardId(), ResourceType.DASHBOARD.name());
        if (!validDashboardFolder(updatedDashboard, updatedFolder)
                || !active(updatedDashboard.getStatus())
                || !context.organizationId().equals(updatedDashboard.getOrgId())
                || !draft.newName().equals(updatedDashboard.getName())
                || !draft.newName().equals(updatedFolder.getName())) {
            throw new IllegalStateException("受控仪表盘重命名结果不一致");
        }
        DashboardTarget updated = target(updatedDashboard, updatedFolder);
        return RenameDashboardOutcome.renamed(new RenamedDashboard(draft.dashboardId(),
                current.name(), updated.name(), updated.stateFingerprint()));
    }

    private boolean trusted(VisualizationWriteContext context) {
        if (context == null || context.subjectId() == null || context.organizationId() == null) {
            return false;
        }
        User current = securityManager.getCurrentUser();
        if (current == null || !context.subjectId().equals(current.getId())) {
            return false;
        }
        var organizations = orgService.listOrganizations();
        return organizations != null && organizations.stream()
                .anyMatch(value -> value != null && context.organizationId().equals(value.getId()));
    }

    private boolean canCreateChartCurrent(VisualizationWriteContext context, LockedCreateTarget target) {
        try {
            if (!trustedSubject(context) || target.view() == null
                    || !context.organizationId().equals(target.view().getOrgId())
                    || !active(target.view().getStatus())) {
                return false;
            }
            viewService.requirePermission(target.view(), Const.READ);
            Folder destination = target.parent();
            if (destination == null) {
                destination = new Folder();
                destination.setOrgId(context.organizationId());
                destination.setRelType(ResourceType.DATACHART.name());
                destination.setName("agent-chart-destination");
            } else if (!ResourceType.FOLDER.name().equals(destination.getRelType())
                    || !context.organizationId().equals(destination.getOrgId())) {
                return false;
            }
            folderService.requirePermission(destination, Const.CREATE);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean canRenameDashboardCurrent(VisualizationWriteContext context,
                                              DashboardTarget dashboard,
                                              Folder folder) {
        try {
            if (!trustedSubject(context) || dashboard == null || folder == null
                    || !context.organizationId().equals(dashboard.organizationId())) {
                return false;
            }
            folderService.requirePermission(folder, Const.MANAGE);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean trustedSubject(VisualizationWriteContext context) {
        if (context == null || context.subjectId() == null || context.organizationId() == null) {
            return false;
        }
        User current = securityManager.getCurrentUser();
        return current != null && context.subjectId().equals(current.getId());
    }

    private boolean nameExists(String organizationId, String parentId, String name) {
        try {
            folderService.checkUnique(organizationId, parentId, name);
            return false;
        } catch (ParamException exception) {
            return true;
        }
    }

    private DashboardTarget target(Dashboard dashboard, Folder folder) {
        return new DashboardTarget(dashboard.getId(), dashboard.getOrgId(), dashboard.getName(),
                folder.getParentId(), dashboardFingerprint(dashboard, folder));
    }

    private String dashboardFingerprint(Dashboard dashboard, Folder folder) {
        return fingerprint("DASHBOARD", dashboard.getId(), dashboard.getOrgId(), dashboard.getName(),
                dashboard.getStatus(), dashboard.getConfig(), dashboard.getUpdateBy(), dashboard.getUpdateTime(),
                folder.getId(), folder.getName(), folder.getParentId(), folder.getIndex(), folder.getSubType(),
                folder.getAvatar(), folder.getUpdateBy(), folder.getUpdateTime());
    }

    private String fingerprint(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object value : values) {
                byte[] bytes = String.valueOf(value).getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用");
        }
    }

    private boolean validDashboardFolder(Dashboard dashboard, Folder folder) {
        return dashboard != null && folder != null
                && ResourceType.DASHBOARD.name().equals(folder.getRelType())
                && Objects.equals(dashboard.getId(), folder.getRelId())
                && Objects.equals(dashboard.getOrgId(), folder.getOrgId());
    }

    private boolean validCreatedChartFolder(Folder folder,
                                            CreateChartDraft draft,
                                            VisualizationWriteContext context) {
        return folder != null && folder.getRelId() != null && !folder.getRelId().isBlank()
                && ResourceType.DATACHART.name().equals(folder.getRelType())
                && context.organizationId().equals(folder.getOrgId())
                && draft.name().equals(folder.getName())
                && Objects.equals(draft.parentId(), folder.getParentId());
    }

    private boolean active(Number status) {
        return status != null && status.intValue() == Const.DATA_STATUS_ACTIVE;
    }

}

package yubi.server.agent.write;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import yubi.core.base.consts.Const;
import yubi.core.entity.Dashboard;
import yubi.core.entity.Folder;
import yubi.core.entity.Organization;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.security.base.ResourceType;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.manager.PermissionDataCache;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.OrganizationBaseInfo;
import yubi.server.base.params.DatachartCreateParam;
import yubi.server.base.params.FolderUpdateParam;
import yubi.server.service.DashboardService;
import yubi.server.service.FolderService;
import yubi.server.service.OrgService;
import yubi.server.service.ViewService;
import yubi.server.service.VizService;
import yubi.visualization.write.api.CreateChartCommand;
import yubi.visualization.write.api.PreparedCreateChart;
import yubi.visualization.write.api.VisualizationWriteContext;
import yubi.visualization.write.api.VisualizationWriteException;
import yubi.visualization.write.api.VisualizationWriteFailureCode;
import yubi.visualization.write.application.DefaultCreateChartService;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.CreateChartStatus;
import yubi.visualization.write.domain.VisualizationWriteModels.DashboardTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.ParentTarget;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardDraft;
import yubi.visualization.write.domain.VisualizationWriteModels.RenameDashboardStatus;
import yubi.visualization.write.domain.VisualizationWriteModels.ViewTarget;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerVisualizationWriteBusinessAdapterTest {

    private static final VisualizationWriteContext CONTEXT = new VisualizationWriteContext(
            "user-1", "org-1", "session-1", "request-1", "correlation-1");

    private ViewService viewService;
    private FolderService folderService;
    private DashboardService dashboardService;
    private VizService vizService;
    private OrgService orgService;
    private YuBiSecurityManager securityManager;
    private PermissionDataCache permissionDataCache;
    private ServerVisualizationWriteCurrentReadGateway currentReads;
    private ServerVisualizationWriteBusinessAdapter adapter;

    @BeforeEach
    void setUp() {
        viewService = mock(ViewService.class);
        folderService = mock(FolderService.class);
        dashboardService = mock(DashboardService.class);
        vizService = mock(VizService.class);
        orgService = mock(OrgService.class);
        securityManager = mock(YuBiSecurityManager.class);
        permissionDataCache = mock(PermissionDataCache.class);
        currentReads = mock(ServerVisualizationWriteCurrentReadGateway.class);
        adapter = new ServerVisualizationWriteBusinessAdapter(viewService, folderService, dashboardService,
                vizService, orgService, securityManager, permissionDataCache, currentReads);

        when(securityManager.getCurrentUser()).thenReturn(user("user-1"));
        when(orgService.listOrganizations()).thenReturn(List.of(organization("org-1")));
        when(folderService.checkUnique(anyString(), any(), anyString())).thenReturn(true);
        when(currentReads.lockTrustedAuthorization(anyString(), anyString())).thenReturn(true);
        when(currentReads.nameExists(anyString(), any(), anyString())).thenReturn(false);
    }

    @Test
    void shouldRequireTrustedIdentityOrganizationReadableViewAndCreatableDestination() {
        View view = view("view-1", "org-1");
        Folder parent = folder("folder-1", "org-1", ResourceType.FOLDER.name(), null,
                null, "业务目录");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        when(folderService.retrieve("folder-1", false)).thenReturn(parent);

        boolean allowed = adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1"));

        assertTrue(allowed);
        verify(viewService).requirePermission(view, Const.READ);
        verify(folderService).requirePermission(parent, Const.CREATE);
    }

    @Test
    void shouldRejectUntrustedScopeAndInvalidDestinationBeforeCreatePermissionCheck() {
        View view = view("view-1", "org-1");
        when(viewService.retrieve("view-1", false)).thenReturn(view);

        when(securityManager.getCurrentUser()).thenReturn(user("user-2"));
        assertFalse(adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1")));

        when(securityManager.getCurrentUser()).thenReturn(user("user-1"));
        when(orgService.listOrganizations()).thenReturn(List.of(organization("org-2")));
        assertFalse(adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1")));

        when(orgService.listOrganizations()).thenReturn(List.of(organization("org-1")));
        Folder wrongType = folder("folder-1", "org-1", ResourceType.DASHBOARD.name(),
                "dashboard-1", null, "非目录资源");
        when(folderService.retrieve("folder-1", false)).thenReturn(wrongType);
        assertFalse(adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1")));

        verify(folderService, never()).requirePermission(any(), eq(Const.CREATE));
        verify(vizService, never()).createDatachart(any());
    }

    @Test
    void shouldRejectUnreadableViewOrMissingDestinationCreatePermission() {
        View view = view("view-1", "org-1");
        Folder parent = folder("folder-1", "org-1", ResourceType.FOLDER.name(), null,
                null, "业务目录");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        when(folderService.retrieve("folder-1", false)).thenReturn(parent);

        doThrow(new PermissionDeniedException("view denied"))
                .when(viewService).requirePermission(view, Const.READ);
        assertFalse(adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1")));

        doNothing().when(viewService).requirePermission(view, Const.READ);
        doThrow(new PermissionDeniedException("create denied"))
                .when(folderService).requirePermission(parent, Const.CREATE);
        assertFalse(adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1")));

        verify(vizService, never()).createDatachart(any());
    }

    @Test
    void shouldRejectViewOrDestinationWhoseCurrentOrganizationNoLongerMatches() {
        View movedView = view("view-1", "org-2");
        when(viewService.retrieve("view-1", false)).thenReturn(movedView);
        assertFalse(adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1")));

        View view = view("view-1", "org-1");
        Folder movedParent = folder("folder-1", "org-2", ResourceType.FOLDER.name(), null,
                null, "已移动目录");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        when(folderService.retrieve("folder-1", false)).thenReturn(movedParent);
        assertFalse(adapter.canCreateChart(CONTEXT,
                new ViewTarget("view-1", "org-1"), new ParentTarget("folder-1", "org-1")));

        verify(folderService, never()).requirePermission(any(), eq(Const.CREATE));
        verify(vizService, never()).createDatachart(any());
    }

    @Test
    void shouldCreateChartWithOnlyServerOwnedConfigurationStatusAndPermissions() {
        View view = view("view-1", "org-1");
        Folder parent = folder("folder-1", "org-1", ResourceType.FOLDER.name(), null,
                null, "业务目录");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        when(folderService.retrieve("folder-1", false)).thenReturn(parent);
        stubLockedCreate(view, parent);
        when(vizService.createDatachart(any())).thenReturn(folder("chart-folder-1", "org-1",
                ResourceType.DATACHART.name(), "chart-1", "folder-1", "季度收入"));

        var result = adapter.createChart(new CreateChartDraft(
                "季度收入", "view-1", "folder-1", "只读数据生成"), CONTEXT);

        assertEquals(CreateChartStatus.CREATED, result.status());
        assertEquals("chart-1", result.chart().id());
        ArgumentCaptor<DatachartCreateParam> captor = ArgumentCaptor.forClass(DatachartCreateParam.class);
        verify(vizService).createDatachart(captor.capture());
        DatachartCreateParam parameter = captor.getValue();
        assertEquals("org-1", parameter.getOrgId());
        assertEquals("季度收入", parameter.getName());
        assertEquals("view-1", parameter.getViewId());
        assertEquals("folder-1", parameter.getParentId());
        assertEquals("只读数据生成", parameter.getDescription());
        assertEquals(ServerVisualizationWriteBusinessAdapter.DEFAULT_CHART_CONFIG, parameter.getConfig());
        assertEquals(ServerVisualizationWriteBusinessAdapter.DEFAULT_CHART_GRAPH, parameter.getAvatar());
        assertEquals((short) Const.DATA_STATUS_ACTIVE, parameter.getStatus());
        assertEquals(List.of(), parameter.getPermissions());
        assertEquals(0D, parameter.getIndex());
        assertNull(parameter.getSubType());
        verify(permissionDataCache).clear();

        assertEquals(List.of("name", "viewId", "parentId", "description"),
                recordComponents(CreateChartDraft.class));
        assertEquals(List.of("dashboardId", "newName"), recordComponents(RenameDashboardDraft.class));
    }

    @Test
    void shouldRejectCreatedChartResultOutsideTrustedOrganizationOrDestination() {
        View view = view("view-1", "org-1");
        Folder parent = folder("folder-1", "org-1", ResourceType.FOLDER.name(), null,
                null, "业务目录");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        when(folderService.retrieve("folder-1", false)).thenReturn(parent);
        stubLockedCreate(view, parent);
        when(vizService.createDatachart(any())).thenReturn(folder("chart-folder-1", "org-2",
                ResourceType.DATACHART.name(), "chart-1", "folder-2", "被篡改名称"));

        assertThrows(IllegalStateException.class, () -> adapter.createChart(new CreateChartDraft(
                "季度收入", "view-1", "folder-1", null), CONTEXT));
    }

    @Test
    void shouldReauthorizeAtExecutionAndStopWhenCreatePermissionWasRevoked() {
        View view = view("view-1", "org-1");
        Folder parent = folder("folder-1", "org-1", ResourceType.FOLDER.name(), null,
                null, "业务目录");
        when(viewService.retrieve("view-1", false)).thenReturn(view);
        when(folderService.retrieve("folder-1", false)).thenReturn(parent);
        stubLockedCreate(view, parent);
        doNothing().doThrow(new PermissionDeniedException("create permission revoked"))
                .when(folderService).requirePermission(parent, Const.CREATE);
        DefaultCreateChartService useCase = new DefaultCreateChartService(adapter, adapter);
        PreparedCreateChart prepared = useCase.prepare(
                new CreateChartCommand("季度收入", "view-1", "folder-1", null), CONTEXT);

        VisualizationWriteException failure = assertThrows(VisualizationWriteException.class,
                () -> useCase.execute(prepared, CONTEXT));

        assertEquals(VisualizationWriteFailureCode.CREATE_CHART_ACCESS_DENIED, failure.code());
        verify(folderService, times(2)).requirePermission(parent, Const.CREATE);
        verify(vizService, never()).createDatachart(any());
    }

    @Test
    void shouldRejectStaleRenameBeforeCheckingNameOrUpdatingFolder() {
        Dashboard dashboard = dashboard("dashboard-1", "org-1", "旧名称", 1L);
        Folder folder = dashboardFolder("folder-dashboard-1", "dashboard-1", "org-1",
                "旧名称", "folder-parent", 1L);
        stubDashboard(dashboard, folder);

        var result = adapter.renameDashboard(new RenameDashboardDraft("dashboard-1", "新名称"),
                "stale-fingerprint", CONTEXT);

        assertEquals(RenameDashboardStatus.STALE, result.status());
        verify(folderService).requirePermission(folder, Const.MANAGE);
        verify(currentReads, never()).nameExists(anyString(), any(), anyString());
        verify(folderService, never()).update(any());
    }

    @Test
    void shouldRequireManagePermissionAgainBeforeRenameExecution() {
        Dashboard dashboard = dashboard("dashboard-1", "org-1", "旧名称", 1L);
        Folder folder = dashboardFolder("folder-dashboard-1", "dashboard-1", "org-1",
                "旧名称", "folder-parent", 1L);
        stubDashboard(dashboard, folder);
        DashboardTarget target = adapter.findDashboard("dashboard-1").orElseThrow();
        doThrow(new PermissionDeniedException("manage permission revoked"))
                .when(folderService).requirePermission(folder, Const.MANAGE);

        var result = adapter.renameDashboard(
                new RenameDashboardDraft("dashboard-1", "新名称"), target.stateFingerprint(), CONTEXT);

        assertEquals(RenameDashboardStatus.ACCESS_DENIED, result.status());
        verify(folderService, never()).update(any());
    }

    @Test
    void shouldReturnConflictWithoutChangingDashboardWhenNameAlreadyExists() {
        Dashboard dashboard = dashboard("dashboard-1", "org-1", "旧名称", 1L);
        Folder folder = dashboardFolder("folder-dashboard-1", "dashboard-1", "org-1",
                "旧名称", "folder-parent", 1L);
        stubDashboard(dashboard, folder);
        DashboardTarget target = adapter.findDashboard("dashboard-1").orElseThrow();
        when(currentReads.nameExists("org-1", "folder-parent", "已存在名称")).thenReturn(true);

        var result = adapter.renameDashboard(new RenameDashboardDraft("dashboard-1", "已存在名称"),
                target.stateFingerprint(), CONTEXT);

        assertEquals(RenameDashboardStatus.CONFLICT, result.status());
        verify(folderService, never()).update(any());
    }

    @Test
    void shouldRenameFolderAndDashboardThroughNarrowFolderUpdatePath() {
        AtomicReference<Dashboard> dashboard = new AtomicReference<>(
                dashboard("dashboard-1", "org-1", "旧名称", 1L));
        AtomicReference<Folder> folder = new AtomicReference<>(dashboardFolder(
                "folder-dashboard-1", "dashboard-1", "org-1", "旧名称", "folder-parent", 1L));
        when(dashboardService.retrieve("dashboard-1", false)).thenAnswer(invocation -> dashboard.get());
        when(folderService.getVizFolder("dashboard-1", ResourceType.DASHBOARD.name()))
                .thenAnswer(invocation -> folder.get());
        when(currentReads.lockDashboardTarget("dashboard-1")).thenAnswer(invocation -> Optional.of(
                new ServerVisualizationWriteCurrentReadGateway.LockedDashboardTarget(
                        dashboard.get(), folder.get())));
        when(folderService.update(any())).thenAnswer(invocation -> {
            FolderUpdateParam parameter = invocation.getArgument(0);
            dashboard.set(dashboard("dashboard-1", "org-1", parameter.getName(), 2L));
            folder.set(dashboardFolder("folder-dashboard-1", "dashboard-1", "org-1",
                    parameter.getName(), parameter.getParentId(), 2L));
            return true;
        });
        DashboardTarget target = adapter.findDashboard("dashboard-1").orElseThrow();

        var result = adapter.renameDashboard(new RenameDashboardDraft("dashboard-1", "新名称"),
                target.stateFingerprint(), CONTEXT);

        assertEquals(RenameDashboardStatus.RENAMED, result.status());
        assertEquals("旧名称", result.dashboard().previousName());
        assertEquals("新名称", result.dashboard().currentName());
        assertEquals("新名称", folder.get().getName());
        assertEquals("新名称", dashboard.get().getName());
        ArgumentCaptor<FolderUpdateParam> captor = ArgumentCaptor.forClass(FolderUpdateParam.class);
        verify(folderService).update(captor.capture());
        FolderUpdateParam parameter = captor.getValue();
        assertEquals("folder-dashboard-1", parameter.getId());
        assertEquals("folder-parent", parameter.getParentId());
        assertEquals("新名称", parameter.getName());
        assertEquals(3D, parameter.getIndex());
        assertNull(parameter.getStatus());
        assertNull(parameter.getSubType());
        assertNull(parameter.getAvatar());
        assertNull(parameter.getDescription());
    }

    @Test
    void shouldFailRenameWhenFolderAndDashboardAreNotSynchronized() {
        Dashboard before = dashboard("dashboard-1", "org-1", "旧名称", 1L);
        Folder beforeFolder = dashboardFolder("folder-dashboard-1", "dashboard-1", "org-1",
                "旧名称", "folder-parent", 1L);
        AtomicReference<Dashboard> dashboard = new AtomicReference<>(before);
        when(dashboardService.retrieve("dashboard-1", false)).thenAnswer(invocation -> dashboard.get());
        when(folderService.getVizFolder("dashboard-1", ResourceType.DASHBOARD.name()))
                .thenReturn(beforeFolder);
        when(currentReads.lockDashboardTarget("dashboard-1")).thenAnswer(invocation -> Optional.of(
                new ServerVisualizationWriteCurrentReadGateway.LockedDashboardTarget(
                        dashboard.get(), beforeFolder)));
        when(folderService.update(any())).thenAnswer(invocation -> {
            dashboard.set(dashboard("dashboard-1", "org-1", "新名称", 2L));
            return true;
        });
        DashboardTarget target = adapter.findDashboard("dashboard-1").orElseThrow();

        assertThrows(IllegalStateException.class, () -> adapter.renameDashboard(
                new RenameDashboardDraft("dashboard-1", "新名称"), target.stateFingerprint(), CONTEXT));
    }

    private void stubDashboard(Dashboard dashboard, Folder folder) {
        when(dashboardService.retrieve(dashboard.getId(), false)).thenReturn(dashboard);
        when(folderService.getVizFolder(dashboard.getId(), ResourceType.DASHBOARD.name())).thenReturn(folder);
        when(currentReads.lockDashboardTarget(dashboard.getId())).thenReturn(Optional.of(
                new ServerVisualizationWriteCurrentReadGateway.LockedDashboardTarget(dashboard, folder)));
    }

    private void stubLockedCreate(View view, Folder parent) {
        when(currentReads.lockCreateTarget("org-1", view.getId(), parent == null ? null : parent.getId()))
                .thenReturn(Optional.of(
                        new ServerVisualizationWriteCurrentReadGateway.LockedCreateTarget(view, parent)));
    }

    private View view(String id, String organizationId) {
        View view = new View();
        view.setId(id);
        view.setOrgId(organizationId);
        view.setName("订单视图");
        view.setStatus(Const.DATA_STATUS_ACTIVE);
        return view;
    }

    private Dashboard dashboard(String id, String organizationId, String name, long version) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(id);
        dashboard.setOrgId(organizationId);
        dashboard.setName(name);
        dashboard.setStatus(Const.DATA_STATUS_ACTIVE);
        dashboard.setConfig("{}");
        dashboard.setUpdateBy("user-1");
        dashboard.setUpdateTime(new Date(version));
        return dashboard;
    }

    private Folder dashboardFolder(String id,
                                   String dashboardId,
                                   String organizationId,
                                   String name,
                                   String parentId,
                                   long version) {
        Folder folder = folder(id, organizationId, ResourceType.DASHBOARD.name(), dashboardId, parentId, name);
        folder.setIndex(3D);
        folder.setSubType("free");
        folder.setAvatar("dashboard");
        folder.setUpdateBy("user-1");
        folder.setUpdateTime(new Date(version));
        return folder;
    }

    private Folder folder(String id,
                          String organizationId,
                          String relType,
                          String relId,
                          String parentId,
                          String name) {
        Folder folder = new Folder();
        folder.setId(id);
        folder.setOrgId(organizationId);
        folder.setRelType(relType);
        folder.setRelId(relId);
        folder.setParentId(parentId);
        folder.setName(name);
        return folder;
    }

    private User user(String id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private OrganizationBaseInfo organization(String id) {
        Organization organization = new Organization();
        organization.setId(id);
        return new OrganizationBaseInfo(organization);
    }

    private List<String> recordComponents(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}

package yubi.server.recycle;

import org.junit.jupiter.api.Test;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import yubi.core.entity.Datachart;
import yubi.core.entity.Folder;
import yubi.core.entity.Source;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.SourceMapperExt;
import yubi.security.base.ResourceType;
import yubi.server.service.DatachartService;
import yubi.server.service.FolderService;
import yubi.server.service.SourceService;
import yubi.server.service.VizService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecycleAdapterOwnerAccessTest {

    private static final RecycleAccess OWNER = RecycleAccess.authenticated(
            "user-1", "org-1", true);
    private static final RecycleAccess MEMBER = RecycleAccess.authenticated(
            "user-1", "org-1", false);

    @Test
    void 组织拥有者无需单独资源角色即可预检数据图表() {
        DatachartService service = mock(DatachartService.class);
        ShareMapperExt shareMapper = mock(ShareMapperExt.class);
        RecycleDependencyResolver dependencyResolver = mock(RecycleDependencyResolver.class);
        Datachart chart = datachart();
        when(service.retrieve(chart.getId())).thenReturn(chart);
        doThrow(new SecurityException("没有资源角色"))
                .when(service).requirePermission(chart, yubi.core.base.consts.Const.MANAGE);
        when(shareMapper.selectByViz(chart.getId())).thenReturn(List.of());
        when(dependencyResolver.find(OWNER, RecycleResourceType.DATACHART, chart.getId()))
                .thenReturn(List.of());
        when(service.safeDelete(chart.getId())).thenReturn(true);

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                service,
                mock(VizService.class),
                mock(FolderService.class),
                mock(FolderMapperExt.class),
                shareMapper,
                dependencyResolver);

        RecycleItemPreflight result = adapter.preflight(
                OWNER, chart.getId(), Set.of(chart.getId()));

        assertEquals(RecycleItemStatus.SUCCESS, result.status());
        verify(service, never()).requirePermission(chart, yubi.core.base.consts.Const.MANAGE);
    }

    @Test
    void 数据图表回收适配器通过事务代理后仍保留资源类型() {
        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                mock(DatachartService.class), mock(VizService.class),
                mock(FolderService.class), mock(FolderMapperExt.class),
                mock(ShareMapperExt.class), mock(RecycleDependencyResolver.class));
        ProxyFactory proxyFactory = new ProxyFactory(adapter);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice((MethodInterceptor) invocation -> invocation.proceed());

        RecycleResourceAdapter proxy = (RecycleResourceAdapter) proxyFactory.getProxy();

        assertEquals(RecycleResourceType.DATACHART, proxy.type());
    }

    @Test
    void 普通成员缺少资源角色时仍不能预检数据图表() {
        DatachartService service = mock(DatachartService.class);
        Datachart chart = datachart();
        when(service.retrieve(chart.getId())).thenReturn(chart);
        doThrow(new SecurityException("没有资源角色"))
                .when(service).requirePermission(chart, yubi.core.base.consts.Const.MANAGE);

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                service,
                mock(VizService.class),
                mock(FolderService.class),
                mock(FolderMapperExt.class),
                mock(ShareMapperExt.class),
                mock(RecycleDependencyResolver.class));

        RecycleItemPreflight result = adapter.preflight(
                MEMBER, chart.getId(), Set.of(chart.getId()));

        assertEquals(RecycleItemStatus.FORBIDDEN, result.status());
        verify(service).requirePermission(chart, yubi.core.base.consts.Const.MANAGE);
    }

    @Test
    void 依赖解析异常不应伪装成没有管理权限() {
        DatachartService service = mock(DatachartService.class);
        ShareMapperExt shareMapper = mock(ShareMapperExt.class);
        RecycleDependencyResolver dependencyResolver = mock(RecycleDependencyResolver.class);
        Datachart chart = datachart();
        when(service.retrieve(chart.getId())).thenReturn(chart);
        when(shareMapper.selectByViz(chart.getId())).thenReturn(List.of());
        when(dependencyResolver.find(OWNER, RecycleResourceType.DATACHART, chart.getId()))
                .thenThrow(new IllegalStateException("依赖查询失败"));

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                service,
                mock(VizService.class),
                mock(FolderService.class),
                mock(FolderMapperExt.class),
                shareMapper,
                dependencyResolver);

        RecycleItemPreflight result = adapter.preflight(
                OWNER, chart.getId(), Set.of(chart.getId()));

        assertEquals(RecycleItemStatus.FAILED, result.status());
        assertEquals("预检失败，请稍后重试", result.message());
    }

    @Test
    void 数据图表目录任一后代被依赖时整体阻断并返回依赖详情() {
        DatachartService service = mock(DatachartService.class);
        FolderMapperExt folderMapper = mock(FolderMapperExt.class);
        ShareMapperExt shareMapper = mock(ShareMapperExt.class);
        RecycleDependencyResolver dependencyResolver = mock(RecycleDependencyResolver.class);
        Folder root = folder("folder-1", "销售分析", null, ResourceType.FOLDER.name(), null);
        root.setSubType(ResourceType.DATACHART.name());
        Folder leaf = folder("nav-chart-1", "订单图表", root.getId(),
                ResourceType.DATACHART.name(), "chart-1");
        Datachart chart = datachart();
        when(folderMapper.selectByPrimaryKey(root.getId())).thenReturn(root);
        when(folderMapper.selectByOrg("org-1")).thenReturn(List.of(root, leaf));
        when(service.retrieve("chart-1")).thenReturn(chart);
        when(shareMapper.selectByViz("chart-1")).thenReturn(List.of());
        RecycleDependency dependency = new RecycleDependency(
                "dashboard-1", "经营驾驶舱", RecycleResourceType.DASHBOARD,
                RecycleDependencyDepth.DIRECT, true);
        when(dependencyResolver.find(OWNER, RecycleResourceType.DATACHART, "chart-1"))
                .thenReturn(List.of(dependency));
        when(service.safeDelete("chart-1")).thenReturn(true);

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                service, mock(VizService.class), mock(FolderService.class), folderMapper,
                shareMapper, dependencyResolver);

        RecycleItemPreflight result = adapter.preflight(
                OWNER, root.getId(), Set.of(root.getId()));

        assertEquals(RecycleItemStatus.BLOCKED, result.status());
        assertEquals(root.getId(), result.rootId());
        assertEquals(List.of(dependency), result.dependencies());
    }

    @Test
    void 数据图表目录作为一条记录整体移入回收站() {
        DatachartService service = mock(DatachartService.class);
        FolderMapperExt folderMapper = mock(FolderMapperExt.class);
        Folder root = folder("folder-1", "销售分析", null, ResourceType.FOLDER.name(), null);
        root.setSubType(ResourceType.DATACHART.name());
        Folder leaf = folder("nav-chart-1", "订单图表", root.getId(),
                ResourceType.DATACHART.name(), "chart-1");
        when(folderMapper.selectByPrimaryKey(root.getId())).thenReturn(root);
        when(folderMapper.selectByOrg("org-1")).thenReturn(List.of(root, leaf));

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                service, mock(VizService.class), mock(FolderService.class),
                folderMapper, mock(ShareMapperExt.class),
                mock(RecycleDependencyResolver.class));

        RecycleRootSnapshot snapshot = adapter.moveToRecycle(OWNER, root.getId());

        assertEquals(root.getId(), snapshot.rootId());
        assertEquals(true, snapshot.folder());
        assertEquals(2, snapshot.expandedItemCount());
        assertEquals(List.of(root.getId(), leaf.getId()),
                snapshot.nodes().stream().map(RecycleNodeSnapshot::id).toList());
        verify(service).archive("chart-1");
        verify(folderMapper, times(1)).deleteByPrimaryKey(root.getId());
    }

    @Test
    void 数据图表目录恢复时重建完整目录树() {
        DatachartService service = mock(DatachartService.class);
        VizService vizService = mock(VizService.class);
        FolderMapperExt folderMapper = mock(FolderMapperExt.class);
        RecycleRootSnapshot snapshot = new RecycleRootSnapshot(
                "folder-1", "销售分析", null, 0D, true, 2,
                List.of(
                        new RecycleNodeSnapshot(
                                "folder-1", "销售分析", null, 0D, true,
                                null, ResourceType.DATACHART.name(), null),
                        new RecycleNodeSnapshot(
                                "nav-chart-1", "订单图表", "folder-1", 0D, false,
                                "chart-1", null, "chart-icon")
                ));
        when(vizService.unarchiveViz(
                "chart-1", ResourceType.DATACHART, "订单图表", "folder-1", 0D))
                .thenReturn(true);

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                service, vizService, mock(FolderService.class), folderMapper,
                mock(ShareMapperExt.class), mock(RecycleDependencyResolver.class));

        RecycleItemResult result = adapter.restore(OWNER, snapshot);

        assertEquals(RecycleItemStatus.SUCCESS, result.status());
        verify(vizService).unarchiveViz(
                "chart-1", ResourceType.DATACHART, "订单图表", "folder-1", 0D);
        verify(folderMapper).deleteByRelTypeAndId(
                ResourceType.DATACHART.name(), "chart-1");
        verify(folderMapper, times(2)).insert(any(Folder.class));
    }

    @Test
    void 数据图表目录恢复遇到同名目录时整树冲突() {
        FolderService folderService = mock(FolderService.class);
        FolderMapperExt folderMapper = mock(FolderMapperExt.class);
        RecycleRootSnapshot snapshot = new RecycleRootSnapshot(
                "folder-1", "销售分析", null, 0D, true, 1,
                List.of(new RecycleNodeSnapshot(
                        "folder-1", "销售分析", null, 0D, true,
                        null, ResourceType.DATACHART.name(), null)));
        doThrow(new IllegalArgumentException("名称已存在"))
                .when(folderService).checkUnique(
                        "org-1", null, "销售分析", ResourceType.DATACHART);

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                mock(DatachartService.class), mock(VizService.class), folderService,
                folderMapper, mock(ShareMapperExt.class),
                mock(RecycleDependencyResolver.class));

        RecycleItemResult result = adapter.restore(OWNER, snapshot);

        assertEquals(RecycleItemStatus.CONFLICT, result.status());
        verify(folderMapper, never()).insert(any(Folder.class));
    }

    @Test
    void 数据图表目录永久删除前原子重检所有后代() {
        DatachartService service = mock(DatachartService.class);
        FolderService folderService = mock(FolderService.class);
        RelRoleResourceMapperExt permissionMapper = mock(RelRoleResourceMapperExt.class);
        RecycleDependencyResolver dependencyResolver = mock(RecycleDependencyResolver.class);
        when(folderService.getRRRMapper()).thenReturn(permissionMapper);
        when(service.safeDelete("chart-1")).thenReturn(true);
        RecycleRootSnapshot snapshot = new RecycleRootSnapshot(
                "folder-1", "销售分析", null, 0D, true, 2,
                List.of(
                        new RecycleNodeSnapshot(
                                "folder-1", "销售分析", null, 0D, true,
                                null, ResourceType.DATACHART.name(), null),
                        new RecycleNodeSnapshot(
                                "nav-chart-1", "订单图表", "folder-1", 0D, false,
                                "chart-1", null, null)
                ));

        DatachartRecycleAdapter adapter = new DatachartRecycleAdapter(
                service, mock(VizService.class), folderService,
                mock(FolderMapperExt.class), mock(ShareMapperExt.class),
                dependencyResolver);

        RecycleItemResult result = adapter.permanentlyDelete(OWNER, snapshot);

        assertEquals(RecycleItemStatus.SUCCESS, result.status());
        verify(dependencyResolver).find(
                OWNER, RecycleResourceType.DATACHART, "chart-1");
        verify(service).delete("chart-1", false, false);
        verify(permissionMapper).deleteByResourceId("folder-1");
    }

    @Test
    void 组织拥有者无需单独资源角色即可预检树形资源() {
        SourceService service = mock(SourceService.class);
        RecycleDependencyResolver dependencyResolver = mock(RecycleDependencyResolver.class);
        Source source = source();
        when(service.retrieve(source.getId())).thenReturn(source);
        doThrow(new SecurityException("没有资源角色"))
                .when(service).requirePermission(source, yubi.core.base.consts.Const.MANAGE);
        when(dependencyResolver.find(OWNER, RecycleResourceType.SOURCE, source.getId()))
                .thenReturn(List.of());
        when(service.safeDelete(source.getId())).thenReturn(true);

        SourceRecycleAdapter adapter = new SourceRecycleAdapter(
                service, mock(SourceMapperExt.class), dependencyResolver);

        RecycleItemPreflight result = adapter.preflight(
                OWNER, source.getId(), Set.of(source.getId()));

        assertEquals(RecycleItemStatus.SUCCESS, result.status());
        verify(service, never()).requirePermission(source, yubi.core.base.consts.Const.MANAGE);
    }

    @Test
    void 普通成员缺少资源角色时仍不能预检树形资源() {
        SourceService service = mock(SourceService.class);
        Source source = source();
        when(service.retrieve(source.getId())).thenReturn(source);
        doThrow(new SecurityException("没有资源角色"))
                .when(service).requirePermission(source, yubi.core.base.consts.Const.MANAGE);

        SourceRecycleAdapter adapter = new SourceRecycleAdapter(
                service,
                mock(SourceMapperExt.class),
                mock(RecycleDependencyResolver.class));

        RecycleItemPreflight result = adapter.preflight(
                MEMBER, source.getId(), Set.of(source.getId()));

        assertEquals(RecycleItemStatus.FORBIDDEN, result.status());
        verify(service).requirePermission(source, yubi.core.base.consts.Const.MANAGE);
    }

    private Datachart datachart() {
        Datachart chart = new Datachart();
        chart.setId("chart-1");
        chart.setName("test1");
        chart.setOrgId("org-1");
        return chart;
    }

    private Source source() {
        Source source = new Source();
        source.setId("source-1");
        source.setName("source");
        source.setOrgId("org-1");
        source.setIsFolder(false);
        return source;
    }

    private Folder folder(String id,
                          String name,
                          String parentId,
                          String relType,
                          String relId) {
        Folder folder = new Folder();
        folder.setId(id);
        folder.setName(name);
        folder.setOrgId("org-1");
        folder.setParentId(parentId);
        folder.setRelType(relType);
        folder.setRelId(relId);
        folder.setIndex(0D);
        return folder;
    }
}

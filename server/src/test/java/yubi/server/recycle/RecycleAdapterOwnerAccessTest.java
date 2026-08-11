package yubi.server.recycle;

import org.junit.jupiter.api.Test;
import yubi.core.entity.Datachart;
import yubi.core.entity.Source;
import yubi.core.mappers.ext.FolderMapperExt;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.SourceMapperExt;
import yubi.server.service.DatachartService;
import yubi.server.service.SourceService;
import yubi.server.service.VizService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
                mock(FolderMapperExt.class),
                shareMapper,
                dependencyResolver);

        RecycleItemPreflight result = adapter.preflight(
                OWNER, chart.getId(), Set.of(chart.getId()));

        assertEquals(RecycleItemStatus.SUCCESS, result.status());
        verify(service, never()).requirePermission(chart, yubi.core.base.consts.Const.MANAGE);
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
                mock(FolderMapperExt.class),
                shareMapper,
                dependencyResolver);

        RecycleItemPreflight result = adapter.preflight(
                OWNER, chart.getId(), Set.of(chart.getId()));

        assertEquals(RecycleItemStatus.FAILED, result.status());
        assertEquals("预检失败，请稍后重试", result.message());
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
}

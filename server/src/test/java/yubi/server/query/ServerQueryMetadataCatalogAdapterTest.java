package yubi.server.query;

import org.junit.jupiter.api.Test;
import yubi.core.entity.View;
import yubi.core.mappers.ext.ViewMapperExt;
import yubi.query.api.QueryExecutionContext;
import yubi.query.domain.QueryModels.Channel;
import yubi.server.service.ViewService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ServerQueryMetadataCatalogAdapterTest {

    @Test
    void shouldUsePermissionFilteredOrganizationListAndReturnOnlySafeAssetProjections() {
        View asset = view("view-1", "org-1", "source-1", false);
        asset.setScript("select password from secret_table");
        asset.setModel("{\"password\":\"secret-model\"}");
        asset.setConfig("{\"connectionString\":\"jdbc:secret\"}");
        View folder = view("folder-1", "org-1", null, true);
        ViewService viewService = mock(ViewService.class);
        ViewMapperExt viewMapper = mock(ViewMapperExt.class);
        when(viewService.getViews("org-1")).thenReturn(List.of(folder, asset));
        ServerQueryMetadataCatalogAdapter adapter =
                new ServerQueryMetadataCatalogAdapter(viewService, viewMapper);

        var result = adapter.listReadable(context());

        assertEquals(1, result.size());
        assertEquals("view-1", result.getFirst().id());
        assertFalse(result.toString().contains("password"));
        assertFalse(result.toString().contains("jdbc:secret"));
        verify(viewService).getViews("org-1");
        verifyNoMoreInteractions(viewService, viewMapper);
    }

    @Test
    void shouldFindThroughDedicatedSafeProjectionAndRejectFolders() {
        ViewService viewService = mock(ViewService.class);
        ViewMapperExt viewMapper = mock(ViewMapperExt.class);
        View projection = view("view-1", "org-1", "source-1", false);
        when(viewMapper.selectMetadataProjection("view-1")).thenReturn(projection);
        when(viewMapper.selectMetadataProjection("folder-1"))
                .thenReturn(view("folder-1", "org-1", null, true));
        ServerQueryMetadataCatalogAdapter adapter =
                new ServerQueryMetadataCatalogAdapter(viewService, viewMapper);

        assertEquals("source-1", adapter.find("view-1").sourceId());
        assertNull(adapter.find("folder-1"));
        verifyNoMoreInteractions(viewService);
    }

    private QueryExecutionContext context() {
        return new QueryExecutionContext(Channel.AUTHENTICATED,
                "user-1", "org-1", "correlation-1");
    }

    private View view(String id, String orgId, String sourceId, boolean folder) {
        View view = new View();
        view.setId(id);
        view.setName(id);
        view.setDescription("safe description");
        view.setOrgId(orgId);
        view.setSourceId(sourceId);
        view.setIsFolder(folder);
        return view;
    }
}

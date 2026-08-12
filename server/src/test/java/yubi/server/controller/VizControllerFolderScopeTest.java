package yubi.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.core.entity.Folder;
import yubi.security.base.ResourceType;
import yubi.server.artifact.ArtifactTaskWebMapper;
import yubi.server.base.params.FolderCreateParam;
import yubi.server.service.VizService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VizControllerFolderScopeTest {

    private VizService vizService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        vizService = mock(VizService.class);
        mvc = MockMvcBuilders.standaloneSetup(new VizController(
                vizService, mock(ArtifactTaskWebMapper.class))).build();
    }

    @Test
    void 按资源类型查询独立目录树() throws Exception {
        when(vizService.listViz("org-1", ResourceType.DASHBOARD))
                .thenReturn(List.of());

        mvc.perform(get("/viz/folders")
                        .param("orgId", "org-1")
                        .param("resourceType", "DASHBOARD"))
                .andExpect(status().isOk());

        verify(vizService).listViz("org-1", ResourceType.DASHBOARD);
    }

    @Test
    void 创建目录时传递所属资源类型() throws Exception {
        Folder created = new Folder();
        created.setId("folder-1");
        when(vizService.createFolder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(created);

        mvc.perform(post("/viz/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "销售看板",
                                  "orgId": "org-1",
                                  "resourceType": "DASHBOARD",
                                  "parentId": null,
                                  "index": 0
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<FolderCreateParam> captor =
                ArgumentCaptor.forClass(FolderCreateParam.class);
        verify(vizService).createFolder(captor.capture());
        assertEquals(ResourceType.DASHBOARD, captor.getValue().getResourceType());
    }

    @Test
    void 创建目录时资源类型必填() throws Exception {
        mvc.perform(post("/viz/folders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "未归属目录",
                                  "orgId": "org-1",
                                  "parentId": null,
                                  "index": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}

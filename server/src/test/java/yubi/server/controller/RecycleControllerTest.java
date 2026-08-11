package yubi.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.recycle.RecycleItemPreflight;
import yubi.server.recycle.RecyclePreflight;
import yubi.server.recycle.RecyclePreflightCommand;
import yubi.server.recycle.RecycleResourceType;
import yubi.server.recycle.RecycleService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecycleControllerTest {

    private RecycleService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RecycleService.class);
        YuBiSecurityManager securityManager = mock(YuBiSecurityManager.class);
        User user = new User();
        user.setId("user-1");
        when(securityManager.getCurrentUser()).thenReturn(user);
        when(securityManager.isOrgOwner("org-1")).thenReturn(false);
        mvc = MockMvcBuilders.standaloneSetup(
                new RecycleController(service, securityManager)).build();
    }

    @Test
    void shouldPreflightOnlyTheResourceTypeBoundToCurrentModule() throws Exception {
        when(service.preflight(any(), any())).thenReturn(new RecyclePreflight(
                "token-1",
                Instant.parse("2026-08-07T06:05:00Z"),
                List.of(RecycleItemPreflight.ready("source-1"))
        ));

        mvc.perform(post("/organizations/org-1/recycle/SOURCE/preflight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootIds\":[\"source-1\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.operationToken").value("token-1"))
                .andExpect(jsonPath("$.data.items[0].rootId").value("source-1"));

        verify(service).preflight(
                yubi.server.recycle.RecycleAccess.authenticated("user-1", "org-1", false),
                new RecyclePreflightCommand(RecycleResourceType.SOURCE, List.of("source-1")));
    }
}

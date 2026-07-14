package yubi.server.controller;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.entity.Download;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.ShareDownloadParam;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DownloadTaskContractTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @SuppressWarnings("unchecked")
    void shouldSerializeOnlyStableDownloadTaskFields() throws Exception {
        Download entity = new Download();
        entity.setId("task-1");
        entity.setName("orders.xlsx");
        entity.setStatus((byte) 1);
        entity.setPath("/internal/download/orders.xlsx");
        entity.setCreateBy("user-1");
        entity.setOwnerId("session-secret-digest");
        entity.setShareId("share-1");
        entity.setFailureCode("INTERNAL_FAILURE");

        String json = objectMapper.writeValueAsString(DownloadTaskDTO.from(entity));
        Map<String, Object> fields = objectMapper.readValue(json, Map.class);

        assertEquals(Set.of("id", "name", "status"), fields.keySet());
        assertFalse(json.contains("internal"));
        assertFalse(json.contains("createBy"));
        assertFalse(json.contains("owner"));
        assertFalse(json.contains("shareId"));
        assertFalse(json.contains("failure"));
    }

    @Test
    void shouldRejectClientOwnerOverridesForAuthenticatedAndSharedRequests() {
        assertThrows(Exception.class, () -> objectMapper.readValue("""
                {"fileName":"orders","downloadParams":[],"ownerId":"user-2"}
                """, DownloadCreateParam.class));
        assertThrows(Exception.class, () -> objectMapper.readValue("""
                {"fileName":"orders","downloadParams":[],"clientId":"forged"}
                """, ShareDownloadParam.class));
        assertThrows(Exception.class, () -> objectMapper.readValue("""
                {"fileName":"orders","downloadParams":[],"subject":"user-2"}
                """, ShareDownloadParam.class));
        assertThrows(Exception.class, () -> objectMapper.readValue("""
                {"fileName":"orders","downloadParams":[],"organization":"org-2"}
                """, ShareDownloadParam.class));
    }
}

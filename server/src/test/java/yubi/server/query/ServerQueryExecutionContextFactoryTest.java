package yubi.server.query;

import org.junit.jupiter.api.Test;
import yubi.core.entity.User;
import yubi.query.domain.QueryModels.Channel;
import yubi.security.manager.YuBiSecurityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServerQueryExecutionContextFactoryTest {

    @Test
    void shouldInjectCurrentSubjectAndTrustedOrganizationForMetadata() {
        YuBiSecurityManager securityManager = mock(YuBiSecurityManager.class);
        User user = new User();
        user.setId("user-1");
        when(securityManager.getCurrentUser()).thenReturn(user);

        var context = new ServerQueryExecutionContextFactory(securityManager).forMetadata("org-1");

        assertEquals(Channel.AUTHENTICATED, context.channel());
        assertEquals("user-1", context.subjectId());
        assertEquals("org-1", context.organizationId());
        assertNotNull(context.correlationId());
    }
}

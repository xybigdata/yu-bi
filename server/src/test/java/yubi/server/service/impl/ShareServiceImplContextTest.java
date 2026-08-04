package yubi.server.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.artifact.ArtifactTasks;
import yubi.server.service.AsyncAccessLogService;
import yubi.server.service.DataProviderService;
import yubi.server.service.RoleService;
import yubi.server.service.VizService;

import static org.mockito.Mockito.mock;

class ShareServiceImplContextTest {

    @Test
    void 应在存在测试辅助构造器时仍能确定生产注入入口() {
        new ApplicationContextRunner()
                .withBean(DataProviderService.class, () -> mock(DataProviderService.class))
                .withBean(VizService.class, () -> mock(VizService.class))
                .withBean(ShareMapperExt.class, () -> mock(ShareMapperExt.class))
                .withBean(RoleService.class, () -> mock(RoleService.class))
                .withBean(UserMapperExt.class, () -> mock(UserMapperExt.class))
                .withBean(ArtifactTasks.class, () -> mock(ArtifactTasks.class))
                .withBean(YuBiSecurityManager.class, () -> mock(YuBiSecurityManager.class))
                .withBean(AsyncAccessLogService.class, () -> mock(AsyncAccessLogService.class))
                .withBean(RelRoleResourceMapperExt.class, () -> mock(RelRoleResourceMapperExt.class))
                .withBean(ShareServiceImpl.class)
                .run(context -> context.getBean(ShareServiceImpl.class));
    }
}

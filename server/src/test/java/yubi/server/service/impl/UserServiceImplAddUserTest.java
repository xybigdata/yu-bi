package yubi.server.service.impl;

import jakarta.validation.Validation;
import yubi.core.base.exception.ParamException;
import yubi.core.common.Application;
import yubi.core.common.MessageResolver;
import yubi.core.entity.User;
import yubi.core.mappers.ext.OrganizationMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.params.UserAddParam;
import yubi.server.service.MailService;
import yubi.server.service.OrgService;
import yubi.server.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplAddUserTest {

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        UserMapperExt userMapper = mock(UserMapperExt.class);
        OrganizationMapperExt orgMapper = mock(OrganizationMapperExt.class);
        OrgService orgService = mock(OrgService.class);
        RoleService roleService = mock(RoleService.class);
        MailService mailService = mock(MailService.class);
        YuBiSecurityManager securityManager = mock(YuBiSecurityManager.class);

        AtomicReference<User> insertedUser = new AtomicReference<>();
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            insertedUser.set(invocation.getArgument(0));
            return 1;
        });
        when(userMapper.selectByUsername("new-user")).thenAnswer(ignored -> insertedUser.get());

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.tenant-management-mode")).thenReturn("platform");
        new Application().setApplicationContext(applicationContext);
        new MessageResolver().setMessageSource(new StaticMessageSource());

        service = new UserServiceImpl(userMapper, orgMapper, orgService, roleService, mailService);
        service.setSecurityManager(securityManager);
    }

    @Test
    void 新增成员时空密码必须被拒绝而不是替换为固定默认密码() {
        UserAddParam param = new UserAddParam();
        param.setUsername("new-user");
        param.setEmail("new-user@example.com");

        assertThrows(ParamException.class, () -> service.addUserToOrg(param, "org-1"));
    }

    @Test
    void 新增成员参数的空白密码违反BeanValidation契约() {
        UserAddParam param = new UserAddParam();
        param.setUsername("new-user");
        param.setPassword("   ");

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(param);

            assertTrue(violations.stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));
        }
    }
}

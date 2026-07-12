/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.context.support.StaticMessageSource;
import yubi.core.common.Application;
import yubi.core.common.MessageResolver;
import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.SelectColumn;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.ResourceType;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.manager.YuBiSecurityManager;
import yubi.security.util.AESUtil;
import yubi.server.base.params.ShareAuthorizedToken;
import yubi.server.base.params.ShareToken;
import yubi.server.base.params.ViewExecuteParam;
import yubi.server.service.DataProviderService;
import yubi.server.service.DownloadService;
import yubi.server.service.RoleService;
import yubi.server.service.VizService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShareServiceImplCharacterizationTest {

    private static final String TOKEN_SECRET = "agent-ready-query-characterization";

    private DataProviderService dataProviderService;

    private YuBiSecurityManager securityManager;

    private ShareServiceImpl shareService;

    @BeforeEach
    void setUp() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.security.token.secret", "d@a$t%a^r&a*t"))
                .thenReturn(TOKEN_SECRET);
        new Application().setApplicationContext(applicationContext);

        MessageSource messageSource = new StaticMessageSource();
        new MessageResolver().setMessageSource(messageSource);

        dataProviderService = mock(DataProviderService.class);
        securityManager = mock(YuBiSecurityManager.class);
        shareService = new ShareServiceImpl(
                dataProviderService,
                mock(VizService.class),
                mock(DownloadService.class),
                mock(ShareMapperExt.class),
                mock(RoleService.class),
                mock(UserMapperExt.class));
        shareService.setSecurityManager(securityManager);
    }

    @Test
    void shouldCharacterizeSharedQueryIdentityAndPermissionBypass() throws Exception {
        ViewExecuteParam request = executableRequest("view-1");
        Dataframe expected = new Dataframe("shared-query-result");
        when(dataProviderService.execute(request, false)).thenReturn(expected);

        Dataframe result = shareService.execute(tokenFor("view-1", "share-owner"), request);

        assertSame(expected, result);
        verify(securityManager).runAs("share-owner");
        verify(dataProviderService).execute(request, false);
        verify(securityManager).releaseRunAs();
    }

    @Test
    void shouldReleaseSharedIdentityWhenQueryFails() throws Exception {
        ViewExecuteParam request = executableRequest("view-1");
        Exception failure = new Exception("query failed");
        when(dataProviderService.execute(request, false)).thenThrow(failure);

        Exception thrown = assertThrows(Exception.class,
                () -> shareService.execute(tokenFor("view-1", "share-owner"), request));

        assertSame(failure, thrown);
        verify(securityManager).runAs("share-owner");
        verify(securityManager).releaseRunAs();
    }

    @Test
    void shouldRejectSharedQueryTokenBoundToAnotherView() {
        ViewExecuteParam request = executableRequest("view-1");

        assertThrows(PermissionDeniedException.class,
                () -> shareService.execute(tokenFor("view-2", "share-owner"), request));

        verify(securityManager, never()).runAs("share-owner");
    }

    private static ViewExecuteParam executableRequest(String viewId) {
        ViewExecuteParam request = new ViewExecuteParam();
        request.setViewId(viewId);
        request.setColumns(List.of(SelectColumn.of("amount", "orders", "amount")));
        return request;
    }

    private static ShareToken tokenFor(String viewId, String permissionBy) {
        ShareAuthorizedToken authorizedToken = new ShareAuthorizedToken();
        authorizedToken.setVizType(ResourceType.VIEW);
        authorizedToken.setVizId(viewId);
        authorizedToken.setPermissionBy(permissionBy);
        return ShareToken.create(AESUtil.encrypt(authorizedToken, TOKEN_SECRET));
    }
}

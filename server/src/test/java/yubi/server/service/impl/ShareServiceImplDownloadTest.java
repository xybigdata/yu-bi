package yubi.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.Environment;
import yubi.core.common.Application;
import yubi.core.common.MessageResolver;
import yubi.core.entity.Download;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.ResourceType;
import yubi.security.util.AESUtil;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.base.params.ShareAuthorizedToken;
import yubi.server.base.params.ShareDownloadParam;
import yubi.server.base.params.ShareToken;
import yubi.server.service.DataProviderService;
import yubi.server.service.DownloadService;
import yubi.server.service.RoleService;
import yubi.server.service.VizService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShareServiceImplDownloadTest {

    private static final String TOKEN_SECRET = "agent-ready-query-download";

    private DownloadService downloadService;

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

        downloadService = mock(DownloadService.class);
        shareService = new ShareServiceImpl(mock(DataProviderService.class), mock(VizService.class), downloadService,
                mock(ShareMapperExt.class), mock(RoleService.class), mock(UserMapperExt.class));
    }

    @Test
    void shouldSubmitSharedDownloadUsingTheQueryDownloadRequest() {
        DownloadQueryRequest request = new DownloadQueryRequest();
        request.setViewId("view-1");
        request.setConcurrencyControlMode("DIRTYREAD");
        ShareDownloadParam param = new ShareDownloadParam();
        param.setFileName("orders.xlsx");
        param.setDownloadParams(List.of(request));
        param.setExecuteToken(Map.of("view-1", tokenFor("view-1")));
        Download expected = new Download();
        when(downloadService.submitDownloadTask(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("client-1"))).thenReturn(expected);

        Download result = shareService.createDownload("client-1", param);

        assertSame(expected, result);
        ArgumentCaptor<DownloadCreateParam> captor = ArgumentCaptor.forClass(DownloadCreateParam.class);
        verify(downloadService).submitDownloadTask(captor.capture(), org.mockito.ArgumentMatchers.eq("client-1"));
        DownloadQueryRequest submitted = captor.getValue().getDownloadParams().getFirst();
        assertSame(request, submitted);
        org.junit.jupiter.api.Assertions.assertEquals("DIRTYREAD", submitted.getConcurrencyControlMode());
    }

    private ShareToken tokenFor(String viewId) {
        ShareAuthorizedToken token = new ShareAuthorizedToken();
        token.setVizType(ResourceType.VIEW);
        token.setVizId(viewId);
        token.setPermissionBy("share-owner");
        return ShareToken.create(AESUtil.encrypt(token, TOKEN_SECRET));
    }
}

package yubi.server.service.impl;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.base.consts.ShareRowPermissionBy;
import yubi.core.base.consts.AttachmentType;
import yubi.core.base.exception.BaseException;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.common.Application;
import yubi.core.common.MessageResolver;
import yubi.core.entity.Dashboard;
import yubi.core.entity.Share;
import yubi.core.entity.Role;
import yubi.core.entity.User;
import yubi.core.entity.View;
import yubi.core.mappers.ext.DashboardMapperExt;
import yubi.core.mappers.ext.DatachartMapperExt;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.StoryboardMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.ResourceType;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.manager.YuBiSecurityManager;
import yubi.security.util.AESUtil;
import yubi.server.base.dto.DashboardDetail;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.base.params.ShareAuthorizedToken;
import yubi.server.base.params.ShareDownloadParam;
import yubi.server.base.params.ShareToken;
import yubi.server.service.DataProviderService;
import yubi.server.service.BaseCRUDService;
import yubi.server.service.DashboardService;
import yubi.server.service.DownloadFileResource;
import yubi.server.service.DownloadService;
import yubi.server.service.RoleService;
import yubi.server.service.ShareDownloadSession;
import yubi.server.service.ShareDownloadSessionManager;
import yubi.server.service.SharedDownloadContext;
import yubi.server.service.ShareVizAccess;
import yubi.server.service.VizService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShareServiceImplDownloadTest {

    private static final String TOKEN_SECRET = "download-share-security-test";
    private static final String SESSION_SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
    );

    private DownloadService downloadService;
    private ShareMapperExt shareMapper;
    private DashboardMapperExt dashboardMapper;
    private UserMapperExt userMapper;
    private VizService vizService;
    private YuBiSecurityManager securityManager;
    private ShareDownloadSessionManager sessionManager;
    private ShareServiceImpl shareService;
    private RoleService roleService;
    private ApplicationContext applicationContext;
    private Share share;
    private ShareDownloadSession session;

    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("yubi.security.token.secret", "d@a$t%a^r&a*t"))
                .thenReturn(TOKEN_SECRET);
        when(environment.getProperty("yubi.server.path-prefix")).thenReturn("/api/v1");
        new Application().setApplicationContext(applicationContext);

        MessageSource messageSource = new StaticMessageSource();
        new MessageResolver().setMessageSource(messageSource);

        downloadService = mock(DownloadService.class);
        shareMapper = mock(ShareMapperExt.class);
        dashboardMapper = mock(DashboardMapperExt.class);
        userMapper = mock(UserMapperExt.class);
        vizService = mock(VizService.class);
        securityManager = mock(YuBiSecurityManager.class);
        sessionManager = new ShareDownloadSessionManager(securityManager, SESSION_SECRET);
        roleService = mock(RoleService.class);
        shareService = new ShareServiceImpl(
                mock(DataProviderService.class),
                vizService,
                downloadService,
                shareMapper,
                roleService,
                userMapper,
                dashboardMapper,
                mock(DatachartMapperExt.class),
                mock(StoryboardMapperExt.class),
                sessionManager
        );
        shareService.setSecurityManager(securityManager);

        share = share(ShareAuthenticationMode.NONE);
        session = sessionFor(share, null);
        stubCurrentShare(share);
        User creator = new User();
        creator.setId("user-1");
        creator.setUsername("share-owner");
        when(userMapper.selectByPrimaryKey("user-1")).thenReturn(creator);
    }

    @Test
    void shouldCreateListAndDownloadWithinSameShareAndSession() {
        ShareDownloadParam param = downloadParam(tokenFor("share-1", "org-1", "view-1"));
        param.getDownloadParams().getFirst().setVizType(ResourceType.DATACHART);
        param.getDownloadParams().getFirst().setVizId("outside-datachart");
        DashboardDetail dashboardDetail = new DashboardDetail();
        dashboardDetail.setId("dashboard-1");
        dashboardDetail.setOrgId("org-1");
        dashboardDetail.setStatus((byte) 1);
        View view = new View();
        view.setId("view-1");
        view.setOrgId("org-1");
        view.setStatus((byte) 1);
        dashboardDetail.setViews(List.of(view));
        when(vizService.getDashboard("dashboard-1")).thenReturn(dashboardDetail);

        DownloadTaskDTO expected = new DownloadTaskDTO("task-1", "orders.xlsx", (byte) 0);
        when(downloadService.submitSharedDownloadTask(any(), any())).thenReturn(expected);

        DownloadTaskDTO created = shareService.createDownload("share-1", session, param);

        assertSame(expected, created);
        ArgumentCaptor<DownloadCreateParam> command = ArgumentCaptor.forClass(DownloadCreateParam.class);
        ArgumentCaptor<SharedDownloadContext> context = ArgumentCaptor.forClass(SharedDownloadContext.class);
        verify(downloadService).submitSharedDownloadTask(command.capture(), context.capture());
        assertEquals("DIRTYREAD", command.getValue().getDownloadParams().getFirst().getConcurrencyControlMode());
        assertEquals(AttachmentType.EXCEL, command.getValue().getDownloadType());
        assertNull(command.getValue().getDownloadParams().getFirst().getVizType());
        assertNull(command.getValue().getDownloadParams().getFirst().getVizId());
        assertEquals("share-1", context.getValue().shareId());
        assertEquals("a".repeat(64), context.getValue().sessionDigest());
        assertEquals("share-owner", context.getValue().executionUsername());
        verify(securityManager).runAs("share-owner");
        verify(securityManager).releaseRunAs();

        when(downloadService.listSharedDownloadTasks(any())).thenReturn(List.of(expected));
        assertEquals(List.of(expected), shareService.listDownloadTask("share-1", session));

        DownloadFileResource file = new DownloadFileResource(
                new java.io.ByteArrayInputStream("content".getBytes()),
                "orders.xlsx"
        );
        when(downloadService.downloadSharedFile(any(), any())).thenReturn(file);
        assertSame(file, shareService.download("share-1", session, "task-1"));
    }

    @Test
    void shouldRejectSharedImageAndPdfBeforeVizQueryOrDownloadCapabilities() {
        for (AttachmentType type : List.of(AttachmentType.IMAGE, AttachmentType.PDF)) {
            ShareDownloadParam param = downloadParam(tokenFor("share-1", "org-1", "view-1"));
            param.setDownloadType(type);
            param.getDownloadParams().getFirst().setVizType(ResourceType.DATACHART);
            param.getDownloadParams().getFirst().setVizId("outside-datachart");

            NotAllowedException exception = assertThrows(
                    NotAllowedException.class,
                    () -> shareService.createDownload("share-1", session, param)
            );

            assertEquals("分享下载请求无效", exception.getMessage());
        }

        verifyNoInteractions(vizService, downloadService);
    }

    @Test
    void shouldIssueViewTokensBoundToCurrentTopShareAfterAuthorization() {
        DashboardDetail dashboardDetail = new DashboardDetail();
        dashboardDetail.setId("dashboard-1");
        dashboardDetail.setOrgId("org-1");
        View view = new View();
        view.setId("view-1");
        view.setOrgId("org-1");
        view.setStatus((byte) 1);
        dashboardDetail.setViews(List.of(view));
        when(vizService.getDashboard("dashboard-1")).thenReturn(dashboardDetail);
        ShareToken request = new ShareToken();
        request.setId("share-1");

        ShareVizAccess access = shareService.getShareViz(request, null);

        ShareAuthorizedToken viewToken = AESUtil.decrypt(
                access.detail().getExecuteToken().get("view-1").getAuthorizedToken(),
                TOKEN_SECRET,
                ShareAuthorizedToken.class
        );
        assertEquals("share-1", access.shareId());
        assertEquals(ShareAuthenticationMode.NONE, access.authenticationMode());
        assertEquals("share-1", viewToken.getShareId());
        assertEquals(ResourceType.DASHBOARD, viewToken.getShareVizType());
        assertEquals("dashboard-1", viewToken.getShareVizId());
        assertEquals("org-1", viewToken.getOrganizationId());
        assertEquals("share-owner", viewToken.getPermissionBy());
        verify(securityManager).runAs("share-owner");
        verify(securityManager).releaseRunAs();
    }

    @Test
    void shouldIgnoreForgedLoginCredentialsAndRequireCurrentAuthenticatedUser() {
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        ShareToken request = new ShareToken();
        request.setId("share-1");
        request.setUsername("administrator");
        request.setPassword("forged-password");
        when(securityManager.getCurrentUser()).thenReturn(null);

        assertThrows(BaseException.class, () -> shareService.getShareViz(request, null));

        verify(vizService, never()).getDashboard(any());
    }

    @Test
    void shouldRejectCrossShareSessionExpiredRevokedAndWrongAuthenticationMode() {
        ShareDownloadSession crossShare = new ShareDownloadSession(
                "share-2",
                "a".repeat(64),
                session.securityFingerprint(),
                ShareAuthenticationMode.NONE,
                null
        );
        assertThrows(NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", crossShare));

        share.setExpiryDate(new Date(System.currentTimeMillis() - 1_000));
        assertThrows(BaseException.class,
                () -> shareService.listDownloadTask("share-1", session));

        share.setExpiryDate(null);
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(null);
        assertThrows(NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", session));

        share.setAuthenticationMode(ShareAuthenticationMode.CODE.name());
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(share);
        assertThrows(NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", session));

        verifyNoInteractions(downloadService);
    }

    @Test
    void shouldRejectWrongTargetOrganizationAndArchivedTarget() {
        Dashboard dashboard = dashboard("org-2", (byte) 1);
        when(dashboardMapper.selectByPrimaryKey("dashboard-1")).thenReturn(dashboard);
        assertThrows(NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", session));

        dashboard.setOrgId("org-1");
        dashboard.setStatus((byte) 0);
        assertThrows(NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", session));

        verifyNoInteractions(downloadService);
    }

    @Test
    void shouldRejectAnonymousOrDifferentUserForLoginShare() {
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        ShareDownloadSession loginSession = sessionFor(share, "user-1");
        when(securityManager.getCurrentUser()).thenReturn(null);
        assertThrows(BaseException.class,
                () -> shareService.listDownloadTask("share-1", loginSession));

        User other = new User();
        other.setId("user-2");
        other.setUsername("other");
        when(securityManager.getCurrentUser()).thenReturn(other);
        assertThrows(NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", loginSession));

        verifyNoInteractions(downloadService);
    }

    @Test
    void shouldReplaceUnexpectedShareValidationFailureWithSafeMessage() {
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        ShareDownloadSession loginSession = sessionFor(share, "user-1");
        when(securityManager.getCurrentUser()).thenThrow(new IllegalStateException(
                "jdbc:mysql://secret/db?password=raw-secret /internal/path"
        ));

        NotAllowedException exception = assertThrows(
                NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", loginSession)
        );

        assertEquals("分享下载会话无效", exception.getMessage());
        verifyNoInteractions(downloadService);
    }

    @Test
    void shouldRejectExecuteTokenIssuedForDifferentLoginSubject() {
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        share.setRowPermissionBy(ShareRowPermissionBy.VISITOR.name());
        User visitor = new User();
        visitor.setId("user-2");
        visitor.setUsername("visitor");
        when(securityManager.getCurrentUser()).thenReturn(visitor);
        when(securityManager.isOrgOwner("org-1")).thenReturn(true);
        ShareDownloadSession loginSession = sessionFor(share, "user-2");
        ShareDownloadParam param = downloadParam(tokenFor(
                "share-1",
                "org-1",
                "view-1",
                ShareAuthenticationMode.LOGIN,
                "another-visitor"
        ));

        assertThrows(NotAllowedException.class,
                () -> shareService.createDownload("share-1", loginSession, param));

        verify(downloadService, never()).submitSharedDownloadTask(any(), any());
    }

    @Test
    void shouldAllowRoleAuthorizedVisitorDownloadWithCreatorMembershipAndVisitorQuerySubject() {
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        share.setRowPermissionBy(ShareRowPermissionBy.VISITOR.name());
        share.setRoles("[\"rrole-1\"]");

        User visitor = new User();
        visitor.setId("user-2");
        visitor.setUsername("visitor");
        when(securityManager.getCurrentUser()).thenReturn(visitor);
        when(securityManager.isOrgOwner("org-1")).thenReturn(false);

        DashboardService dashboardService = mock(DashboardService.class);
        when(dashboardService.getEntityClz()).thenReturn(Dashboard.class);
        doThrow(new PermissionDeniedException("dashboard read denied"))
                .when(dashboardService).retrieve("dashboard-1", true);
        when(applicationContext.getBeansOfType(BaseCRUDService.class))
                .thenReturn(Map.of("dashboardService", dashboardService));

        Role permittedRole = new Role();
        permittedRole.setId("role-1");
        when(roleService.listUserRoles("org-1", "user-2")).thenReturn(List.of(permittedRole));

        DashboardDetail dashboardDetail = dashboardDetailWithView();
        dashboardDetail.setStatus((byte) 1);
        when(vizService.getDashboard("dashboard-1")).thenReturn(dashboardDetail);
        ShareDownloadSession loginSession = sessionFor(share, "user-2");
        ShareDownloadParam param = downloadParam(tokenFor(
                "share-1",
                "org-1",
                "view-1",
                ShareAuthenticationMode.LOGIN,
                "visitor"
        ));
        when(downloadService.submitSharedDownloadTask(any(), any()))
                .thenReturn(new DownloadTaskDTO("task-1", "orders.xlsx", (byte) 0));

        shareService.createDownload("share-1", loginSession, param);

        ArgumentCaptor<SharedDownloadContext> context = ArgumentCaptor.forClass(SharedDownloadContext.class);
        verify(downloadService).submitSharedDownloadTask(any(), context.capture());
        assertEquals("visitor", context.getValue().executionUsername());
        assertEquals("user-2", context.getValue().querySubjectId());
        assertEquals("user-1", context.getValue().auditCreatorId());
        assertEquals("org-1", context.getValue().organizationId());
        verify(dashboardService).retrieve("dashboard-1", true);
        verify(roleService, org.mockito.Mockito.atLeastOnce()).listUserRoles("org-1", "user-2");
        verify(securityManager).runAs("share-owner");
    }

    @Test
    void shouldRejectVisitorOutsideShareRoleBeforeMembershipOrDownloadCapability() {
        share.setAuthenticationMode(ShareAuthenticationMode.LOGIN.name());
        share.setRowPermissionBy(ShareRowPermissionBy.VISITOR.name());
        share.setRoles("[\"rrole-allowed\"]");

        User visitor = new User();
        visitor.setId("user-2");
        visitor.setUsername("visitor");
        when(securityManager.getCurrentUser()).thenReturn(visitor);
        when(securityManager.isOrgOwner("org-1")).thenReturn(false);

        DashboardService dashboardService = mock(DashboardService.class);
        when(dashboardService.getEntityClz()).thenReturn(Dashboard.class);
        doThrow(new PermissionDeniedException("dashboard read denied"))
                .when(dashboardService).retrieve("dashboard-1", true);
        when(applicationContext.getBeansOfType(BaseCRUDService.class))
                .thenReturn(Map.of("dashboardService", dashboardService));

        Role unrelatedRole = new Role();
        unrelatedRole.setId("role-other");
        when(roleService.listUserRoles("org-1", "user-2")).thenReturn(List.of(unrelatedRole));
        ShareDownloadSession loginSession = sessionFor(share, "user-2");

        assertThrows(BaseException.class, () -> shareService.createDownload(
                "share-1",
                loginSession,
                downloadParam(tokenFor(
                        "share-1",
                        "org-1",
                        "view-1",
                        ShareAuthenticationMode.LOGIN,
                        "visitor"
                ))
        ));

        verifyNoInteractions(vizService, downloadService);
    }

    @Test
    void shouldRevokeCodeSessionBeforeVizAndDownloadCapabilitiesAfterCodeRotation() {
        share.setAuthenticationMode(ShareAuthenticationMode.CODE.name());
        share.setAuthenticationCode("old-code");
        DashboardDetail detail = dashboardDetailWithView();
        when(vizService.getDashboard("dashboard-1")).thenReturn(detail);

        ShareToken initialRequest = new ShareToken();
        initialRequest.setId("share-1");
        initialRequest.setAuthenticationCode("old-code");
        ShareVizAccess initialAccess = shareService.getShareViz(initialRequest, null);
        MockHttpServletResponse oldResponse = new MockHttpServletResponse();
        sessionManager.issue(
                "share-1",
                ShareAuthenticationMode.CODE,
                initialAccess.securityFingerprint(),
                null,
                new MockHttpServletRequest(),
                oldResponse
        );
        MockHttpServletRequest oldRequest = new MockHttpServletRequest();
        oldRequest.setCookies(new Cookie(
                sessionManager.cookieName("share-1"),
                cookieValue(oldResponse.getHeader("Set-Cookie"))
        ));
        ShareDownloadSession oldSession = sessionManager.require("share-1", oldRequest);
        ShareToken authorizedReuse = initialAccess.detail().getExecuteToken().get("view-1");

        share.setAuthenticationCode("new-code");
        reset(vizService, downloadService);

        assertThrows(
                NotAllowedException.class,
                () -> shareService.getShareViz(authorizedReuse, oldSession)
        );
        assertThrows(
                NotAllowedException.class,
                () -> shareService.createDownload(
                        "share-1",
                        oldSession,
                        downloadParam(tokenFor("share-1", "org-1", "view-1"))
                )
        );
        assertThrows(
                NotAllowedException.class,
                () -> shareService.listDownloadTask("share-1", oldSession)
        );
        assertThrows(
                NotAllowedException.class,
                () -> shareService.download("share-1", oldSession, "task-1")
        );
        verifyNoInteractions(vizService, downloadService);

        when(vizService.getDashboard("dashboard-1")).thenReturn(detail);
        ShareToken newRequest = new ShareToken();
        newRequest.setId("share-1");
        newRequest.setAuthenticationCode("new-code");
        ShareVizAccess newAccess = shareService.getShareViz(newRequest, null);
        MockHttpServletResponse newResponse = new MockHttpServletResponse();
        ShareDownloadSession newSession = sessionManager.issue(
                "share-1",
                ShareAuthenticationMode.CODE,
                newAccess.securityFingerprint(),
                null,
                oldRequest,
                newResponse
        );
        assertNotEquals(oldSession.sessionDigest(), newSession.sessionDigest());
        assertNotEquals(oldSession.securityFingerprint(), newSession.securityFingerprint());
        DownloadTaskDTO task = new DownloadTaskDTO("task-1", "orders.xlsx", (byte) 1);
        when(downloadService.listSharedDownloadTasks(any())).thenReturn(List.of(task));
        assertEquals(List.of(task), shareService.listDownloadTask("share-1", newSession));
    }

    @Test
    void shouldRejectEmptyCrossShareCrossOrganizationAndRemovedViewCreation() {
        ShareDownloadParam empty = new ShareDownloadParam();
        assertThrows(NotAllowedException.class,
                () -> shareService.createDownload("share-1", session, empty));

        ShareDownloadParam crossShare = downloadParam(tokenFor("share-2", "org-1", "view-1"));
        assertThrows(BaseException.class,
                () -> shareService.createDownload("share-1", session, crossShare));

        ShareDownloadParam crossOrganization = downloadParam(tokenFor("share-1", "org-2", "view-1"));
        assertThrows(BaseException.class,
                () -> shareService.createDownload("share-1", session, crossOrganization));

        DashboardDetail dashboardDetail = new DashboardDetail();
        dashboardDetail.setId("dashboard-1");
        dashboardDetail.setOrgId("org-1");
        dashboardDetail.setStatus((byte) 1);
        View crossOrganizationView = new View();
        crossOrganizationView.setId("view-1");
        crossOrganizationView.setOrgId("org-2");
        crossOrganizationView.setStatus((byte) 1);
        dashboardDetail.setViews(List.of(crossOrganizationView));
        when(vizService.getDashboard("dashboard-1")).thenReturn(dashboardDetail);
        ShareDownloadParam removedView = downloadParam(tokenFor("share-1", "org-1", "view-1"));
        assertThrows(NotAllowedException.class,
                () -> shareService.createDownload("share-1", session, removedView));

        View archivedView = new View();
        archivedView.setId("view-1");
        archivedView.setOrgId("org-1");
        archivedView.setStatus((byte) 0);
        dashboardDetail.setViews(List.of(archivedView));
        assertThrows(NotAllowedException.class,
                () -> shareService.createDownload("share-1", session, removedView));

        dashboardDetail.setViews(List.of());
        assertThrows(NotAllowedException.class,
                () -> shareService.createDownload("share-1", session, removedView));

        verify(downloadService, never()).submitSharedDownloadTask(any(), any());
    }

    private void stubCurrentShare(Share current) {
        when(shareMapper.selectByPrimaryKey("share-1")).thenReturn(current);
        when(dashboardMapper.selectByPrimaryKey("dashboard-1"))
                .thenReturn(dashboard("org-1", (byte) 1));
    }

    private Share share(ShareAuthenticationMode mode) {
        Share value = new Share();
        value.setId("share-1");
        value.setOrgId("org-1");
        value.setVizType(ResourceType.DASHBOARD.name());
        value.setVizId("dashboard-1");
        value.setAuthenticationMode(mode.name());
        value.setRowPermissionBy(ShareRowPermissionBy.CREATOR.name());
        value.setCreateBy("user-1");
        return value;
    }

    private Dashboard dashboard(String orgId, byte status) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId("dashboard-1");
        dashboard.setOrgId(orgId);
        dashboard.setStatus(status);
        return dashboard;
    }

    private ShareDownloadParam downloadParam(ShareToken token) {
        DownloadQueryRequest request = new DownloadQueryRequest();
        request.setViewId("view-1");
        request.setConcurrencyControlMode("DIRTYREAD");
        ShareDownloadParam param = new ShareDownloadParam();
        param.setFileName("orders.xlsx");
        param.setDownloadParams(List.of(request));
        param.setExecuteToken(Map.of("view-1", token));
        return param;
    }

    private DashboardDetail dashboardDetailWithView() {
        DashboardDetail dashboardDetail = new DashboardDetail();
        dashboardDetail.setId("dashboard-1");
        dashboardDetail.setOrgId("org-1");
        dashboardDetail.setStatus((byte) 1);
        View view = new View();
        view.setId("view-1");
        view.setOrgId("org-1");
        view.setStatus((byte) 1);
        dashboardDetail.setViews(List.of(view));
        return dashboardDetail;
    }

    private ShareDownloadSession sessionFor(Share currentShare, String subjectId) {
        return new ShareDownloadSession(
                currentShare.getId(),
                "a".repeat(64),
                sessionManager.securityFingerprint(currentShare),
                ShareAuthenticationMode.valueOf(currentShare.getAuthenticationMode()),
                subjectId
        );
    }

    private String cookieValue(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private ShareToken tokenFor(String shareId, String organizationId, String viewId) {
        return tokenFor(
                shareId,
                organizationId,
                viewId,
                ShareAuthenticationMode.NONE,
                "share-owner"
        );
    }

    private ShareToken tokenFor(String shareId,
                                String organizationId,
                                String viewId,
                                ShareAuthenticationMode authenticationMode,
                                String permissionBy) {
        ShareAuthorizedToken token = new ShareAuthorizedToken();
        token.setCreateBy("user-1");
        token.setPermissionBy(permissionBy);
        token.setVizType(ResourceType.VIEW);
        token.setVizId(viewId);
        token.setShareId(shareId);
        token.setShareVizType(ResourceType.DASHBOARD);
        token.setShareVizId("dashboard-1");
        token.setOrganizationId(organizationId);
        token.setShareAuthenticationMode(authenticationMode);
        return ShareToken.create(AESUtil.encrypt(token, TOKEN_SECRET));
    }
}

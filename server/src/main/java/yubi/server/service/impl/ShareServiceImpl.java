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

import yubi.core.base.consts.Const;
import yubi.core.base.consts.AttachmentType;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.base.consts.ShareRowPermissionBy;
import yubi.core.base.exception.BaseException;
import yubi.core.base.exception.Exceptions;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.common.Application;
import yubi.core.common.UUIDGenerator;
import yubi.core.data.provider.StdSqlOperator;
import yubi.core.entity.*;
import yubi.core.mappers.ext.ShareMapperExt;
import yubi.core.mappers.ext.DashboardMapperExt;
import yubi.core.mappers.ext.DatachartMapperExt;
import yubi.core.mappers.ext.StoryboardMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.ResourceType;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.util.AESUtil;
import yubi.security.util.SecurityUtils;
import yubi.server.base.dto.DashboardDetail;
import yubi.server.base.dto.DatachartDetail;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.dto.ShareInfo;
import yubi.server.base.dto.StoryboardDetail;
import yubi.server.base.params.*;
import yubi.server.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShareServiceImpl extends BaseService implements ShareService {

    private final DataProviderService dataProviderService;

    private final VizService vizService;

    private final DownloadService downloadService;

    private final ShareMapperExt shareMapper;

    private final RoleService roleService;

    private final UserMapperExt userMapperExt;

    private final DashboardMapperExt dashboardMapper;

    private final DatachartMapperExt datachartMapper;

    private final StoryboardMapperExt storyboardMapper;

    private final ShareDownloadSessionManager shareDownloadSessionManager;

    public ShareServiceImpl(DataProviderService dataProviderService,
                            VizService vizService,
                            DownloadService downloadService,
                            ShareMapperExt shareMapper,
                            RoleService roleService,
                            UserMapperExt userMapperExt,
                            DashboardMapperExt dashboardMapper,
                            DatachartMapperExt datachartMapper,
                            StoryboardMapperExt storyboardMapper,
                            ShareDownloadSessionManager shareDownloadSessionManager) {
        this.dataProviderService = dataProviderService;
        this.vizService = vizService;
        this.downloadService = downloadService;
        this.shareMapper = shareMapper;
        this.roleService = roleService;
        this.userMapperExt = userMapperExt;
        this.dashboardMapper = dashboardMapper;
        this.datachartMapper = datachartMapper;
        this.storyboardMapper = storyboardMapper;
        this.shareDownloadSessionManager = shareDownloadSessionManager;
    }

    @Override
    public ShareToken createShare(ShareCreateParam createParam) {
        return createShare(getCurrentUser().getId(), createParam);
    }

    @Override
    public ShareToken createShare(String shareUser, ShareCreateParam createParam) {
        validateShareParam(createParam);
        String orgId = null;
        switch (createParam.getVizType()) {
            case STORYBOARD:
                Storyboard storyboard = retrieve(createParam.getVizId(), Storyboard.class, true);
                if (storyboard != null) {
                    orgId = storyboard.getOrgId();
                }
                break;
            case DASHBOARD:
                Dashboard dashboard = retrieve(createParam.getVizId(), Dashboard.class, true);
                if (dashboard != null) {
                    orgId = dashboard.getOrgId();
                }
                break;
            case DATACHART:
                Datachart datachart = retrieve(createParam.getVizId(), Datachart.class, true);
                if (datachart != null) {
                    orgId = datachart.getOrgId();
                }
                break;
            default:
                Exceptions.tr(BaseException.class, "message.share.unsupported", createParam.getVizType().name());
        }

        if (createParam.getAuthenticationMode().equals(ShareAuthenticationMode.CODE)) {
            createParam.setAuthenticationCode(SecurityUtils.randomPassword());
        }

        Share share = new Share();
        BeanUtils.copyProperties(createParam, share);
        share.setCreateBy(shareUser);
        Set<String> roles = new HashSet<>();
        if (!CollectionUtils.isEmpty(createParam.getRoles())) {
            for (String role : createParam.getRoles()) {
                roles.add('r' + role);
            }
        }
        if (!CollectionUtils.isEmpty(createParam.getUsers())) {
            for (String userId : createParam.getUsers()) {
                Role role = roleService.getPerUserRole(orgId, userId);
                roles.add('u' + role.getId());
            }
        }

        share.setRoles(writeRoles(roles));
        share.setVizType(createParam.getVizType().name());
        share.setAuthenticationMode(createParam.getAuthenticationMode().name());
        share.setRowPermissionBy(createParam.getRowPermissionBy().name());
        share.setOrgId(orgId);
        share.setVizType(createParam.getVizType().name());
        share.setId(UUIDGenerator.generate());
        share.setCreateTime(new Date());
        shareMapper.insert(share);

        ShareToken shareToken = new ShareToken();
        BeanUtils.copyProperties(createParam, shareToken);
        shareToken.setId(share.getId());
        return shareToken;
    }

    @Override
    public ShareInfo updateShare(ShareUpdateParam updateParam) {
        Share update = retrieve(updateParam.getId());
        requirePermission(update, Const.MANAGE);

        BeanUtils.copyProperties(updateParam, update);
        if (updateParam.getRowPermissionBy() != null) {
            update.setRowPermissionBy(updateParam.getRowPermissionBy().name());
        } else {
            update.setRowPermissionBy(ShareRowPermissionBy.CREATOR.name());
        }

        if (ShareAuthenticationMode.CODE.equals(updateParam.getAuthenticationMode()) && !ShareAuthenticationMode.CODE.name().equals(update.getAuthenticationMode())) {
            update.setAuthenticationCode(SecurityUtils.randomPassword());
        }
        update.setAuthenticationMode(updateParam.getAuthenticationMode().name());

        Set<String> roleIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(updateParam.getRoles())) {
            for (String role : updateParam.getRoles()) {
                roleIds.add('r' + role);
            }
        }
        if (!CollectionUtils.isEmpty(updateParam.getUsers())) {
            for (String user : updateParam.getUsers()) {
                Role role = roleService.getPerUserRole(update.getOrgId(), user);
                roleIds.add('u' + role.getId());
            }
        }

        update.setRoles(writeRoles(roleIds));
        update.setUpdateBy(getCurrentUser().getId());
        update.setUpdateTime(new Date());
        shareMapper.updateByPrimaryKey(update);

        ShareInfo shareInfo = new ShareInfo();
        BeanUtils.copyProperties(update, shareInfo);
        shareInfo.setId(update.getId());
        shareInfo.setAuthenticationMode(updateParam.getAuthenticationMode());
        //返回基本信息,防止用户更新后再次点击操作基础信息丢失
        shareInfo.setRowPermissionBy(updateParam.getRowPermissionBy());
        shareInfo.setRoles(updateParam.getRoles());
        shareInfo.setUsers(updateParam.getUsers());

        return shareInfo;
    }

    @Override
    public List<ShareInfo> listShare(String vizId) {
        List<Share> shares = shareMapper.selectByViz(vizId);
        if (CollectionUtils.isEmpty(shares)) {
            return Collections.emptyList();
        }
        // check permission
        String vizType = shares.get(0).getVizType();
        switch (ResourceType.valueOf(vizType)) {
            case STORYBOARD:
                retrieve(vizId, Storyboard.class, true);
                break;
            case DASHBOARD:
                retrieve(vizId, Dashboard.class, true);
                break;
            case DATACHART:
                retrieve(vizId, Datachart.class, true);
                break;
            default:
                Exceptions.tr(BaseException.class, "message.share.unsupported", vizType);

        }
        return shares.parallelStream().map(share -> {
            ShareInfo shareInfo = new ShareInfo();
            BeanUtils.copyProperties(share, shareInfo);
            shareInfo.setAuthenticationMode(ShareAuthenticationMode.valueOf(share.getAuthenticationMode()));
            shareInfo.setRowPermissionBy(ShareRowPermissionBy.valueOf(share.getRowPermissionBy()));
            shareInfo.setRoles(new LinkedHashSet<>());
            shareInfo.setUsers(new LinkedHashSet<>());
            if (StringUtils.isNotBlank(share.getRoles())) {
                List<String> roles = readRoles(share.getRoles());
                for (String str : roles) {
                    if (str.charAt(0) == 'r') {
                        shareInfo.getRoles().add(str.substring(1));
                    } else {
                        User user = roleService.getPerUserRoleUser(str.substring(1));
                        shareInfo.getUsers().add(user.getId());
                    }
                }
            }
            return shareInfo;
        }).collect(Collectors.toList());
    }

    private String writeRoles(Set<String> roles) {
        return OBJECT_MAPPER.writeValueAsString(roles);
    }

    private List<String> readRoles(String roles) {
        return OBJECT_MAPPER.readerForListOf(String.class).readValue(roles);
    }

    @Override
    public ShareVizAccess getShareViz(ShareToken shareToken, ShareDownloadSession existingSession) {
        ParsedShare parsed = parseToken(shareToken);
        validateExpiration(parsed.share());
        String securityFingerprint = shareDownloadSessionManager.securityFingerprint(parsed.share());
        if (StringUtils.isNotBlank(shareToken.getAuthorizedToken())) {
            validateExistingShareSession(parsed, existingSession, securityFingerprint);
        }
        ShareVizDetail detail = getVizDetail(parsed.authorizedToken());
        return new ShareVizAccess(
                detail,
                parsed.share().getId(),
                ShareAuthenticationMode.valueOf(parsed.share().getAuthenticationMode()),
                parsed.authenticatedSubjectId(),
                securityFingerprint
        );
    }

    private void validateExistingShareSession(ParsedShare parsed,
                                              ShareDownloadSession existingSession,
                                              String securityFingerprint) {
        ShareAuthenticationMode authenticationMode;
        try {
            authenticationMode = ShareAuthenticationMode.valueOf(parsed.share().getAuthenticationMode());
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享下载会话无效");
        }
        if (existingSession == null
                || !Objects.equals(parsed.share().getId(), existingSession.shareId())
                || !Objects.equals(authenticationMode, existingSession.authenticationMode())
                || !Objects.equals(parsed.authenticatedSubjectId(), existingSession.authenticatedSubjectId())
                || !Objects.equals(securityFingerprint, existingSession.securityFingerprint())) {
            throw new NotAllowedException("分享下载会话无效");
        }
    }

    @Override
    public DownloadTaskDTO createDownload(String shareId,
                                          ShareDownloadSession session,
                                          ShareDownloadParam downloadParam) {
        Share share = validateShareDownloadAccess(shareId, session);
        if (downloadParam == null
                || !AttachmentType.EXCEL.equals(Objects.requireNonNullElse(
                        downloadParam.getDownloadType(),
                        AttachmentType.EXCEL
                ))
                || CollectionUtils.isEmpty(downloadParam.getDownloadParams())
                || CollectionUtils.isEmpty(downloadParam.getExecuteToken())) {
            throw new NotAllowedException("分享下载请求无效");
        }
        String executionUsername = null;
        for (DownloadQueryRequest param : downloadParam.getDownloadParams()) {
            if (param == null || StringUtils.isBlank(param.getViewId())) {
                throw new NotAllowedException("分享下载请求无效");
            }
            ShareToken executeToken = downloadParam.getExecuteToken().get(param.getViewId());
            ShareAuthorizedToken authorizedToken = validateExecutePermission(
                    executeToken == null ? null : executeToken.getAuthorizedToken(),
                    param,
                    share
            );
            if (executionUsername == null) {
                executionUsername = authorizedToken.getPermissionBy();
            } else if (!Objects.equals(executionUsername, authorizedToken.getPermissionBy())) {
                throw new NotAllowedException("分享下载请求无效");
            }
        }

        Set<String> currentViewIds = currentDashboardViewIds(share);
        boolean containsUnrelatedView = downloadParam.getDownloadParams().stream()
                .map(DownloadQueryRequest::getViewId)
                .anyMatch(viewId -> !currentViewIds.contains(viewId));
        if (containsUnrelatedView) {
            throw new NotAllowedException("分享下载请求无效");
        }

        DownloadCreateParam downloadCreateParam = new DownloadCreateParam();
        downloadCreateParam.setFileName(downloadParam.getFileName());
        downloadCreateParam.setDownloadParams(downloadParam.getDownloadParams().stream()
                .map(this::withoutUntrustedVizMetadata)
                .toList());
        downloadCreateParam.setDownloadType(AttachmentType.EXCEL);

        return downloadService.submitSharedDownloadTask(
                downloadCreateParam,
                sharedContext(share, session, executionUsername)
        );
    }

    @Override
    public List<DownloadTaskDTO> listDownloadTask(String shareId, ShareDownloadSession session) {
        Share share = validateShareDownloadAccess(shareId, session);
        return downloadService.listSharedDownloadTasks(sharedContext(share, session, null));
    }

    @Override
    public DownloadFileResource download(String shareId,
                                         ShareDownloadSession session,
                                         String downloadId) {
        Share share = validateShareDownloadAccess(shareId, session);
        return downloadService.downloadSharedFile(
                downloadId,
                sharedContext(share, session, null)
        );
    }

    @Override
    public Set<StdSqlOperator> supportedStdFunctions(ShareToken shareToken, String sourceId) {
        ParsedShare parsed = parseToken(shareToken);
        validateExpiration(parsed.share());
        return dataProviderService.supportedStdFunctions(sourceId);
    }

    private ShareVizDetail getVizDetail(ShareAuthorizedToken authorizedToken) {
        User user = userMapperExt.selectByPrimaryKey(authorizedToken.getCreateBy());
        if (user == null || StringUtils.isBlank(user.getUsername())) {
            throw new NotAllowedException("分享访问无效");
        }
        try {
            getSecurityManager().runAs(user.getUsername());
            return buildVizDetail(authorizedToken);
        } finally {
            getSecurityManager().releaseRunAs();
        }
    }

    private ShareVizDetail buildVizDetail(ShareAuthorizedToken authorizedToken) {
        ShareVizDetail shareVizDetail = new ShareVizDetail();

        shareVizDetail.setVizType(authorizedToken.getVizType());

        Object vizDetail = null;

        Map<String, ShareToken> subVizToken = null;

        Map<String, ShareToken> executeToken = null;

        switch (shareVizDetail.getVizType()) {
            case STORYBOARD:
                StoryboardDetail storyboard = vizService.getStoryboard(authorizedToken.getVizId());
                vizDetail = storyboard;
                subVizToken = storyboard.getStorypages().stream().collect(Collectors.toMap(Storypage::getId, storypage -> {
                    ShareAuthorizedToken subShare = new ShareAuthorizedToken();
                    BeanUtils.copyProperties(authorizedToken, subShare);
                    subShare.setVizId(storypage.getRelId());
                    subShare.setVizType(ResourceType.valueOf(storypage.getRelType()));
                    return ShareToken.create(AESUtil.encrypt(subShare, Application.getTokenSecret()));
                }));
                break;
            case DASHBOARD:
                DashboardDetail dashboard = vizService.getDashboard(authorizedToken.getVizId());
                vizDetail = dashboard;
                executeToken = dashboard.getViews().stream().collect(Collectors.toMap(View::getId, view -> {
                    ShareAuthorizedToken subShare = new ShareAuthorizedToken();
                    BeanUtils.copyProperties(authorizedToken, subShare);
                    subShare.setVizType(ResourceType.VIEW);
                    subShare.setVizId(view.getId());
                    return ShareToken.create(AESUtil.encrypt(subShare, Application.getTokenSecret()));
                }));
                break;
            case DATACHART:
                DatachartDetail datachart = vizService.getDatachart(authorizedToken.getVizId());
                vizDetail = datachart;
                shareVizDetail.setVizDetail(datachart);
                ShareAuthorizedToken subShare = new ShareAuthorizedToken();
                BeanUtils.copyProperties(authorizedToken, subShare);
                subShare.setVizType(ResourceType.VIEW);
                subShare.setVizId(datachart.getViewId());
                if (datachart.getViewId() != null) {
                    executeToken = new HashMap<>();
                    executeToken.put(datachart.getViewId(), ShareToken.create(AESUtil.encrypt(subShare, Application.getTokenSecret())));
                }
                break;
            default:
                Exceptions.tr(BaseException.class, "message.share.unsupported", shareVizDetail.getVizType().name());

        }
        shareVizDetail.setVizDetail(vizDetail);
        shareVizDetail.setSubVizToken(subVizToken);
        shareVizDetail.setExecuteToken(executeToken);
        shareVizDetail.setShareToken(ShareToken.create(AESUtil.encrypt(authorizedToken, Application.getTokenSecret())));
        return shareVizDetail;
    }

    private ShareAuthorizedToken validateExecutePermission(String authorizedToken,
                                                           DownloadQueryRequest executeParam,
                                                           Share share) {
        if (StringUtils.isBlank(authorizedToken)) {
            Exceptions.tr(PermissionDeniedException.class, "message.provider.execute.permission.denied");
        }
        ShareAuthorizedToken shareAuthorizedToken;
        try {
            shareAuthorizedToken = AESUtil.decrypt(
                    authorizedToken,
                    Application.getTokenSecret(),
                    ShareAuthorizedToken.class
            );
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享下载请求无效");
        }
        if (shareAuthorizedToken == null
                || executeParam == null
                || !ResourceType.VIEW.equals(shareAuthorizedToken.getVizType())
                || !Objects.equals(shareAuthorizedToken.getVizId(), executeParam.getViewId())
                || !Objects.equals(shareAuthorizedToken.getShareId(), share.getId())
                || !Objects.equals(shareAuthorizedToken.getShareVizType(), ResourceType.valueOf(share.getVizType()))
                || !Objects.equals(shareAuthorizedToken.getShareVizId(), share.getVizId())
                || !Objects.equals(shareAuthorizedToken.getOrganizationId(), share.getOrgId())
                || !Objects.equals(
                        shareAuthorizedToken.getShareAuthenticationMode(),
                        ShareAuthenticationMode.valueOf(share.getAuthenticationMode())
                )
                || StringUtils.isBlank(shareAuthorizedToken.getPermissionBy())) {
            Exceptions.tr(PermissionDeniedException.class, "message.provider.execute.permission.denied");
        }
        validateAuthorizedToken(shareAuthorizedToken, share);
        return shareAuthorizedToken;
    }

    private void validateExpiration(Share share) {
        if (share == null || (share.getExpiryDate() != null && new Date().after(share.getExpiryDate()))) {
            Exceptions.tr(BaseException.class, "message.share.expired");
        }
    }

    private User authenticationShare(Share share, ShareToken shareToken) {
        ShareAuthenticationMode authenticationMode = ShareAuthenticationMode.valueOf(share.getAuthenticationMode());
        switch (authenticationMode) {
            case NONE:
                return null;
            case CODE:
                if (StringUtils.isEmpty(shareToken.getAuthenticationCode()) || !shareToken.getAuthenticationCode().equals(share.getAuthenticationCode())) {
                    Exceptions.tr(BaseException.class, "message.share.permission.denied");
                }
                return null;
            case LOGIN:
                return authenticateLoginShare(share);
            default:
                Exceptions.tr(BaseException.class, "message.share.permission.denied");
                return null;
        }
    }

    private User authenticateLoginShare(Share share) {
        User user = getSecurityManager().getCurrentUser();
        if (user == null || StringUtils.isBlank(user.getId())) {
            Exceptions.tr(BaseException.class, "message.share.permission.denied");
        }
        if (ShareRowPermissionBy.CREATOR.name().equals(share.getRowPermissionBy())) {
            return user;
        }
        if (getSecurityManager().isOrgOwner(share.getOrgId())) {
            return user;
        }
        try {
            checkVizReadPermission(ResourceType.valueOf(share.getVizType()), share.getVizId());
            return user;
        } catch (PermissionDeniedException ignored) {
            // 继续按分享配置中的角色白名单校验。
        }
        if (StringUtils.isBlank(share.getRoles())) {
            Exceptions.tr(BaseException.class, "message.share.permission.denied");
        }
        List<Role> roles = roleService.listUserRoles(share.getOrgId(), user.getId());
        if (CollectionUtils.isEmpty(roles)) {
            Exceptions.tr(BaseException.class, "message.share.permission.denied");
        }
        Set<String> roleIdList = roles.stream().map(BaseEntity::getId).collect(Collectors.toSet());
        List<String> permittedRoles = readRoles(share.getRoles()).stream()
                .filter(value -> value != null && value.length() > 1)
                .map(value -> value.substring(1))
                .toList();
        if (Collections.disjoint(roleIdList, permittedRoles)) {
            Exceptions.tr(BaseException.class, "message.share.permission.denied");
        }
        return user;
    }

    @Override
    public void requirePermission(Share entity, int permission) {

    }

    private void validateShareParam(ShareCreateParam createParam) {
        if (ShareRowPermissionBy.VISITOR.equals(createParam.getRowPermissionBy())) {
            if (!ShareAuthenticationMode.LOGIN.equals(createParam.getAuthenticationMode())) {
                Exceptions.msg("The authentication mode must be LOGIN");
            }
        } else {
            createParam.setRowPermissionBy(ShareRowPermissionBy.CREATOR);
        }
    }

    private void checkVizReadPermission(ResourceType vizType, String vizId) {
        switch (vizType) {
            case DASHBOARD:
                retrieve(vizId, Dashboard.class, true);
                break;
            case DATACHART:
                retrieve(vizId, Datachart.class, true);
                break;
            case STORYBOARD:
                retrieve(vizId, Storyboard.class, true);
                break;
            default:
                Exceptions.tr(BaseException.class, "message.share.unsupported", vizType.name());
        }
    }

    private ParsedShare parseToken(ShareToken shareToken) {
        if (shareToken == null) {
            throw new NotAllowedException("分享访问无效");
        }
        ShareAuthorizedToken authorizedToken = null;
        if (StringUtils.isNotBlank(shareToken.getAuthorizedToken())) {
            try {
                authorizedToken = AESUtil.decrypt(
                        shareToken.getAuthorizedToken(),
                        Application.getTokenSecret(),
                        ShareAuthorizedToken.class
                );
            } catch (RuntimeException exception) {
                throw new NotAllowedException("分享访问无效");
            }
        }

        String shareId = StringUtils.defaultIfBlank(
                shareToken.getId(),
                authorizedToken == null ? null : authorizedToken.getShareId()
        );
        Share share = loadCurrentShare(shareId);
        validateExpiration(share);
        validateShareTarget(share);

        User authenticatedUser;
        if (authorizedToken == null) {
            authenticatedUser = authenticationShare(share, shareToken);
            authorizedToken = createAuthorizedToken(share, authenticatedUser);
        } else {
            validateAuthorizedToken(authorizedToken, share);
            authenticatedUser = ShareAuthenticationMode.LOGIN.name().equals(share.getAuthenticationMode())
                    ? authenticateLoginShare(share)
                    : null;
        }
        return new ParsedShare(
                share,
                authorizedToken,
                authenticatedUser == null ? null : authenticatedUser.getId()
        );
    }

    private ShareAuthorizedToken createAuthorizedToken(Share share, User authenticatedUser) {
        ShareAuthorizedToken authorizedToken = new ShareAuthorizedToken();
        BeanUtils.copyProperties(share, authorizedToken);
        ResourceType topType = ResourceType.valueOf(share.getVizType());
        authorizedToken.setVizType(topType);
        authorizedToken.setShareId(share.getId());
        authorizedToken.setShareVizType(topType);
        authorizedToken.setShareVizId(share.getVizId());
        authorizedToken.setOrganizationId(share.getOrgId());
        authorizedToken.setShareAuthenticationMode(
                ShareAuthenticationMode.valueOf(share.getAuthenticationMode())
        );

        User shareCreator = loadShareCreator(share);
        authorizedToken.setCreateBy(shareCreator.getId());

        if (ShareRowPermissionBy.CREATOR.name().equals(share.getRowPermissionBy())) {
            authorizedToken.setPermissionBy(shareCreator.getUsername());
        } else {
            if (authenticatedUser == null || StringUtils.isBlank(authenticatedUser.getUsername())) {
                throw new NotAllowedException("分享访问无效");
            }
            authorizedToken.setPermissionBy(authenticatedUser.getUsername());
        }
        return authorizedToken;
    }

    private void validateAuthorizedToken(ShareAuthorizedToken token, Share share) {
        ResourceType topType;
        ShareAuthenticationMode authenticationMode;
        try {
            topType = ResourceType.valueOf(share.getVizType());
            authenticationMode = ShareAuthenticationMode.valueOf(share.getAuthenticationMode());
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享访问无效");
        }
        if (token == null
                || !Objects.equals(token.getShareId(), share.getId())
                || !Objects.equals(token.getShareVizType(), topType)
                || !Objects.equals(token.getShareVizId(), share.getVizId())
                || !Objects.equals(token.getOrganizationId(), share.getOrgId())
                || !Objects.equals(token.getShareAuthenticationMode(), authenticationMode)
                || !Objects.equals(token.getCreateBy(), normalizedShareCreatorId(share))
                || !Objects.equals(token.getPermissionBy(), expectedPermissionBy(share))
                || StringUtils.isAnyBlank(token.getCreateBy(), token.getPermissionBy())) {
            throw new NotAllowedException("分享访问无效");
        }
    }

    private Share validateShareDownloadAccess(String shareId, ShareDownloadSession session) {
        try {
            return validateShareDownloadAccessInternal(shareId, session);
        } catch (BaseException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享下载会话无效");
        }
    }

    private Share validateShareDownloadAccessInternal(String shareId, ShareDownloadSession session) {
        if (session == null || !Objects.equals(shareId, session.shareId())) {
            throw new NotAllowedException("分享下载会话无效");
        }
        Share share = loadCurrentShare(shareId);
        validateExpiration(share);
        validateShareTarget(share);
        ShareAuthenticationMode authenticationMode;
        try {
            authenticationMode = ShareAuthenticationMode.valueOf(share.getAuthenticationMode());
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享下载会话无效");
        }
        if (!ResourceType.DASHBOARD.name().equals(share.getVizType())
                || !Objects.equals(authenticationMode, session.authenticationMode())
                || !Objects.equals(
                        shareDownloadSessionManager.securityFingerprint(share),
                        session.securityFingerprint()
                )) {
            throw new NotAllowedException("分享下载会话无效");
        }
        if (ShareAuthenticationMode.LOGIN.equals(authenticationMode)) {
            User user = authenticateLoginShare(share);
            if (!Objects.equals(user.getId(), session.authenticatedSubjectId())) {
                throw new NotAllowedException("分享下载会话无效");
            }
        } else if (session.authenticatedSubjectId() != null) {
            throw new NotAllowedException("分享下载会话无效");
        }
        return share;
    }

    private Share loadCurrentShare(String shareId) {
        if (StringUtils.isBlank(shareId)) {
            throw new NotAllowedException("分享访问无效");
        }
        Share share;
        try {
            share = shareMapper.selectByPrimaryKey(shareId);
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享访问无效");
        }
        if (share == null) {
            throw new NotAllowedException("分享访问无效");
        }
        return share;
    }

    private User loadShareCreator(Share share) {
        try {
            User user = userMapperExt.selectByPrimaryKey(normalizedShareCreatorId(share));
            if (user == null || StringUtils.isAnyBlank(user.getId(), user.getUsername())) {
                throw new NotAllowedException("分享访问无效");
            }
            return user;
        } catch (NotAllowedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享访问无效");
        }
    }

    private String normalizedShareCreatorId(Share share) {
        String createBy = share.getCreateBy();
        if (StringUtils.isBlank(createBy)) {
            throw new NotAllowedException("分享访问无效");
        }
        return createBy.startsWith(AttachmentService.SHARE_USER)
                ? createBy.substring(AttachmentService.SHARE_USER.length())
                : createBy;
    }

    private String expectedPermissionBy(Share share) {
        if (ShareRowPermissionBy.CREATOR.name().equals(share.getRowPermissionBy())) {
            return loadShareCreator(share).getUsername();
        }
        User currentUser = getSecurityManager().getCurrentUser();
        if (currentUser == null || StringUtils.isBlank(currentUser.getUsername())) {
            throw new NotAllowedException("分享访问无效");
        }
        return currentUser.getUsername();
    }

    private void validateShareTarget(Share share) {
        boolean valid;
        try {
            ResourceType type = ResourceType.valueOf(share.getVizType());
            valid = switch (type) {
                case DASHBOARD -> belongsToShare(dashboardMapper.selectByPrimaryKey(share.getVizId()), share);
                case DATACHART -> belongsToShare(datachartMapper.selectByPrimaryKey(share.getVizId()), share);
                case STORYBOARD -> belongsToShare(storyboardMapper.selectByPrimaryKey(share.getVizId()), share);
                default -> false;
            };
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享访问无效");
        }
        if (!valid) {
            throw new NotAllowedException("分享访问无效");
        }
    }

    private boolean belongsToShare(BaseEntity entity, Share share) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Dashboard dashboard) {
            return dashboard.getStatus() != null
                    && !Byte.valueOf((byte) 0).equals(dashboard.getStatus())
                    && Objects.equals(dashboard.getOrgId(), share.getOrgId());
        }
        if (entity instanceof Datachart datachart) {
            return datachart.getStatus() != null
                    && !Byte.valueOf((byte) 0).equals(datachart.getStatus())
                    && Objects.equals(datachart.getOrgId(), share.getOrgId());
        }
        if (entity instanceof Storyboard storyboard) {
            return storyboard.getStatus() != null
                    && !Byte.valueOf((byte) 0).equals(storyboard.getStatus())
                    && Objects.equals(storyboard.getOrgId(), share.getOrgId());
        }
        return false;
    }

    private Set<String> currentDashboardViewIds(Share share) {
        User shareCreator = loadShareCreator(share);
        try {
            getSecurityManager().runAs(shareCreator.getUsername());
            DashboardDetail dashboard = vizService.getDashboard(share.getVizId());
            if (dashboard == null
                    || !Objects.equals(dashboard.getId(), share.getVizId())
                    || !Objects.equals(dashboard.getOrgId(), share.getOrgId())
                    || dashboard.getStatus() == null
                    || Byte.valueOf((byte) 0).equals(dashboard.getStatus())) {
                throw new NotAllowedException("分享下载请求无效");
            }
            return Optional.ofNullable(dashboard.getViews()).orElseGet(Collections::emptyList).stream()
                    .peek(view -> {
                        if (view == null
                                || StringUtils.isBlank(view.getId())
                                || !Objects.equals(view.getOrgId(), share.getOrgId())
                                || !Byte.valueOf(Const.DATA_STATUS_ACTIVE).equals(view.getStatus())) {
                            throw new NotAllowedException("分享下载请求无效");
                        }
                    })
                    .map(View::getId)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (NotAllowedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new NotAllowedException("分享下载请求无效");
        } finally {
            try {
                getSecurityManager().releaseRunAs();
            } catch (RuntimeException ignored) {
                // 清理失败不记录会话或令牌。
            }
        }
    }

    private DownloadQueryRequest withoutUntrustedVizMetadata(DownloadQueryRequest request) {
        DownloadQueryRequest trusted = new DownloadQueryRequest();
        BeanUtils.copyProperties(request, trusted);
        trusted.setVizType(null);
        trusted.setVizId(null);
        return trusted;
    }

    private SharedDownloadContext sharedContext(Share share,
                                                ShareDownloadSession session,
                                                String executionUsername) {
        return new SharedDownloadContext(
                share.getId(),
                session.sessionDigest(),
                executionUsername,
                normalizedShareCreatorId(share),
                sharedQuerySubjectId(share, session),
                share.getOrgId()
        );
    }

    private String sharedQuerySubjectId(Share share, ShareDownloadSession session) {
        if (ShareRowPermissionBy.VISITOR.name().equals(share.getRowPermissionBy())) {
            if (!ShareAuthenticationMode.LOGIN.name().equals(share.getAuthenticationMode())
                    || StringUtils.isBlank(session.authenticatedSubjectId())) {
                throw new NotAllowedException("分享下载请求无效");
            }
            return session.authenticatedSubjectId();
        }
        return normalizedShareCreatorId(share);
    }

    private record ParsedShare(
            Share share,
            ShareAuthorizedToken authorizedToken,
            String authenticatedSubjectId
    ) {
    }

}

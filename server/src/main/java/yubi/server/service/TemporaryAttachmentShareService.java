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

package yubi.server.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.base.consts.ShareRowPermissionBy;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.common.Application;
import yubi.core.entity.Folder;
import yubi.core.entity.User;
import yubi.security.base.ResourceType;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.base.params.ShareCreateParam;
import yubi.server.base.params.ShareToken;

import java.util.Date;

@Slf4j
@Service
public class TemporaryAttachmentShareService {

    private static final String TEMPORARY_SHARE_UNAVAILABLE = "临时下载分享不可用";

    private final YuBiSecurityManager securityManager;
    private final ShareService shareService;
    private final FolderService folderService;

    public TemporaryAttachmentShareService(YuBiSecurityManager securityManager,
                                           ShareService shareService,
                                           FolderService folderService) {
        this.securityManager = securityManager;
        this.shareService = shareService;
        this.folderService = folderService;
    }

    public <T> T withTemporaryShare(DownloadCreateParam downloadParam,
                                    TemporaryShareWork<T> work) throws Exception {
        if (downloadParam == null || downloadParam.getDownloadParams() == null
                || downloadParam.getDownloadParams().isEmpty() || work == null) {
            throw new NotAllowedException(TEMPORARY_SHARE_UNAVAILABLE);
        }
        DownloadQueryRequest query = downloadParam.getDownloadParams().getFirst();
        if (query == null || StringUtils.isBlank(query.getVizId())) {
            throw new NotAllowedException(TEMPORARY_SHARE_UNAVAILABLE);
        }
        User currentUser = securityManager.getCurrentUser();
        if (currentUser == null || StringUtils.isBlank(currentUser.getId())) {
            throw new NotAllowedException(TEMPORARY_SHARE_UNAVAILABLE);
        }
        Folder folder = folderService.retrieve(query.getVizId());
        if (folder == null || StringUtils.isAnyBlank(folder.getRelId(), folder.getRelType())) {
            throw new NotAllowedException(TEMPORARY_SHARE_UNAVAILABLE);
        }

        ShareCreateParam shareParam = new ShareCreateParam();
        shareParam.setVizId(folder.getRelId());
        shareParam.setVizType(ResourceType.valueOf(folder.getRelType()));
        shareParam.setExpiryDate(DateUtils.addHours(new Date(), 1));
        shareParam.setAuthenticationMode(ShareAuthenticationMode.NONE);
        shareParam.setRowPermissionBy(ShareRowPermissionBy.CREATOR);
        ShareToken share = shareService.createShare(
                AttachmentService.SHARE_USER + currentUser.getId(),
                shareParam
        );
        if (share == null || StringUtils.isBlank(share.getId())) {
            throw new NotAllowedException(TEMPORARY_SHARE_UNAVAILABLE);
        }

        Throwable primaryFailure = null;
        try {
            String url = Application.getWebRootURL()
                    + "/" + shareParam.getVizType().getShareRoute()
                    + "/" + share.getId()
                    + "?eager=true&type=" + ShareAuthenticationMode.NONE;
            return work.execute(url);
        } catch (Exception exception) {
            primaryFailure = exception;
            throw exception;
        } catch (Error error) {
            primaryFailure = error;
            throw error;
        } finally {
            boolean revoked = revoke(share.getId());
            if (primaryFailure == null && !revoked) {
                throw new NotAllowedException(TEMPORARY_SHARE_UNAVAILABLE);
            }
        }
    }

    private boolean revoke(String shareId) {
        try {
            if (!shareService.delete(shareId, false)) {
                log.warn("临时下载分享撤销失败");
                return false;
            }
            return true;
        } catch (RuntimeException exception) {
            log.warn("临时下载分享撤销失败");
            return false;
        }
    }

    @FunctionalInterface
    public interface TemporaryShareWork<T> {
        T execute(String url) throws Exception;
    }
}

package yubi.server.service.impl;

import yubi.core.base.consts.AttachmentType;
import yubi.core.base.consts.ShareAuthenticationMode;
import yubi.core.base.consts.ShareRowPermissionBy;
import yubi.core.common.Application;
import yubi.core.common.FileUtils;
import yubi.core.common.WebUtils;
import yubi.core.entity.Folder;
import yubi.security.base.ResourceType;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.base.params.ShareCreateParam;
import yubi.server.base.params.ShareToken;
import yubi.server.base.params.ViewExecuteParam;
import yubi.server.service.AttachmentService;
import yubi.server.service.FolderService;
import yubi.server.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

@Service("imageAttachmentService")
@Slf4j
public class AttachmentImageServiceImpl implements AttachmentService {

    protected final AttachmentType attachmentType = AttachmentType.IMAGE;

    private final YuBiSecurityManager securityManager;

    private final ShareService shareService;

    private final FolderService folderService;

    public AttachmentImageServiceImpl(YuBiSecurityManager yubiSecurityManager, ShareService shareService, FolderService folderService) {
        this.securityManager = yubiSecurityManager;
        this.shareService = shareService;
        this.folderService = folderService;
    }

    @Override
    public File getFile(DownloadCreateParam downloadCreateParam, String path, String fileName) throws Exception {
        ViewExecuteParam viewExecuteParam = downloadCreateParam.getDownloadParams().size() > 0 ? downloadCreateParam.getDownloadParams().get(0) : new ViewExecuteParam();
        String folderId = viewExecuteParam.getVizId();
        Folder folder = folderService.retrieve(folderId);
        ShareCreateParam shareCreateParam = new ShareCreateParam();
        shareCreateParam.setVizId(folder.getRelId());
        shareCreateParam.setVizType(ResourceType.valueOf(folder.getRelType()));
        shareCreateParam.setExpiryDate(DateUtils.addHours(new Date(), 1));
        shareCreateParam.setAuthenticationMode(ShareAuthenticationMode.NONE);
        shareCreateParam.setRowPermissionBy(ShareRowPermissionBy.CREATOR);
        ShareToken share = shareService.createShare(SHARE_USER + securityManager.getCurrentUser().getId(), shareCreateParam);

        String url = Application.getWebRootURL() + "/" + shareCreateParam.getVizType().getShareRoute() + "/" + share.getId() + "?eager=true&type=" + share.getAuthenticationMode();
        log.info("created share url: {} ", url);
        File target = WebUtils.screenShot2File(url, FileUtils.withBasePath(path), downloadCreateParam.getImageWidth());

        path = generateFileName(path, fileName, attachmentType);
        File file = new File(path);
        target.renameTo(file);
        log.info("create image file complete.");
        shareService.delete(share.getId(), false);
        return file;
    }

}

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
import yubi.server.base.params.DownloadQueryRequest;
import yubi.server.service.AttachmentService;
import yubi.server.service.FolderService;
import yubi.server.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

@Service("pdfAttachmentService")
@Slf4j
public class AttachmentPdfServiceImpl implements AttachmentService {

    protected final AttachmentType attachmentType = AttachmentType.PDF;

    private final YuBiSecurityManager securityManager;

    private final ShareService shareService;

    private final FolderService folderService;

    public AttachmentPdfServiceImpl(YuBiSecurityManager securityManager, ShareService shareService, FolderService folderService) {
        this.securityManager = securityManager;
        this.shareService = shareService;
        this.folderService = folderService;
    }

    @Override
    public File getFile(DownloadCreateParam downloadCreateParam, String path, String fileName) throws Exception {
        DownloadQueryRequest viewExecuteParam = downloadCreateParam.getDownloadParams().size() > 0 ? downloadCreateParam.getDownloadParams().get(0) : new DownloadQueryRequest();
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
        log.info("share url {} ", url);

        File imageFile = WebUtils.screenShot2File(url, FileUtils.withBasePath(path), downloadCreateParam.getImageWidth());
        File file = new File(generateFileName(path, fileName, attachmentType));
        createPDFFromImage(file.getPath(), imageFile.getPath());

        log.info("create pdf file complete.");
        imageFile.delete();
        shareService.delete(share.getId(), false);
        return file;
    }

    /**
     * 使用Apache pdfbox将图片生成pdf
     *
     * @param pdfPath
     * @param imagePath
     * @throws Exception
     */
    public void createPDFFromImage(String pdfPath, String imagePath) throws Exception {
        PDDocument doc = new PDDocument();
        PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, doc);
        float width = pdImage.getWidth();
        float height = pdImage.getHeight();
        PDPage page = new PDPage(new PDRectangle(width, height));
        doc.addPage(page);
        PDPageContentStream contents = new PDPageContentStream(doc, page);
        contents.drawImage(pdImage, 0, 0, width, height);
        contents.close();
        doc.save(pdfPath);
        doc.close();
    }
}

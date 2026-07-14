package yubi.server.service.impl;

import yubi.core.base.consts.AttachmentType;
import yubi.core.common.WebUtils;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.AttachmentService;
import yubi.server.service.TemporaryAttachmentShareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service("imageAttachmentService")
@Slf4j
public class AttachmentImageServiceImpl implements AttachmentService {

    protected final AttachmentType attachmentType = AttachmentType.IMAGE;

    private final TemporaryAttachmentShareService temporaryShareService;

    public AttachmentImageServiceImpl(TemporaryAttachmentShareService temporaryShareService) {
        this.temporaryShareService = temporaryShareService;
    }

    @Override
    public File getFile(DownloadCreateParam downloadCreateParam, String path, String fileName) throws Exception {
        return temporaryShareService.withTemporaryShare(downloadCreateParam, url -> {
            File target = WebUtils.screenShot2File(
                    url,
                    path,
                    downloadCreateParam.getImageWidth()
            );
            File file = new File(generateFileName(path, fileName, attachmentType));
            if (!target.renameTo(file)) {
                throw new IOException("图片下载文件生成失败");
            }
            log.info("create image file complete.");
            return file;
        });
    }

}

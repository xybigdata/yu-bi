package yubi.server.service.impl;

import yubi.core.base.consts.AttachmentType;
import yubi.core.common.WebUtils;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.AttachmentService;
import yubi.server.service.TemporaryAttachmentShareService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.File;

@Service("pdfAttachmentService")
@Slf4j
public class AttachmentPdfServiceImpl implements AttachmentService {

    protected final AttachmentType attachmentType = AttachmentType.PDF;

    private final TemporaryAttachmentShareService temporaryShareService;

    public AttachmentPdfServiceImpl(TemporaryAttachmentShareService temporaryShareService) {
        this.temporaryShareService = temporaryShareService;
    }

    @Override
    public File getFile(DownloadCreateParam downloadCreateParam, String path, String fileName) throws Exception {
        return temporaryShareService.withTemporaryShare(downloadCreateParam, url -> {
            File imageFile = null;
            try {
                imageFile = WebUtils.screenShot2File(
                        url,
                        path,
                        downloadCreateParam.getImageWidth()
                );
                File file = new File(generateFileName(path, fileName, attachmentType));
                createPDFFromImage(file.getPath(), imageFile.getPath());
                log.info("create pdf file complete.");
                return file;
            } finally {
                if (imageFile != null) {
                    imageFile.delete();
                }
            }
        });
    }

    /**
     * 使用Apache pdfbox将图片生成pdf
     *
     * @param pdfPath
     * @param imagePath
     * @throws Exception
     */
    public void createPDFFromImage(String pdfPath, String imagePath) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDImageXObject pdImage = PDImageXObject.createFromFile(imagePath, doc);
            float width = pdImage.getWidth();
            float height = pdImage.getHeight();
            PDPage page = new PDPage(new PDRectangle(width, height));
            doc.addPage(page);
            try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                contents.drawImage(pdImage, 0, 0, width, height);
            }
            doc.save(pdfPath);
        }
    }
}

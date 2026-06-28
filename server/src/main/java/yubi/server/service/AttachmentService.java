package yubi.server.service;

import yubi.core.base.consts.AttachmentType;
import yubi.core.base.consts.Const;
import yubi.core.base.exception.Exceptions;
import yubi.core.common.Application;
import yubi.core.common.FileUtils;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.impl.AttachmentExcelServiceImpl;
import yubi.server.service.impl.AttachmentImageServiceImpl;
import yubi.server.service.impl.AttachmentPdfServiceImpl;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.File;
import java.util.Calendar;

public interface AttachmentService {

    String SHARE_USER = "SCHEDULER_";

    File getFile(DownloadCreateParam downloadCreateParam, String path, String fileName) throws Exception;

    default String generateFileName(String path, String fileName, AttachmentType attachmentType) {
        path = FileUtils.withBasePath(path);
        String timeStr = DateFormatUtils.format(Calendar.getInstance(), Const.FILE_SUFFIX_DATE_FORMAT);
        String randomStr = org.apache.commons.lang3.RandomStringUtils.secure().nextNumeric(3);
        fileName = fileName + "_" + timeStr + "_" + randomStr + attachmentType.getSuffix();
        return FileUtils.concatPath(path, fileName);
    }

    static AttachmentService matchAttachmentService(AttachmentType type) {
        switch (type) {
            case EXCEL:
                return Application.getBean(AttachmentExcelServiceImpl.class);
            case IMAGE:
                return Application.getBean(AttachmentImageServiceImpl.class);
            case PDF:
                return Application.getBean(AttachmentPdfServiceImpl.class);
            default:
                Exceptions.msg("unsupported download type." + type);
                return null;
        }
    }

}

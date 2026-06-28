package yubi.server.base.dto;

import yubi.core.base.consts.AttachmentType;
import lombok.Data;

import java.io.File;

@Data
public class JobFile {

    private File file;

    private AttachmentType type;

}

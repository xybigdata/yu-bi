package yubi.server.service;

import yubi.core.entity.Datachart;
import yubi.core.entity.Folder;
import yubi.core.mappers.ext.DatachartMapperExt;
import yubi.server.base.dto.DatachartDetail;
import yubi.server.base.params.BaseCreateParam;
import yubi.server.base.transfer.model.DatachartResourceModel;
import yubi.server.base.transfer.model.DatachartTemplateModel;

public interface DatachartService extends VizCRUDService<Datachart, DatachartMapperExt>, ResourceTransferService<Folder, DatachartResourceModel, DatachartTemplateModel, Folder> {

    DatachartDetail getDatachartDetail(String datachartId);

    Folder createWithFolder(BaseCreateParam createParam);
}

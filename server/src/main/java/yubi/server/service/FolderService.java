package yubi.server.service;

import yubi.core.entity.Folder;
import yubi.core.mappers.FolderMapper;
import yubi.server.base.transfer.model.FolderTransferModel;

import java.util.List;

public interface FolderService extends BaseCRUDService<Folder, FolderMapper>, ResourceTransferService<Folder, FolderTransferModel, FolderTransferModel, Folder> {

    List<Folder> listOrgFolders(String orgId);

    boolean checkUnique(String orgId, String parentId, String name);

    Folder getVizFolder(String vizId, String relType);

    List<Folder> getAllParents(String folderId);

}
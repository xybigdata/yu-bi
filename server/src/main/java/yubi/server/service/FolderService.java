package yubi.server.service;

import yubi.core.entity.Folder;
import yubi.core.mappers.FolderMapper;
import yubi.security.base.ResourceType;
import yubi.server.base.transfer.model.FolderTransferModel;

import java.util.List;

public interface FolderService extends BaseCRUDService<Folder, FolderMapper>, ResourceTransferService<Folder, FolderTransferModel, FolderTransferModel, Folder> {

    List<Folder> listOrgFolders(String orgId);

    List<Folder> listOrgFolders(String orgId, ResourceType resourceType);

    boolean checkUnique(String orgId, String parentId, String name);

    boolean checkUnique(String orgId, String parentId, String name, ResourceType resourceType);

    void requireParentScope(String parentId, ResourceType resourceType);

    Folder getVizFolder(String vizId, String relType);

    List<Folder> getAllParents(String folderId);

}

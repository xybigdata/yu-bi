package yubi.server.service;

import yubi.core.entity.Storyboard;
import yubi.core.mappers.ext.StoryboardMapperExt;
import yubi.server.base.dto.StoryboardDetail;
import yubi.server.base.params.StoryboardBaseUpdateParam;

import java.util.List;

public interface StoryboardService extends VizCRUDService<Storyboard, StoryboardMapperExt> {

    List<Storyboard> listStoryBoards(String orgId);

    StoryboardDetail getStoryboard(String storyboardId);

    boolean updateBase(StoryboardBaseUpdateParam updateParam);

    boolean unarchive(String id, String newName, String parentId, double index);

    boolean checkUnique(String orgId, String parentId, String name);

}

package yubi.server.service;

import yubi.core.entity.Storypage;
import yubi.core.mappers.StorypageMapper;

import java.util.List;

public interface StorypageService extends BaseCRUDService<Storypage, StorypageMapper> {

    List<Storypage> listByStoryboard(String storyboardId);

    boolean deleteByStoryboard(String storyboardId);

}
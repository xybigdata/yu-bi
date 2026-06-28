package yubi.server.service;

import yubi.core.entity.UserSettings;
import yubi.core.mappers.ext.UserSettingsMapperExt;

import java.util.List;

public interface UserSettingService extends BaseCRUDService<UserSettings, UserSettingsMapperExt> {

    List<UserSettings> listUserSettings();

    boolean deleteByUserId(String userId);

}

/*
 * YuBi
 * <p>
 * Copyright 2021 (originally Datart by running-elephant)
 * Copyright 2024-2026 YuBi Contributors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yubi.server.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.common.Application;
import yubi.core.common.MessageResolver;
import yubi.core.entity.BaseEntity;
import yubi.core.entity.User;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

public class BaseService extends MessageResolver {

    protected static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    protected YuBiSecurityManager securityManager;

    protected AsyncAccessLogService accessLogService;

    protected RelRoleResourceMapperExt rrrMapper;

    private Map<Class<? extends BaseEntity>, BaseCRUDService<?, ?>> entityServiceMap;

    @Autowired
    public void setSecurityManager(YuBiSecurityManager yubiSecurityManager) {
        this.securityManager = yubiSecurityManager;
    }

    @Autowired
    public void setAccessLogService(AsyncAccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }

    @Autowired
    public void setRrrMapper(RelRoleResourceMapperExt rrrMapper) {
        this.rrrMapper = rrrMapper;
    }

    public AsyncAccessLogService getAccessLogService() {
        return accessLogService;
    }

    public User getCurrentUser() {
        return securityManager.getCurrentUser();
    }

    public RelRoleResourceMapperExt getRRRMapper() {
        return rrrMapper;
    }

    /**
     * 检查某类型的数据是否在对应表中存在，不存在则抛出 NotFoundException
     *
     * @param id  数据ID
     * @param clz 要调用的service类型
     */
    public void requireExists(String id, Class<? extends BaseEntity> clz) {
        getEntityService(clz).requireExists(id);
    }

    public <T> T retrieve(String id, Class<T> clz) {
        return clz.cast(getEntityService(clz).retrieve(id));
    }

    public <T> T retrieve(String id, Class<T> clz, boolean checkPermission) {
        return clz.cast(getEntityService(clz).retrieve(id, checkPermission));
    }

    private BaseCRUDService<?, ?> getEntityService(Class<?> clz) {
        if (CollectionUtils.isEmpty(entityServiceMap) || !entityServiceMap.containsKey(clz)) {
            entityServiceMap = new HashMap<>();
            Map<String, BaseCRUDService<?, ?>> beansOfType = getCrudServices();
            for (BaseCRUDService<?, ?> service : beansOfType.values()) {
                entityServiceMap.put(service.getEntityClz(), service);
            }
        }
        return entityServiceMap.get(clz);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, BaseCRUDService<?, ?>> getCrudServices() {
        // Spring's class-token lookup loses BaseCRUDService generic arguments at runtime.
        return (Map) Application.getContext().getBeansOfType(BaseCRUDService.class);
    }

    public MessageResolver getMessageResolver() {
        return this;
    }

    public YuBiSecurityManager getSecurityManager() {
        return securityManager;
    }

}

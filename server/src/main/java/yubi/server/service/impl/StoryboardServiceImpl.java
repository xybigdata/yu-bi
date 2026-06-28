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
package yubi.server.service.impl;

import yubi.core.base.consts.Const;
import yubi.core.base.exception.Exceptions;
import yubi.core.base.exception.ParamException;
import yubi.core.entity.*;
import yubi.core.mappers.ext.RelRoleResourceMapperExt;
import yubi.core.mappers.ext.StoryboardMapperExt;
import yubi.security.base.PermissionInfo;
import yubi.security.base.ResourceType;
import yubi.security.base.SubjectType;
import yubi.security.exception.PermissionDeniedException;
import yubi.security.manager.PermissionStringCodec;
import yubi.security.util.PermissionHelper;
import yubi.server.base.dto.StoryboardDetail;
import yubi.server.base.params.BaseCreateParam;
import yubi.server.base.params.StoryboardBaseUpdateParam;
import yubi.server.service.BaseService;
import yubi.server.service.RoleService;
import yubi.server.service.StoryboardService;
import yubi.server.service.StorypageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoryboardServiceImpl extends BaseService implements StoryboardService {

    private final RoleService roleService;

    private final StoryboardMapperExt storyboardMapper;

    private final StorypageService storypageService;

    private final RelRoleResourceMapperExt rrrMapper;

    public StoryboardServiceImpl(RoleService roleService,
                                 StoryboardMapperExt storyboardMapper,
                                 StorypageService storypageService, RelRoleResourceMapperExt rrrMapper) {
        this.roleService = roleService;
        this.storyboardMapper = storyboardMapper;
        this.storypageService = storypageService;
        this.rrrMapper = rrrMapper;
    }

    @Override
    public RoleService getRoleService() {
        return roleService;
    }

    @Override
    public List<Storyboard> listStoryBoards(String orgId) {
        List<Storyboard> storyboards = storyboardMapper.selectByOrg(orgId);

        Map<String, Storyboard> filtered = new HashMap<>();

        List<Storyboard> permitted = storyboards.stream().filter(storyboard -> {
            try {
                if (hasReadPermission(storyboard)) {
                    return true;
                } else {
                    filtered.put(storyboard.getId(), storyboard);
                    return false;
                }
            } catch (Exception e) {
                filtered.put(storyboard.getId(), storyboard);
                return false;
            }
        }).collect(Collectors.toList());

        while (!filtered.isEmpty()) {
            boolean updated = false;
            for (Storyboard storyboard : permitted) {
                Storyboard parent = filtered.remove(storyboard.getParentId());
                if (parent != null) {
                    permitted.add(parent);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                break;
            }
        }

        return permitted;
    }

    @Override
    public StoryboardDetail getStoryboard(String storyboardId) {
        StoryboardDetail storyboardDetail = new StoryboardDetail();
        Storyboard storyboard = retrieve(storyboardId);
        BeanUtils.copyProperties(storyboard, storyboardDetail);
        // story pages
        storyboardDetail.setStorypages(storypageService.listByStoryboard(storyboardId));
        // download permission
        storyboardDetail.setDownload(securityManager
                .hasPermission(PermissionHelper.vizPermission(storyboard.getOrgId(), "*", storyboardId, Const.DOWNLOAD)));

        return storyboardDetail;
    }

    @Override
    public boolean updateBase(StoryboardBaseUpdateParam updateParam) {
        Storyboard storyboard = retrieve(updateParam.getId(), Storyboard.class);
        requirePermission(storyboard, Const.MANAGE);
        if (!storyboard.getName().equals(updateParam.getName())) {
            //check name
            Storyboard check = new Storyboard();
            check.setParentId(updateParam.getParentId());
            check.setOrgId(storyboard.getOrgId());
            check.setName(updateParam.getName());
            checkUnique(check);
        }

        // update base info
        storyboard.setId(updateParam.getId());
        storyboard.setUpdateBy(getCurrentUser().getId());
        storyboard.setUpdateTime(new Date());
        storyboard.setName(updateParam.getName());
        storyboard.setParentId(updateParam.getParentId());
        storyboard.setIndex(updateParam.getIndex());
        return 1 == storyboardMapper.updateByPrimaryKey(storyboard);
    }

    @Override
    public boolean checkUnique(BaseEntity entity) {
        Storyboard sb = (Storyboard) entity;
        Storyboard check = new Storyboard();
        check.setName(sb.getName());
        check.setOrgId(sb.getOrgId());
        return StoryboardService.super.checkUnique(check);
    }

    @Override
    public void requirePermission(Storyboard storyboard, int permission) {
        if (securityManager.isOrgOwner(storyboard.getOrgId())) {
            return;
        }
        List<Role> roles = roleService.listUserRoles(storyboard.getOrgId(), getCurrentUser().getId());
        boolean hasPermission = roles.stream().anyMatch(role -> hasPermission(role, storyboard, permission));
        if (!hasPermission) {
            Exceptions.tr(PermissionDeniedException.class, "message.security.permission-denied",
                    ResourceType.STORYBOARD + ":" + storyboard.getName() + ":" + PermissionStringCodec.expandPermissions(permission));
        }
    }

    private boolean hasPermission(Role role, Storyboard storyboard, int permission) {
        if (storyboard.getId() == null || rrrMapper.countRolePermission(storyboard.getId(), role.getId()) == 0) {
            Storyboard parent = storyboardMapper.selectByPrimaryKey(storyboard.getParentId());
            if (parent == null) {
                return securityManager.hasPermission(PermissionHelper.vizPermission(storyboard.getOrgId(), role.getId(), ResourceType.STORYBOARD.name(), permission));
            } else {
                return hasPermission(role, parent, permission);
            }
        } else {
            return securityManager.hasPermission(PermissionHelper.vizPermission(storyboard.getOrgId(), role.getId(), storyboard.getId(), permission));
        }
    }

    @Override
    @Transactional
    public Storyboard create(BaseCreateParam createParam) {
        Storyboard storyboard = StoryboardService.super.create(createParam);
        grantDefaultPermission(storyboard);
        return storyboard;
    }

    @Override
    @Transactional
    public void grantDefaultPermission(Storyboard storyboard) {
        if (securityManager.isOrgOwner(storyboard.getOrgId())) {
            return;
        }
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setOrgId(storyboard.getOrgId());
        permissionInfo.setSubjectType(SubjectType.USER);
        permissionInfo.setSubjectId(getCurrentUser().getId());
        permissionInfo.setResourceType(ResourceType.VIZ);
        permissionInfo.setResourceId(storyboard.getId());
        permissionInfo.setPermission(Const.MANAGE);
        roleService.grantPermission(Collections.singletonList(permissionInfo));
    }

    @Override
    public void deleteReference(Storyboard storyboard) {
        storypageService.deleteByStoryboard(storyboard.getId());
    }

    private boolean hasReadPermission(Storyboard storyboard) {
        try {
            requirePermission(storyboard, Const.MANAGE);
            return true;
        } catch (Exception ignored) {
        }
        try {
            requirePermission(storyboard, Const.READ);
            return retrieve(storyboard.getId()).getStatus() == Const.VIZ_PUBLISH;
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public boolean unarchive(String id, String newName, String parentId, double index) {
        Storyboard storyboard = retrieve(id);
        requirePermission(storyboard, Const.MANAGE);

        //check name
        if (!storyboard.getName().equals(newName)) {
            checkUnique(storyboard.getOrgId(), parentId, newName);
        }

        // update status
        storyboard.setName(newName);
        storyboard.setParentId(parentId);
        storyboard.setStatus(Const.DATA_STATUS_ACTIVE);
        storyboard.setIndex(index);
        return 1 == storyboardMapper.updateByPrimaryKey(storyboard);
    }

    @Override
    public boolean checkUnique(String orgId, String parentId, String name) {
        if (!CollectionUtils.isEmpty(storyboardMapper.checkName(orgId, parentId, name))) {
            Exceptions.tr(ParamException.class, "error.param.exists.name");
        }
        return true;
    }
}

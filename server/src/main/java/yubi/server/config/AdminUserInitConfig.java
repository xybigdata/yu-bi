/*
 * YuBi
 * <p>
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

package yubi.server.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import yubi.core.common.UUIDGenerator;
import yubi.core.entity.Organization;
import yubi.core.entity.RelRoleUser;
import yubi.core.entity.Role;
import yubi.core.entity.User;
import yubi.core.mappers.ext.OrganizationMapperExt;
import yubi.core.mappers.ext.RelRoleUserMapperExt;
import yubi.core.mappers.ext.RoleMapperExt;
import yubi.core.mappers.ext.UserMapperExt;
import yubi.security.base.RoleType;

import java.util.Date;
import java.util.List;

/**
 * Startup configuration that ensures the configured admin user
 * is granted ORG_OWNER role in all organizations they belong to.
 * <p>
 * This is useful for demo environments where a specific user
 * needs full admin permissions on startup.
 */
@Slf4j
@Component
public class AdminUserInitConfig implements ApplicationListener<ApplicationReadyEvent> {

    private final UserMapperExt userMapper;
    private final OrganizationMapperExt organizationMapper;
    private final RoleMapperExt roleMapper;
    private final RelRoleUserMapperExt rruMapper;

    @Value("${yubi.user.admin-username:}")
    private String adminUsername;

    public AdminUserInitConfig(UserMapperExt userMapper,
                               OrganizationMapperExt organizationMapper,
                               RoleMapperExt roleMapper,
                               RelRoleUserMapperExt rruMapper) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
        this.roleMapper = roleMapper;
        this.rruMapper = rruMapper;
    }

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (StringUtils.isBlank(adminUsername)) {
            return;
        }

        User adminUser = userMapper.selectByNameOrEmail(adminUsername);
        if (adminUser == null) {
            log.warn("Configured admin user '{}' not found, skipping admin initialization.", adminUsername);
            return;
        }

        List<Organization> userOrgs = organizationMapper.listOrganizationsByUserId(adminUser.getId());
        if (userOrgs == null || userOrgs.isEmpty()) {
            // If the user has no organization, grant on all existing orgs
            userOrgs = organizationMapper.list();
        }

        if (userOrgs == null || userOrgs.isEmpty()) {
            log.info("No organizations found, skipping admin initialization for user '{}'.", adminUsername);
            return;
        }

        for (Organization org : userOrgs) {
            ensureOrgOwner(adminUser, org);
        }
    }

    private void ensureOrgOwner(User user, Organization org) {
        Role ownerRole = roleMapper.selectOrgOwnerRole(org.getId());

        if (ownerRole == null) {
            // Create ORG_OWNER role if it doesn't exist for this org
            ownerRole = new Role();
            ownerRole.setId(UUIDGenerator.generate());
            ownerRole.setOrgId(org.getId());
            ownerRole.setName(RoleType.ORG_OWNER.name());
            ownerRole.setType(RoleType.ORG_OWNER.name());
            ownerRole.setCreateBy(user.getId());
            ownerRole.setCreateTime(new Date());
            roleMapper.insert(ownerRole);
            log.info("Created ORG_OWNER role for organization '{}'.", org.getName());
        }

        // Check if user already has the owner role
        RelRoleUser existing = rruMapper.selectByUserAndRole(user.getId(), ownerRole.getId());
        if (existing != null) {
            log.debug("User '{}' is already ORG_OWNER of organization '{}'.", user.getUsername(), org.getName());
            return;
        }

        // Grant ORG_OWNER to the user
        RelRoleUser relRoleUser = new RelRoleUser();
        relRoleUser.setId(UUIDGenerator.generate());
        relRoleUser.setRoleId(ownerRole.getId());
        relRoleUser.setUserId(user.getId());
        relRoleUser.setCreateBy(user.getId());
        relRoleUser.setCreateTime(new Date());
        rruMapper.insert(relRoleUser);
        log.info("Granted ORG_OWNER to user '{}' in organization '{}'.", user.getUsername(), org.getName());
    }
}

/*
 * Datart
 * <p>
 * Copyright 2021
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

package datart.security.manager.shiro;

import datart.core.entity.User;
import datart.core.mappers.ext.UserMapperExt;
import datart.security.manager.AuthenticationAssembler;
import datart.security.manager.AuthenticationCache;
import org.springframework.stereotype.Component;

@Component
public class ShiroAuthenticationAssembler implements AuthenticationAssembler {

    private final UserMapperExt userMapper;

    public ShiroAuthenticationAssembler(UserMapperExt userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public AuthenticationCache assemble(String usernameOrEmail, String realmName) {
        User user = userMapper.selectByNameOrEmail(usernameOrEmail);
        if (user == null) {
            return null;
        }
        return new AuthenticationCache(user, user.getPassword(), realmName);
    }

}

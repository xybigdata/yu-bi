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

package yubi.server.base.transfer.model;

import yubi.core.base.consts.FileOwner;
import yubi.core.common.Application;
import yubi.server.service.FileService;
import lombok.Data;

import java.io.Serializable;

@Data
public abstract class TransferModel implements Serializable {

    private String orgId;

    public String getFileName() {
        return Application.getBean(FileService.class).getBasePath(FileOwner.EXPORT, null) + "/" + getVizName() + "-" + System.currentTimeMillis() + ".viz";
    }

    public abstract String getVizName();

}

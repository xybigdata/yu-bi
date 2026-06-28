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

package yubi.server.base.params;

import yubi.core.data.provider.ScriptType;
import yubi.core.entity.RelSubjectColumns;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ViewCreateParam extends VizCreateParam {

    private String name;

    private String description;

    private String sourceId;

    private String parentId;

    private Boolean isFolder;

    private Double index;

    private String script;

    private ScriptType type;

    private String model;

    private List<RelSubjectColumns> columnPermission;

    private List<VariableCreateParam> variablesToCreate;

    private String config;

}

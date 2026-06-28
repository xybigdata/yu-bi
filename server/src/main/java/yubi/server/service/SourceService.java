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

import yubi.core.data.provider.SchemaInfo;
import yubi.core.entity.Folder;
import yubi.core.entity.Source;
import yubi.core.mappers.ext.SourceMapperExt;
import yubi.server.base.params.SourceBaseUpdateParam;
import yubi.server.base.params.SourceCreateParam;
import yubi.server.base.params.SourceUpdateParam;
import yubi.server.base.transfer.model.SourceResourceModel;
import yubi.server.base.transfer.model.TransferModel;

import java.util.List;

public interface SourceService extends BaseCRUDService<Source, SourceMapperExt>, ResourceTransferService<Source, SourceResourceModel, TransferModel, Folder> {

    boolean checkUnique(String orgId, String parentId, String name);

    List<Source> listSources(String orgId, boolean active);

    SchemaInfo getSourceSchemaInfo(String sourceId);

    SchemaInfo syncSourceSchema(String sourceId) throws Exception;

    Source createSource(SourceCreateParam createParam);

    boolean updateSource(SourceUpdateParam updateParam);

    boolean updateBase(SourceBaseUpdateParam updateParam);

    boolean unarchive(String id, String newName, String parentId, double index);

}

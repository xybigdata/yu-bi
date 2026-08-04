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

package yubi.server.controller;

import yubi.server.base.dto.ResponseData;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.DownloadService;
import yubi.server.artifact.ArtifactTaskWebMapper;
import yubi.server.artifact.ArtifactTaskWebMapper.ArtifactTaskResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "DownloadController")
@RestController
@RequestMapping(value = "/download")
public class DownloadController extends BaseController {

    private final DownloadService downloadService;
    private final ArtifactTaskWebMapper artifactTaskWebMapper;

    public DownloadController(DownloadService downloadService,
                              ArtifactTaskWebMapper artifactTaskWebMapper) {
        this.downloadService = downloadService;
        this.artifactTaskWebMapper = artifactTaskWebMapper;
    }

    @Operation(summary = "submit a new download task")
    @PostMapping(value = "/submit/task")
    public ResponseData<ArtifactTaskResponse> submitDownloadTask(
            @RequestBody @Validated DownloadCreateParam createParam
    ) {
        return ResponseData.success(artifactTaskWebMapper.response(
                downloadService.submitDownloadTask(createParam)
        ));
    }

}

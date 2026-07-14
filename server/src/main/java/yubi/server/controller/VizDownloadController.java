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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.dto.ResponseData;
import yubi.server.base.transfer.DashboardTemplateParam;
import yubi.server.base.transfer.DatachartTemplateParam;
import yubi.server.base.transfer.ResourceTransferParam;
import yubi.server.service.VizService;

import java.io.IOException;

@RestController
@RequestMapping(value = "/viz")
@Tag(name = "VizController")
public class VizDownloadController {

    private final VizService vizService;

    public VizDownloadController(VizService vizService) {
        this.vizService = vizService;
    }

    @Operation(summary = "export viz")
    @PostMapping(value = "/export")
    public ResponseData<DownloadTaskDTO> exportViz(@RequestBody ResourceTransferParam param) throws IOException {
        return ResponseData.success(vizService.exportResource(param));
    }

    @Operation(summary = "export dashboard template")
    @PostMapping(value = "/export/dashboard/template")
    public ResponseData<DownloadTaskDTO> exportDashboardTemplate(
            @Validated @RequestBody DashboardTemplateParam param
    ) throws IOException {
        return ResponseData.success(vizService.exportDashboardTemplate(param));
    }

    @Operation(summary = "export datachart template")
    @PostMapping(value = "/export/datachart/template")
    public ResponseData<DownloadTaskDTO> exportDatachartTemplate(
            @Validated @RequestBody DatachartTemplateParam param
    ) throws IOException {
        return ResponseData.success(vizService.exportDatachartTemplate(param));
    }
}

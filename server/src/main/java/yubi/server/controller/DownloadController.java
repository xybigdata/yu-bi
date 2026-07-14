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

import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.dto.ResponseData;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.DownloadFileResource;
import yubi.server.service.DownloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;


@Tag(name = "DownloadController")
@RestController
@RequestMapping(value = "/download")
public class DownloadController extends BaseController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @Operation(summary = "get download tasks")
    @GetMapping(value = "/tasks")
    public ResponseData<List<DownloadTaskDTO>> listDownloadTasks() {
        return ResponseData.success(downloadService.listDownloadTasks());
    }

    @Operation(summary = "submit a new download task")
    @PostMapping(value = "/submit/task")
    public ResponseData<DownloadTaskDTO> submitDownloadTask(
            @RequestBody @Validated DownloadCreateParam createParam
    ) {
        return ResponseData.success(downloadService.submitDownloadTask(createParam));
    }

    @Operation(summary = "get download file")
    @GetMapping(value = "/files/{id}")
    public void downloadFile(@PathVariable String id,
                             HttpServletResponse response) throws IOException {
        try (DownloadFileResource download = downloadService.downloadFile(id)) {
            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Content-Disposition", String.format(
                    "attachment;filename=\"%s\"",
                    URLEncoder.encode(download.fileName(), "utf-8")
            ));
            download.inputStream().transferTo(response.getOutputStream());
        }
    }

}

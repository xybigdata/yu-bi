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

import yubi.core.common.FileUtils;
import yubi.core.entity.Download;
import yubi.server.base.dto.ResponseData;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.DownloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    public ResponseData<List<Download>> listDownloadTasks() {
        return ResponseData.success(downloadService.listDownloadTasks());
    }

    @Operation(summary = "submit a new download task")
    @PostMapping(value = "/submit/task")
    public ResponseData<Download> submitDownloadTask(@RequestBody @Validated DownloadCreateParam createParam) {
        return ResponseData.success(downloadService.submitDownloadTask(createParam));
    }

    @Operation(summary = "get download file")
    @GetMapping(value = "/files/{id}")
    public void downloadFile(@PathVariable String id,
                             HttpServletResponse response) throws IOException {
        Download download = downloadService.downloadFile(id);
        response.setHeader("Content-Type", "application/octet-stream");
        File file = new File(FileUtils.withBasePath(download.getPath()));
        response.setHeader("Content-Disposition", String.format("attachment;filename=\"%s\"", URLEncoder.encode(file.getName(), "utf-8")));
        try (InputStream inputStream = new FileInputStream(file)) {
            Streams.copy(inputStream, response.getOutputStream(), true);
        }
    }

}

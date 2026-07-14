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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yubi.core.base.exception.NotAllowedException;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.dto.ResponseData;
import yubi.server.base.params.ShareDownloadParam;
import yubi.server.service.DownloadFileResource;
import yubi.server.service.ShareDownloadSession;
import yubi.server.service.ShareDownloadSessionManager;
import yubi.server.service.ShareService;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/shares")
public class ShareDownloadController {

    private final ShareService shareService;

    private final ShareDownloadSessionManager sessionManager;

    public ShareDownloadController(ShareService shareService, ShareDownloadSessionManager sessionManager) {
        this.shareService = shareService;
        this.sessionManager = sessionManager;
    }

    @Operation(summary = "create a download task")
    @PostMapping("/{shareId}/download")
    public ResponseData<DownloadTaskDTO> createDownload(
            @PathVariable String shareId,
            @RequestBody ShareDownloadParam downloadCreateParam,
            HttpServletRequest request
    ) {
        requireNoClientOverrides(request);
        ShareDownloadSession session = sessionManager.require(shareId, request);
        return ResponseData.success(shareService.createDownload(shareId, session, downloadCreateParam));
    }

    @Operation(summary = "get download task")
    @GetMapping("/{shareId}/download/tasks")
    public ResponseData<List<DownloadTaskDTO>> downloadList(
            @PathVariable String shareId,
            HttpServletRequest request
    ) {
        requireNoClientOverrides(request);
        ShareDownloadSession session = sessionManager.require(shareId, request);
        return ResponseData.success(shareService.listDownloadTask(shareId, session));
    }

    @Operation(summary = "download file")
    @GetMapping("/{shareId}/download/{downloadId}")
    public void downloadFile(@PathVariable String shareId,
                             @PathVariable String downloadId,
                             HttpServletRequest request,
                             HttpServletResponse response) throws IOException {
        requireNoClientOverrides(request);
        ShareDownloadSession session = sessionManager.require(shareId, request);
        try (DownloadFileResource download = shareService.download(shareId, session, downloadId)) {
            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Content-Disposition", String.format(
                    "attachment; filename=\"%s\"",
                    URLEncoder.encode(download.fileName(), "utf-8")
            ));
            download.inputStream().transferTo(response.getOutputStream());
        }
    }

    private void requireNoClientOverrides(HttpServletRequest request) {
        if (!request.getParameterMap().isEmpty()) {
            throw new NotAllowedException("分享下载请求包含不允许的字段");
        }
    }
}

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

import yubi.core.data.provider.StdSqlOperator;
import yubi.core.base.exception.NotAllowedException;
import yubi.server.base.dto.ResponseData;
import yubi.server.base.dto.ShareInfo;
import yubi.server.base.params.*;
import yubi.server.service.ShareDownloadSession;
import yubi.server.service.ShareDownloadSessionManager;
import yubi.server.service.ShareService;
import yubi.server.service.ShareVizAccess;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/shares")
public class ShareController extends BaseController {

    private final ShareService shareService;

    private final ShareDownloadSessionManager sessionManager;

    public ShareController(ShareService shareService, ShareDownloadSessionManager sessionManager) {

        this.shareService = shareService;
        this.sessionManager = sessionManager;
    }

    @Operation(summary = "create a share")
    @PostMapping
    public ResponseData<ShareToken> create(@Validated @RequestBody ShareCreateParam createParam) {
        return ResponseData.success(shareService.createShare(createParam));
    }

    @Operation(summary = "update a share")
    @PutMapping(value = "{shareId:^(?!(?:execute|download)$).+}")
    public ResponseData<ShareInfo> update(
            @PathVariable String shareId,
            @Validated @RequestBody ShareUpdateParam updateParam) {
        updateParam.setId(shareId);
        return ResponseData.success(shareService.updateShare(updateParam));
    }

    @Operation(summary = "delete a share")
    @DeleteMapping(value = "{shareId:^(?!(?:execute|download)$).+}")
    public ResponseData<Boolean> delete(@PathVariable String shareId) {
        return ResponseData.success(shareService.delete(shareId, false));
    }

    @Operation(summary = "list share")
    @GetMapping(value = "{vizId:^(?!(?:execute|download)$).+}")
    public ResponseData<List<ShareInfo>> list(@PathVariable String vizId) {
        return ResponseData.success(shareService.listShare(vizId));
    }


    @Operation(summary = "get viz detail")
    @PostMapping("{shareId}/viz")
    public ResponseData<ShareVizDetail> vizDetail(@PathVariable String shareId,
                                                  @RequestBody ShareToken shareToken,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        if (shareToken == null) {
            throw new NotAllowedException("分享访问无效");
        }
        ShareDownloadSession existingSession = null;
        if (shareToken.getAuthorizedToken() != null && !shareToken.getAuthorizedToken().isBlank()) {
            existingSession = sessionManager.require(shareId, request);
        }
        shareToken.setId(shareId);
        ShareVizAccess access = shareService.getShareViz(shareToken, existingSession);
        if (existingSession == null) {
            sessionManager.issue(
                    access.shareId(),
                    access.authenticationMode(),
                    access.securityFingerprint(),
                    access.authenticatedSubjectId(),
                    request,
                    response
            );
        }
        return ResponseData.success(access.detail());
    }


    @Operation(summary = "support std functions")
    @PostMapping("/function/support/{sourceId}")
    public ResponseData<Set<StdSqlOperator>> supportFunctions(@PathVariable String sourceId,
                                                              @RequestBody ShareToken executeToken) {
        return ResponseData.success(shareService.supportedStdFunctions(executeToken, sourceId));
    }

}

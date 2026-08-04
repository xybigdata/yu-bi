package yubi.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yubi.server.artifact.ArtifactContent;
import yubi.server.artifact.ArtifactTaskWebMapper;
import yubi.server.artifact.ArtifactTaskWebMapper.ArtifactTaskResponse;
import yubi.server.base.dto.ResponseData;
import yubi.server.artifact.TaskPage;
import yubi.server.service.ShareService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Tag(name = "ShareArtifactTaskController")
@RestController
@RequestMapping("/shares/{shareId}/artifact-tasks")
public class ShareArtifactTaskController extends BaseController {

    private final ShareService shareService;
    private final ArtifactTaskWebMapper mapper;

    public ShareArtifactTaskController(ShareService shareService, ArtifactTaskWebMapper mapper) {
        this.shareService = shareService;
        this.mapper = mapper;
    }

    @Operation(summary = "分页查询分享页产物任务")
    @GetMapping
    public ResponseData<ArtifactTaskController.TaskPageResponse> list(
            @PathVariable String shareId,
            @RequestParam String clientId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        TaskPage page = shareService.listArtifactTasks(shareId, clientId, password, offset, limit);
        return ResponseData.success(new ArtifactTaskController.TaskPageResponse(
                page.tasks().stream().map(mapper::response).toList(), page.nextOffset()));
    }

    @Operation(summary = "查询分享页产物任务")
    @GetMapping("/{id}")
    public ResponseData<ArtifactTaskResponse> inspect(@PathVariable String shareId,
                                                      @PathVariable String id,
                                                      @RequestParam String clientId,
                                                      @RequestParam(required = false) String password) {
        return ResponseData.success(mapper.response(
                shareService.getArtifactTask(shareId, clientId, password, id)));
    }

    @Operation(summary = "重试分享页失败或超时的产物任务")
    @PostMapping("/{id}/retry")
    public ResponseData<ArtifactTaskResponse> retry(@PathVariable String shareId,
                                                    @PathVariable String id,
                                                    @RequestParam String clientId,
                                                    @RequestParam(required = false) String password) {
        return ResponseData.success(mapper.response(
                shareService.retryArtifactTask(shareId, clientId, password, id)));
    }

    @Operation(summary = "下载分享页产物文件")
    @GetMapping("/{id}/content")
    public void open(@PathVariable String shareId,
                     @PathVariable String id,
                     @RequestParam String clientId,
                     @RequestParam(required = false) String password,
                     HttpServletResponse response) throws IOException {
        try (ArtifactContent content = shareService.openArtifact(shareId, clientId, password, id)) {
            response.setContentType(content.mediaType());
            response.setContentLengthLong(content.length());
            response.setHeader("Content-Disposition", ContentDisposition.attachment()
                    .filename(content.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString());
            Streams.copy(content.stream(), response.getOutputStream(), false);
        }
        shareService.confirmArtifactDelivery(shareId, clientId, password, id);
    }

    @Operation(summary = "清除分享页已结束的产物任务")
    @DeleteMapping("/{id}")
    public ResponseData<Boolean> delete(@PathVariable String shareId,
                                        @PathVariable String id,
                                        @RequestParam String clientId,
                                        @RequestParam(required = false) String password) {
        shareService.deleteArtifactTask(shareId, clientId, password, id);
        return ResponseData.success(true);
    }
}

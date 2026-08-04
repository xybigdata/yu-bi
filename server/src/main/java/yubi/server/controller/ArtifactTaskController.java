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
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.artifact.ArtifactAccess;
import yubi.server.artifact.ArtifactContent;
import yubi.server.artifact.ArtifactTaskException;
import yubi.server.artifact.ArtifactTaskWebMapper;
import yubi.server.artifact.ArtifactTaskWebMapper.ArtifactTaskResponse;
import yubi.server.artifact.ArtifactTasks;
import yubi.server.artifact.TaskBatch;
import yubi.server.artifact.TaskPage;
import yubi.server.base.dto.ResponseData;
import yubi.server.service.OrgService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Tag(name = "ArtifactTaskController")
@RestController
@RequestMapping("/organizations/{organizationId}/artifact-tasks")
public class ArtifactTaskController extends BaseController {

    private static final String NOT_FOUND = "ARTIFACT_NOT_FOUND";

    private final ArtifactTasks tasks;
    private final YuBiSecurityManager securityManager;
    private final OrgService orgService;
    private final ArtifactTaskWebMapper mapper;

    public ArtifactTaskController(ArtifactTasks tasks,
                                  YuBiSecurityManager securityManager,
                                  OrgService orgService,
                                  ArtifactTaskWebMapper mapper) {
        this.tasks = tasks;
        this.securityManager = securityManager;
        this.orgService = orgService;
        this.mapper = mapper;
    }

    @Operation(summary = "分页查询当前组织的产物任务")
    @GetMapping
    public ResponseData<TaskPageResponse> list(
            @PathVariable String organizationId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        TaskPage page = tasks.list(access(organizationId), offset, limit);
        return ResponseData.success(new TaskPageResponse(
                page.tasks().stream().map(mapper::response).toList(),
                page.nextOffset()
        ));
    }

    @Operation(summary = "查询产物任务")
    @GetMapping("/{id}")
    public ResponseData<ArtifactTaskResponse> inspect(@PathVariable String organizationId,
                                                      @PathVariable String id) {
        TaskBatch batch = tasks.inspect(access(organizationId), Set.of(id));
        if (batch.missingIds().contains(id)) {
            throw notFound();
        }
        return ResponseData.success(mapper.response(batch.tasks().getFirst()));
    }

    @Operation(summary = "重试失败或超时的产物任务")
    @PostMapping("/{id}/retry")
    public ResponseData<ArtifactTaskResponse> retry(@PathVariable String organizationId,
                                                    @PathVariable String id) {
        return ResponseData.success(mapper.response(tasks.retry(access(organizationId), id)));
    }

    @Operation(summary = "下载产物文件")
    @GetMapping("/{id}/content")
    public void open(@PathVariable String organizationId,
                     @PathVariable String id,
                     HttpServletResponse response) throws IOException {
        try (ArtifactContent content = tasks.open(access(organizationId), id)) {
            response.setContentType(content.mediaType());
            response.setContentLengthLong(content.length());
            response.setHeader("Content-Disposition", ContentDisposition.attachment()
                    .filename(content.fileName(), StandardCharsets.UTF_8)
                    .build()
                    .toString());
            Streams.copy(content.stream(), response.getOutputStream(), false);
        }
        tasks.confirmDelivery(access(organizationId), id);
    }

    @Operation(summary = "清除已结束的产物任务")
    @DeleteMapping("/{id}")
    public ResponseData<Boolean> delete(@PathVariable String organizationId,
                                        @PathVariable String id) {
        tasks.delete(access(organizationId), id);
        return ResponseData.success(true);
    }

    private ArtifactAccess access(String organizationId) {
        User user = securityManager.getCurrentUser();
        if (user == null || orgService.listOrganizations().stream()
                .noneMatch(organization -> organizationId.equals(organization.getId()))) {
            throw notFound();
        }
        return ArtifactAccess.authenticated(user.getId(), organizationId, user.getUsername());
    }

    private ArtifactTaskException notFound() {
        return new ArtifactTaskException(
                NOT_FOUND,
                "产物任务不存在或已过期",
                UUID.randomUUID().toString()
        );
    }

    public record TaskPageResponse(java.util.List<ArtifactTaskResponse> tasks,
                                   Integer nextOffset) {
    }
}

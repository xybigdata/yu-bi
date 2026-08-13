package yubi.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yubi.core.entity.User;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.ResponseData;
import yubi.server.recycle.RecycleAccess;
import yubi.server.recycle.RecycleBatch;
import yubi.server.recycle.RecycleBulkCommand;
import yubi.server.recycle.RecycleEntry;
import yubi.server.recycle.RecycleExecutionCommand;
import yubi.server.recycle.RecyclePolicy;
import yubi.server.recycle.RecyclePreflight;
import yubi.server.recycle.RecyclePreflightCommand;
import yubi.server.recycle.RecycleResourceType;
import yubi.server.recycle.RecycleService;

import java.util.List;

@Tag(name = "RecycleController")
@RestController
@RequestMapping("/organizations/{organizationId}/recycle/{resourceType}")
public class RecycleController extends BaseController {

    private final RecycleService recycleService;
    private final YuBiSecurityManager securityManager;

    public RecycleController(RecycleService recycleService,
                             YuBiSecurityManager securityManager) {
        this.recycleService = recycleService;
        this.securityManager = securityManager;
    }

    @Operation(summary = "预检当前业务模块的批量删除")
    @PostMapping("/preflight")
    public ResponseData<RecyclePreflight> preflight(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @RequestBody PreflightRequest request) {
        return ResponseData.success(recycleService.preflight(
                access(organizationId),
                new RecyclePreflightCommand(resourceType, request.rootIds())));
    }

    @Operation(summary = "批量移入当前业务模块回收站")
    @PostMapping
    public ResponseData<RecycleBatch> moveToRecycle(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @RequestBody ExecutionRequest request) {
        return ResponseData.success(recycleService.moveToRecycle(
                access(organizationId),
                new RecycleExecutionCommand(
                        resourceType, request.operationToken(), request.clientRequestId())));
    }

    @Operation(summary = "查询当前业务模块回收站")
    @GetMapping
    public ResponseData<List<RecycleEntry>> list(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType) {
        return ResponseData.success(recycleService.list(
                access(organizationId), resourceType));
    }

    @Operation(summary = "查询当前业务模块的删除批次")
    @GetMapping("/batches/{batchId}")
    public ResponseData<RecycleBatch> getBatch(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @PathVariable String batchId) {
        return ResponseData.success(recycleService.getBatch(
                access(organizationId), resourceType, batchId));
    }

    @Operation(summary = "批量恢复当前业务模块回收站内容")
    @PostMapping("/restore")
    public ResponseData<RecycleBatch> restore(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @RequestBody BulkRequest request) {
        return ResponseData.success(recycleService.restore(
                access(organizationId), request.command(resourceType)));
    }

    @Operation(summary = "撤销最近一次移入回收站批次")
    @PostMapping("/batches/{batchId}/undo")
    public ResponseData<RecycleBatch> undo(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @PathVariable String batchId,
            @RequestBody UndoRequest request) {
        RecycleAccess access = access(organizationId);
        recycleService.getBatch(access, resourceType, batchId);
        return ResponseData.success(recycleService.undo(
                access, batchId, request.undoToken()));
    }

    @Operation(summary = "批量永久删除当前业务模块回收站内容")
    @DeleteMapping
    public ResponseData<RecycleBatch> permanentlyDelete(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @RequestBody BulkRequest request) {
        return ResponseData.success(recycleService.permanentlyDelete(
                access(organizationId), request.command(resourceType)));
    }

    @Operation(summary = "清空当前业务模块回收站")
    @PostMapping("/empty")
    public ResponseData<RecycleBatch> empty(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @RequestBody ClientRequest request) {
        RecycleAccess access = access(organizationId);
        List<String> recordIds = recycleService.list(access, resourceType).stream()
                .map(RecycleEntry::id)
                .toList();
        if (recordIds.isEmpty()) {
            throw new IllegalArgumentException("当前模块回收站为空");
        }
        return ResponseData.success(recycleService.permanentlyDelete(
                access, new RecycleBulkCommand(
                        resourceType, recordIds, request.clientRequestId())));
    }

    @Operation(summary = "查询当前业务模块自动清理策略")
    @GetMapping("/policy")
    public ResponseData<RecyclePolicy> getPolicy(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType) {
        return ResponseData.success(recycleService.getPolicy(
                access(organizationId), resourceType));
    }

    @Operation(summary = "修改当前业务模块自动清理策略")
    @PutMapping("/policy")
    public ResponseData<RecyclePolicy> updatePolicy(
            @PathVariable String organizationId,
            @PathVariable RecycleResourceType resourceType,
            @RequestBody RecyclePolicy policy) {
        return ResponseData.success(recycleService.updatePolicy(
                access(organizationId), resourceType, policy));
    }

    private RecycleAccess access(String organizationId) {
        User user = securityManager.getCurrentUser();
        if (user == null) {
            throw new SecurityException("用户未登录");
        }
        return RecycleAccess.authenticated(
                user.getId(), organizationId, securityManager.isOrgOwner(organizationId));
    }

    public record PreflightRequest(List<String> rootIds) {
    }

    public record ExecutionRequest(String operationToken, String clientRequestId) {
    }

    public record BulkRequest(List<String> recordIds, String clientRequestId) {
        RecycleBulkCommand command(RecycleResourceType resourceType) {
            return new RecycleBulkCommand(resourceType, recordIds, clientRequestId);
        }
    }

    public record ClientRequest(String clientRequestId) {
    }

    public record UndoRequest(String undoToken) {
    }
}

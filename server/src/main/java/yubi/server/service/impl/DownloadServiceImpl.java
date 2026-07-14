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
package yubi.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import yubi.core.base.consts.AttachmentType;
import yubi.core.base.consts.DownloadOwnerType;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.common.UUIDGenerator;
import yubi.core.entity.Download;
import yubi.core.entity.User;
import yubi.core.mappers.ext.DownloadMapperExt;
import yubi.security.manager.YuBiSecurityManager;
import yubi.server.base.dto.DownloadTaskDTO;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.AttachmentService;
import yubi.server.service.DownloadFileResolver;
import yubi.server.service.DownloadFileResource;
import yubi.server.service.DownloadFileTarget;
import yubi.server.service.DownloadService;
import yubi.server.service.DownloadTaskFileGenerator;
import yubi.server.service.DownloadTaskScheduler;
import yubi.server.service.SharedDownloadContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Service
public class DownloadServiceImpl implements DownloadService {

    private static final String ACCESS_DENIED = "无权访问下载任务";
    private static final String TASK_INVALID = "下载任务参数无效";
    private static final String TASK_NOT_FINISHED = "下载任务尚未完成";
    private static final String INTERNAL_FAILURE = "INTERNAL_FAILURE";
    private static final int MAX_TASK_NAME_BYTES = 180;
    private static final String SESSION_DIGEST_PATTERN = "[0-9a-f]{64}";

    private final DownloadMapperExt downloadMapper;
    private final YuBiSecurityManager securityManager;
    private final DownloadFileResolver fileResolver;
    private final DownloadTaskScheduler taskScheduler;

    public DownloadServiceImpl(DownloadMapperExt downloadMapper,
                               YuBiSecurityManager securityManager,
                               DownloadFileResolver fileResolver,
                               DownloadTaskScheduler taskScheduler) {
        this.downloadMapper = downloadMapper;
        this.securityManager = securityManager;
        this.fileResolver = fileResolver;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public DownloadTaskDTO submitDownloadTask(DownloadCreateParam downloadParams) {
        User user = requireAuthenticatedUser();
        validateDownloadParams(downloadParams);
        String taskName = safeTaskName(downloadParams.getFileName());
        return createAndSchedule(
                DownloadOwnerType.AUTHENTICATED,
                user.getId(),
                null,
                user.getId(),
                user.getUsername(),
                taskName,
                taskDirectory -> generateAttachment(downloadParams, taskName, taskDirectory, null, null)
        );
    }

    @Override
    public DownloadTaskDTO submitSharedDownloadTask(DownloadCreateParam downloadParams,
                                                    SharedDownloadContext context) {
        requireSharedContext(context, true);
        validateDownloadParams(downloadParams);
        String taskName = safeTaskName(downloadParams.getFileName());
        return createAndSchedule(
                DownloadOwnerType.SHARE,
                context.sessionDigest(),
                context.shareId(),
                context.auditCreatorId(),
                context.executionUsername(),
                taskName,
                taskDirectory -> generateAttachment(
                        downloadParams,
                        taskName,
                        taskDirectory,
                        context.querySubjectId(),
                        context.organizationId()
                )
        );
    }

    @Override
    public DownloadTaskDTO submitGeneratedTask(String fileName, DownloadTaskFileGenerator generator) {
        User user = requireAuthenticatedUser();
        String taskName = safeGeneratedFileName(fileName);
        if (generator == null) {
            throw new NotAllowedException(TASK_INVALID);
        }
        return createAndSchedule(
                DownloadOwnerType.AUTHENTICATED,
                user.getId(),
                null,
                user.getId(),
                user.getUsername(),
                taskName,
                taskDirectory -> {
                    try (DownloadFileTarget target = fileResolver.createTarget(taskDirectory, taskName)) {
                        generator.generate(target.outputStream());
                        return target.storedPath();
                    }
                }
        );
    }

    @Override
    public List<DownloadTaskDTO> listDownloadTasks() {
        User user = requireAuthenticatedUser();
        return persistence(
                () -> downloadMapper.selectAuthenticatedTasks(user.getId(), sevenDaysAgo()),
                "LIST_AUTHENTICATED"
        ).stream()
                .map(DownloadTaskDTO::from)
                .toList();
    }

    @Override
    public List<DownloadTaskDTO> listSharedDownloadTasks(SharedDownloadContext context) {
        requireSharedContext(context, false);
        return persistence(
                () -> downloadMapper.selectSharedTasks(
                        context.shareId(),
                        context.sessionDigest(),
                        sevenDaysAgo()
                ),
                "LIST_SHARED"
        ).stream()
                .map(DownloadTaskDTO::from)
                .toList();
    }

    public void verifyStorageContract() {
        Date beginningOfTime = Date.from(Instant.EPOCH);
        persistence(
                () -> downloadMapper.selectAuthenticatedTasks("__demo_schema_probe__", beginningOfTime),
                "VERIFY_AUTHENTICATED_SCHEMA"
        );
        persistence(
                () -> downloadMapper.selectSharedTasks(
                        "__demo_schema_probe__",
                        "0".repeat(64),
                        beginningOfTime
                ),
                "VERIFY_SHARED_SCHEMA"
        );
    }

    @Override
    public DownloadFileResource downloadFile(String downloadId) {
        User user = requireAuthenticatedUser();
        Download download = persistence(
                () -> downloadMapper.selectAuthenticatedTask(downloadId, user.getId()),
                "READ_AUTHENTICATED"
        );
        requireDownloadable(download);
        DownloadFileResource resource = fileResolver.openForRead(download.getPath());
        try {
            int updated = persistence(
                    () -> downloadMapper.markAuthenticatedTaskDownloaded(
                            downloadId,
                            user.getId(),
                            new Date()
                    ),
                    "MARK_AUTHENTICATED_DOWNLOADED"
            );
            if (updated != 1) {
                throw new NotAllowedException(ACCESS_DENIED);
            }
            return resource;
        } catch (RuntimeException exception) {
            closeQuietly(resource);
            throw exception;
        }
    }

    @Override
    public DownloadFileResource downloadSharedFile(String downloadId, SharedDownloadContext context) {
        requireSharedContext(context, false);
        Download download = persistence(
                () -> downloadMapper.selectSharedTask(
                        downloadId,
                        context.shareId(),
                        context.sessionDigest()
                ),
                "READ_SHARED"
        );
        requireDownloadable(download);
        DownloadFileResource resource = fileResolver.openForRead(download.getPath());
        try {
            int updated = persistence(
                    () -> downloadMapper.markSharedTaskDownloaded(
                            downloadId,
                            context.shareId(),
                            context.sessionDigest(),
                            new Date()
                    ),
                    "MARK_SHARED_DOWNLOADED"
            );
            if (updated != 1) {
                throw new NotAllowedException(ACCESS_DENIED);
            }
            return resource;
        } catch (RuntimeException exception) {
            closeQuietly(resource);
            throw exception;
        }
    }

    private DownloadTaskDTO createAndSchedule(DownloadOwnerType ownerType,
                                              String ownerId,
                                              String shareId,
                                              String createBy,
                                              String executionUsername,
                                              String taskName,
                                              GeneratedFileSupplier generatedFileSupplier) {
        Download download = new Download();
        download.setId(UUIDGenerator.generate());
        download.setName(taskName);
        download.setStatus((byte) 0);
        download.setCreateTime(new Date());
        download.setCreateBy(createBy);
        download.setOwnerType(ownerType.name());
        download.setOwnerId(ownerId);
        download.setShareId(shareId);
        persistence(() -> downloadMapper.insert(download), "CREATE");

        try {
            taskScheduler.submit(() -> executeTask(download, executionUsername, generatedFileSupplier));
        } catch (RuntimeException exception) {
            markFailed(download);
        }
        return DownloadTaskDTO.from(download);
    }

    private void executeTask(Download download,
                             String executionUsername,
                             GeneratedFileSupplier generatedFileSupplier) {
        try {
            securityManager.runAs(executionUsername);
            Path taskDirectory = fileResolver.createTaskDirectory(download.getId());
            String storedPath = generatedFileSupplier.get(taskDirectory);
            Download update = new Download();
            update.setId(download.getId());
            update.setPath(storedPath);
            update.setStatus((byte) 1);
            persistence(() -> downloadMapper.updateByPrimaryKeySelective(update), "COMPLETE");
            download.setPath(storedPath);
            download.setStatus((byte) 1);
        } catch (Throwable throwable) {
            markFailed(download);
            if (throwable instanceof Error error) {
                throw error;
            }
        } finally {
            try {
                securityManager.releaseRunAs();
            } catch (RuntimeException ignored) {
                // 清理失败不能把原始异常或会话值写入日志。
            }
        }
    }

    private void markFailed(Download download) {
        Download update = new Download();
        update.setId(download.getId());
        update.setStatus((byte) -1);
        update.setFailureCode(INTERNAL_FAILURE);
        try {
            downloadMapper.updateByPrimaryKeySelective(update);
        } catch (RuntimeException exception) {
            log.warn("下载任务失败状态写入失败，taskId={}", download.getId());
        }
        download.setStatus((byte) -1);
        download.setFailureCode(INTERNAL_FAILURE);
        log.warn("下载任务执行失败，taskId={}，failureCode={}", download.getId(), INTERNAL_FAILURE);
    }

    private String generateAttachment(DownloadCreateParam downloadParams,
                                      String taskName,
                                      Path taskDirectory,
                                      String trustedSharedQuerySubjectId,
                                      String trustedSharedQueryOrganizationId) throws Exception {
        Path stagingDirectory = Files.createTempDirectory("yubi-download-staging-");
        try {
            AttachmentType attachmentType = Objects.requireNonNullElse(
                    downloadParams.getDownloadType(),
                    AttachmentType.EXCEL
            );
            AttachmentService attachmentService = AttachmentService.matchAttachmentService(attachmentType);
            File file = attachmentService.getFile(
                    downloadParams,
                    stagingDirectory.toString(),
                    taskName,
                    trustedSharedQuerySubjectId,
                    trustedSharedQueryOrganizationId
            );
            return fileResolver.copyIntoTask(taskDirectory, stagingDirectory, file.toPath());
        } finally {
            try {
                org.springframework.util.FileSystemUtils.deleteRecursively(stagingDirectory);
            } catch (IOException | RuntimeException exception) {
                log.warn("下载任务临时文件清理失败");
            }
        }
    }

    private void validateDownloadParams(DownloadCreateParam downloadParams) {
        if (downloadParams == null || downloadParams.getDownloadParams() == null
                || downloadParams.getDownloadParams().isEmpty()) {
            throw new NotAllowedException(TASK_INVALID);
        }
    }

    private User requireAuthenticatedUser() {
        User user = securityManager.getCurrentUser();
        if (user == null || StringUtils.isAnyBlank(user.getId(), user.getUsername())) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
        return user;
    }

    private void requireSharedContext(SharedDownloadContext context, boolean requireExecutionIdentity) {
        if (context == null || StringUtils.isAnyBlank(
                context.shareId(),
                context.sessionDigest(),
                context.auditCreatorId(),
                context.querySubjectId(),
                context.organizationId()
        ) || !context.sessionDigest().matches(SESSION_DIGEST_PATTERN)
                || requireExecutionIdentity && StringUtils.isBlank(context.executionUsername())) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
    }

    private void requireDownloadable(Download download) {
        if (download == null) {
            throw new NotAllowedException(ACCESS_DENIED);
        }
        if (download.getStatus() == null || download.getStatus() < 1) {
            throw new NotAllowedException(TASK_NOT_FINISHED);
        }
    }

    private String safeTaskName(String value) {
        if (StringUtils.isBlank(value)) {
            return "download";
        }
        String normalized = value.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replace("\r", "")
                .replace("\n", "")
                .trim();
        normalized = StringUtils.defaultIfBlank(normalized, "download");
        if (normalized.getBytes(StandardCharsets.UTF_8).length > MAX_TASK_NAME_BYTES) {
            throw new NotAllowedException(TASK_INVALID);
        }
        return normalized;
    }

    private String safeGeneratedFileName(String value) {
        String normalized = safeTaskName(value);
        if (!normalized.equals(value)) {
            throw new NotAllowedException(TASK_INVALID);
        }
        return normalized;
    }

    private Date sevenDaysAgo() {
        return Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
    }

    private void closeQuietly(DownloadFileResource resource) {
        try {
            resource.close();
        } catch (IOException ignored) {
            // 关闭失败不能覆盖权限或持久化错误。
        }
    }

    private <T> T persistence(Supplier<T> operation, String operationName) {
        try {
            return operation.get();
        } catch (RuntimeException exception) {
            log.warn("下载任务持久化操作失败，operation={}", operationName);
            throw new NotAllowedException("下载任务不可用");
        }
    }

    @FunctionalInterface
    private interface GeneratedFileSupplier {
        String get(Path taskDirectory) throws Exception;
    }
}

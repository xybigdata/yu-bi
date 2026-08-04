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

import yubi.core.base.consts.AttachmentType;
import yubi.core.base.consts.FileOwner;
import yubi.core.common.FileUtils;
import yubi.core.entity.Download;
import yubi.core.mappers.ext.DownloadMapperExt;
import yubi.server.artifact.ArtifactAccess;
import yubi.server.artifact.ArtifactDescriptor;
import yubi.server.artifact.ArtifactTasks;
import yubi.server.artifact.TaskHandle;
import yubi.server.base.params.DownloadCreateParam;
import yubi.server.service.AttachmentService;
import yubi.server.service.BaseService;
import yubi.server.service.DownloadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Function;

@Slf4j
@Service
public class DownloadServiceImpl extends BaseService implements DownloadService {

    private final DownloadMapperExt downloadMapper;
    private final ArtifactTasks artifactTasks;
    private final Function<AttachmentType, AttachmentService> attachmentServiceResolver;

    @Autowired
    public DownloadServiceImpl(DownloadMapperExt downloadMapper, ArtifactTasks artifactTasks) {
        this(downloadMapper, artifactTasks, AttachmentService::matchAttachmentService);
    }

    DownloadServiceImpl(DownloadMapperExt downloadMapper,
                        ArtifactTasks artifactTasks,
                        Function<AttachmentType, AttachmentService> attachmentServiceResolver) {
        this.downloadMapper = downloadMapper;
        this.artifactTasks = artifactTasks;
        this.attachmentServiceResolver = attachmentServiceResolver;
    }

    @Override
    public void requirePermission(Download entity, int permission) {

    }

    @Override
    public TaskHandle submitDownloadTask(DownloadCreateParam downloadParams) {
        if (downloadParams == null || downloadParams.getDownloadParams() == null) {
            return null;
        }
        AttachmentType downloadType = downloadParams.getDownloadType() == null
                ? AttachmentType.EXCEL
                : downloadParams.getDownloadType();
        String fileName = StringUtils.isEmpty(downloadParams.getFileName())
                ? "download"
                : downloadParams.getFileName();
        ArtifactDescriptor descriptor = new ArtifactDescriptor(
                fileName,
                mediaType(downloadType),
                downloadType.getSuffix(),
                "VISUALIZATION"
        );
        return artifactTasks.submit(
                ArtifactAccess.authenticated(getCurrentUser().getId(), downloadParams.getOrgId(),
                        getCurrentUser().getUsername()),
                descriptor,
                context -> createAttachment(downloadParams, downloadType, fileName, context.output(),
                        context.executionUser())
        );
    }

    private void createAttachment(DownloadCreateParam downloadParams,
                                  AttachmentType downloadType,
                                  String fileName,
                                  java.io.OutputStream output,
                                  String executionUser) throws Exception {
        File temporaryFile = null;
        try {
            securityManager.runAs(executionUser);
            AttachmentService attachmentService = attachmentServiceResolver.apply(downloadType);
            temporaryFile = attachmentService.getFile(
                    downloadParams,
                    FileUtils.withBasePath(FileOwner.DOWNLOAD.getPath()),
                    fileName
            );
            Files.copy(temporaryFile.toPath(), output);
        } catch (Exception exception) {
            throw ArtifactExportFailures.classify(downloadType, exception);
        } finally {
            if (temporaryFile != null) {
                FileUtils.delete(temporaryFile);
            }
            securityManager.releaseRunAs();
        }
    }

    private String mediaType(AttachmentType downloadType) {
        return switch (downloadType) {
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case IMAGE -> "image/png";
            case PDF -> "application/pdf";
        };
    }

}

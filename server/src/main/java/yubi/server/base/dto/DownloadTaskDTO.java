package yubi.server.base.dto;

import yubi.core.entity.Download;

public record DownloadTaskDTO(String id, String name, Byte status) {

    public static DownloadTaskDTO from(Download download) {
        return new DownloadTaskDTO(download.getId(), download.getName(), download.getStatus());
    }
}

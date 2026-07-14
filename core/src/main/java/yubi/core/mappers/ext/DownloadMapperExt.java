package yubi.core.mappers.ext;

import yubi.core.entity.Download;
import yubi.core.mappers.DownloadMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface DownloadMapperExt extends DownloadMapper {

    String DOWNLOAD_COLUMNS = "id, `name`, `path`, last_download_time, create_time, create_by, `status`, "
            + "owner_type, owner_id, share_id, failure_code, deleted_at";

    @Select({
            "SELECT " + DOWNLOAD_COLUMNS,
            "FROM download",
            "WHERE owner_type = 'AUTHENTICATED'",
            "AND owner_id = #{ownerId}",
            "AND deleted_at IS NULL",
            "AND create_time > #{createdAfter}",
            "ORDER BY create_time DESC"
    })
    List<Download> selectAuthenticatedTasks(
            @Param("ownerId") String ownerId,
            @Param("createdAfter") Date createdAfter
    );

    @Select({
            "SELECT " + DOWNLOAD_COLUMNS,
            "FROM download",
            "WHERE owner_type = 'SHARE'",
            "AND share_id = #{shareId}",
            "AND owner_id = #{ownerId}",
            "AND deleted_at IS NULL",
            "AND create_time > #{createdAfter}",
            "ORDER BY create_time DESC"
    })
    List<Download> selectSharedTasks(
            @Param("shareId") String shareId,
            @Param("ownerId") String ownerId,
            @Param("createdAfter") Date createdAfter
    );

    @Select({
            "SELECT " + DOWNLOAD_COLUMNS,
            "FROM download",
            "WHERE id = #{id}",
            "AND owner_type = 'AUTHENTICATED'",
            "AND owner_id = #{ownerId}",
            "AND deleted_at IS NULL"
    })
    Download selectAuthenticatedTask(
            @Param("id") String id,
            @Param("ownerId") String ownerId
    );

    @Select({
            "SELECT " + DOWNLOAD_COLUMNS,
            "FROM download",
            "WHERE id = #{id}",
            "AND owner_type = 'SHARE'",
            "AND share_id = #{shareId}",
            "AND owner_id = #{ownerId}",
            "AND deleted_at IS NULL"
    })
    Download selectSharedTask(
            @Param("id") String id,
            @Param("shareId") String shareId,
            @Param("ownerId") String ownerId
    );

    @Update({
            "UPDATE download",
            "SET last_download_time = #{downloadedAt}, `status` = 2",
            "WHERE id = #{id}",
            "AND owner_type = 'AUTHENTICATED'",
            "AND owner_id = #{ownerId}",
            "AND deleted_at IS NULL",
            "AND `status` >= 1"
    })
    int markAuthenticatedTaskDownloaded(
            @Param("id") String id,
            @Param("ownerId") String ownerId,
            @Param("downloadedAt") Date downloadedAt
    );

    @Update({
            "UPDATE download",
            "SET last_download_time = #{downloadedAt}, `status` = 2",
            "WHERE id = #{id}",
            "AND owner_type = 'SHARE'",
            "AND share_id = #{shareId}",
            "AND owner_id = #{ownerId}",
            "AND deleted_at IS NULL",
            "AND `status` >= 1"
    })
    int markSharedTaskDownloaded(
            @Param("id") String id,
            @Param("shareId") String shareId,
            @Param("ownerId") String ownerId,
            @Param("downloadedAt") Date downloadedAt
    );
}

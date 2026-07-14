package yubi.core.mappers;

import yubi.core.entity.Download;
import org.apache.ibatis.jdbc.SQL;

public class DownloadSqlProvider {
    public String insertSelective(Download record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("download");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getName() != null) {
            sql.VALUES("`name`", "#{name,jdbcType=VARCHAR}");
        }
        
        if (record.getPath() != null) {
            sql.VALUES("`path`", "#{path,jdbcType=VARCHAR}");
        }
        
        if (record.getLastDownloadTime() != null) {
            sql.VALUES("last_download_time", "#{lastDownloadTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getCreateTime() != null) {
            sql.VALUES("create_time", "#{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getCreateBy() != null) {
            sql.VALUES("create_by", "#{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getStatus() != null) {
            sql.VALUES("`status`", "#{status,jdbcType=TINYINT}");
        }

        if (record.getOwnerType() != null) {
            sql.VALUES("owner_type", "#{ownerType,jdbcType=VARCHAR}");
        }

        if (record.getOwnerId() != null) {
            sql.VALUES("owner_id", "#{ownerId,jdbcType=VARCHAR}");
        }

        if (record.getShareId() != null) {
            sql.VALUES("share_id", "#{shareId,jdbcType=VARCHAR}");
        }

        if (record.getFailureCode() != null) {
            sql.VALUES("failure_code", "#{failureCode,jdbcType=VARCHAR}");
        }

        if (record.getDeletedAt() != null) {
            sql.VALUES("deleted_at", "#{deletedAt,jdbcType=TIMESTAMP}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(Download record) {
        SQL sql = new SQL();
        sql.UPDATE("download");
        
        if (record.getName() != null) {
            sql.SET("`name` = #{name,jdbcType=VARCHAR}");
        }
        
        if (record.getPath() != null) {
            sql.SET("`path` = #{path,jdbcType=VARCHAR}");
        }
        
        if (record.getLastDownloadTime() != null) {
            sql.SET("last_download_time = #{lastDownloadTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getCreateTime() != null) {
            sql.SET("create_time = #{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getCreateBy() != null) {
            sql.SET("create_by = #{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getStatus() != null) {
            sql.SET("`status` = #{status,jdbcType=TINYINT}");
        }

        if (record.getOwnerType() != null) {
            sql.SET("owner_type = #{ownerType,jdbcType=VARCHAR}");
        }

        if (record.getOwnerId() != null) {
            sql.SET("owner_id = #{ownerId,jdbcType=VARCHAR}");
        }

        if (record.getShareId() != null) {
            sql.SET("share_id = #{shareId,jdbcType=VARCHAR}");
        }

        if (record.getFailureCode() != null) {
            sql.SET("failure_code = #{failureCode,jdbcType=VARCHAR}");
        }

        if (record.getDeletedAt() != null) {
            sql.SET("deleted_at = #{deletedAt,jdbcType=TIMESTAMP}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}

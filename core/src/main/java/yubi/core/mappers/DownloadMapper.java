package yubi.core.mappers;

import yubi.core.entity.Download;
import yubi.core.mappers.ext.CRUDMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;

public interface DownloadMapper extends CRUDMapper {
    @Delete({
        "delete from download",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into download (id, `name`, ",
        "`path`, last_download_time, ",
        "create_time, create_by, ",
        "`status`, owner_type, ",
        "owner_id, share_id, ",
        "failure_code, deleted_at)",
        "values (#{id,jdbcType=VARCHAR}, #{name,jdbcType=VARCHAR}, ",
        "#{path,jdbcType=VARCHAR}, #{lastDownloadTime,jdbcType=TIMESTAMP}, ",
        "#{createTime,jdbcType=TIMESTAMP}, #{createBy,jdbcType=VARCHAR}, ",
        "#{status,jdbcType=TINYINT}, #{ownerType,jdbcType=VARCHAR}, ",
        "#{ownerId,jdbcType=VARCHAR}, #{shareId,jdbcType=VARCHAR}, ",
        "#{failureCode,jdbcType=VARCHAR}, #{deletedAt,jdbcType=TIMESTAMP})"
    })
    int insert(Download record);

    @InsertProvider(type=DownloadSqlProvider.class, method="insertSelective")
    int insertSelective(Download record);

    @Select({
        "select",
        "id, `name`, `path`, last_download_time, create_time, create_by, `status`, ",
        "owner_type, owner_id, share_id, failure_code, deleted_at",
        "from download",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="path", property="path", jdbcType=JdbcType.VARCHAR),
        @Result(column="last_download_time", property="lastDownloadTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="status", property="status", jdbcType=JdbcType.TINYINT),
        @Result(column="owner_type", property="ownerType", jdbcType=JdbcType.VARCHAR),
        @Result(column="owner_id", property="ownerId", jdbcType=JdbcType.VARCHAR),
        @Result(column="share_id", property="shareId", jdbcType=JdbcType.VARCHAR),
        @Result(column="failure_code", property="failureCode", jdbcType=JdbcType.VARCHAR),
        @Result(column="deleted_at", property="deletedAt", jdbcType=JdbcType.TIMESTAMP)
    })
    Download selectByPrimaryKey(String id);

    @UpdateProvider(type=DownloadSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Download record);

    @Update({
        "update download",
        "set `name` = #{name,jdbcType=VARCHAR},",
          "`path` = #{path,jdbcType=VARCHAR},",
          "last_download_time = #{lastDownloadTime,jdbcType=TIMESTAMP},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "`status` = #{status,jdbcType=TINYINT},",
          "owner_type = #{ownerType,jdbcType=VARCHAR},",
          "owner_id = #{ownerId,jdbcType=VARCHAR},",
          "share_id = #{shareId,jdbcType=VARCHAR},",
          "failure_code = #{failureCode,jdbcType=VARCHAR},",
          "deleted_at = #{deletedAt,jdbcType=TIMESTAMP}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Download record);
}

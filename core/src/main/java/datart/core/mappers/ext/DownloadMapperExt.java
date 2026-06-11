package datart.core.mappers.ext;

import datart.core.entity.Download;
import datart.core.mappers.DownloadMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface DownloadMapperExt extends DownloadMapper {
    @Select({
            "SELECT " +
                    " * " +
                    "FROM " +
                    " download " +
                    "WHERE" +
                    " create_by = #{userId} and create_time > #{createdAfter} order by create_time desc"
    })
    List<Download> selectByCreator(
            @Param("userId") String userId,
            @Param("createdAfter") Date createdAfter
    );

}

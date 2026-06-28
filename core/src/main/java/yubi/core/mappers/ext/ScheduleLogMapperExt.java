package yubi.core.mappers.ext;

import yubi.core.entity.ScheduleLog;
import yubi.core.mappers.ScheduleLogMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleLogMapperExt extends ScheduleLogMapper {

    @Select({
            "SELECT * FROM schedule_log WHERE schedule_id=#{scheduleId}",
            " ORDER BY start DESC LIMIT #{limit}"
    })
    List<ScheduleLog> selectByScheduleId(String scheduleId, int limit);

}

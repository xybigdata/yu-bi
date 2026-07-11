package yubi.core.mappers.ext;

import yubi.core.entity.OrgSettings;
import yubi.core.mappers.OrgSettingsMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgSettingsMapperExt extends OrgSettingsMapper {

    @Select({
            "SELECT * FROM org_settings t WHERE t.org_id = #{orgId}"
    })
    List<OrgSettings> selectByOrg(String orgId);

    @Select({
            "SELECT * FROM org_settings t WHERE t.org_id = #{orgId} and `type`=#{type}"
    })
    OrgSettings selectByOrgAndType(String orgId, String type);

}

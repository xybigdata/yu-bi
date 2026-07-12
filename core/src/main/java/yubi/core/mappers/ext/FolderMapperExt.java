package yubi.core.mappers.ext;

import yubi.core.entity.Folder;
import yubi.core.mappers.FolderMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@Mapper
@CacheNamespace(flushInterval = 5 * 1000)
public interface FolderMapperExt extends FolderMapper {

    @Select({
            "SELECT * FROM folder t WHERE t.org_id = #{orgId}"
    })
    List<Folder> selectByOrg(String orgId);

    @Select({
            "SELECT * FROM folder t WHERE t.parent_id = #{parentId}"
    })
    List<Folder> selectByParentId(String parentId);

    @Select({
            "SELECT * FROM folder t WHERE t.parent_id = #{parentId}"
    })
    Folder selectByParentIdAndName(String parentId, String name);

    @Select({
            "<script>",
            "SELECT * FROM folder  WHERE org_id=#{orgId} AND `name` = #{name}",
            "<if test=\"parentId==null\">",
            " AND parent_id IS NULL ",
            "</if>",
            "<if test=\"parentId!=null\">",
            " AND parent_id=#{parentId} ",
            "</if>",
            "</script>",
    })
    List<Folder> checkVizName(String orgId, String parentId, String name);

    @Select({
            "<script>",
            "SELECT * FROM folder WHERE org_id=#{orgId} AND `name`=#{name}",
            "<if test='parentId == null'> AND parent_id IS NULL </if>",
            "<if test='parentId != null'> AND parent_id=#{parentId} </if>",
            "FOR UPDATE",
            "</script>"
    })
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<Folder> selectControlledWriteCurrentNamesForUpdate(@Param("orgId") String orgId,
                                                            @Param("parentId") String parentId,
                                                            @Param("name") String name);

    @Delete({
            "DELETE FROM folder WHERE rel_type=#{relType} and rel_id=#{relId}"
    })
    int deleteByRelTypeAndId(String relType, String relId);

    @Select({
            "SELECT * FROM folder WHERE rel_type=#{relType} and rel_id=#{relId}"
    })
    Folder selectByRelTypeAndId(String relType, String relId);

    @Select("SELECT * FROM folder WHERE id=#{id} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Folder selectControlledWriteCurrentForUpdate(@Param("id") String id);

    @Select("SELECT * FROM folder WHERE rel_type=#{relType} AND rel_id=#{relId} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Folder selectControlledWriteCurrentByRelationForUpdate(@Param("relType") String relType,
                                                            @Param("relId") String relId);

    @Select("SELECT id FROM organization WHERE id=#{orgId} FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    String selectControlledWriteRootNamespaceForUpdate(@Param("orgId") String orgId);


    @Select({
            "<script>",
            "SELECT * FROM folder WHERE ",
            "		<if test='ids != null and ids.size > 0'>",
            "			 id in",
            "			<foreach collection='ids' item='item' index='index' open='(' close=')' separator=','>",
            "				#{item}",
            "			</foreach>",
            "		</if>",
            "		<if test='ids == null or ids.size == 0'>",
            "		 1=0",
            "		</if>",
            "</script>"
    })
    List<Folder> selectByPrimaryKeys(Set<String> ids);


}

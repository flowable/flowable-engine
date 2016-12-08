package flowable.mappers;

import org.apache.ibatis.annotations.Select;

/**
 * @author Dominik Bartos
 */
public interface CustomMybatisMapper {

    @Select("SELECT ID_ FROM ACT_RE_PROCDEF WHERE KEY_ = #{key}")
    String loadProcessDefinitionIdByKey(String key);
}

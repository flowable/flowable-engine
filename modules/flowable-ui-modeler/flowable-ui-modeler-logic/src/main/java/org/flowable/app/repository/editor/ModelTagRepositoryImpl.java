/**
 * 
 */
package org.flowable.app.repository.editor;

import java.util.List;

import org.flowable.app.domain.editor.ModelTag;
import org.flowable.app.repository.UuidIdGenerator;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */

@Component
public class ModelTagRepositoryImpl implements ModelTagRepository
{
  private static final String NAMESPACE = "org.flowable.app.domain.editor.ModelTag.";

  @Autowired
  protected SqlSessionTemplate sqlSessionTemplate;

  @Autowired
  protected UuidIdGenerator idGenerator;

  @Override
  public ModelTag get(String id) {
      return sqlSessionTemplate.selectOne(NAMESPACE + "selectModelTagById", id);
  }
  
  @Override
  public void save(ModelTag modelTag) {
      if (modelTag.getId() == null) {
          modelTag.setId(idGenerator.generateId());
          sqlSessionTemplate.insert(NAMESPACE + "insertModelTag", modelTag);
      } else {
          sqlSessionTemplate.update(NAMESPACE + "updateModelTag", modelTag);
      }
  }

  @Override
  public List<ModelTag> findAll() {      
      return sqlSessionTemplate.selectList(NAMESPACE + "selectAll");
  }

  @Override
  public void delete(ModelTag model) {
      sqlSessionTemplate.delete(NAMESPACE + "deleteModelTag", model);
  }

  @Override
  public Long countModelByTagId(String tagId)
  {
    return sqlSessionTemplate.selectOne(NAMESPACE + "countModelByTagId", tagId);
  }
  
  
  
}

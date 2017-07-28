/**
 * 
 */
package org.flowable.app.service.editor;

import java.util.Calendar;
import java.util.List;

import org.flowable.app.domain.editor.ModelTag;
import org.flowable.app.repository.editor.ModelTagRepository;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.api.ModelTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */

@Service
@Transactional
public class ModelTageServiceImpl implements ModelTagService
{
  @Autowired
  protected ModelTagRepository modelTagRepository;
  
  @Override
  public boolean deleteModelTag(String tagId) {
    
      long refCounts = modelTagRepository.countModelByTagId(tagId);
      if(refCounts>0) {
        return false;
      }

      ModelTag modelTag = modelTagRepository.get(tagId);
      if (modelTag == null) {
          throw new IllegalArgumentException("No model found with id: " + tagId);
      }

      // TODO: cannot delete if tag is used by any model      
      modelTagRepository.delete(modelTag);
      return true;
  }

  @Override
  public List<ModelTag> findAll()
  {
    return this.modelTagRepository.findAll();
  }

  @Override
  public void saveModelTag(ModelTag modelTag)
  {
    this.modelTagRepository.save(modelTag);    
  }
  
  public void createModelTag(String name) {
    ModelTag tag = new ModelTag();
    tag.setName(name);
    tag.setCreated(Calendar.getInstance().getTime());
    tag.setCreatedBy(SecurityUtils.getCurrentUserObject().getId());
    tag.setUpdated(Calendar.getInstance().getTime());
    tag.setUpdatedBy(SecurityUtils.getCurrentUserObject().getId());
    this.saveModelTag(tag);
  }

  @Override
  public void updateModelTag(String tagId, String name)
  {
    ModelTag tag = this.modelTagRepository.get(tagId);
    tag.setName(name);
    tag.setUpdated(Calendar.getInstance().getTime());
    tag.setUpdatedBy(SecurityUtils.getCurrentUserObject().getId());
    this.saveModelTag(tag);
  }
}

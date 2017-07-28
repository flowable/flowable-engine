/**
 * 
 */
package org.flowable.app.service.api;

import java.util.List;

import org.flowable.app.domain.editor.ModelTag;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */
public interface ModelTagService
{
  void createModelTag(String name);
  
  void updateModelTag(String tagId, String name);
  
  void saveModelTag(ModelTag modelTag);
  
  List<ModelTag> findAll();
  
  boolean deleteModelTag(String modelId);
}

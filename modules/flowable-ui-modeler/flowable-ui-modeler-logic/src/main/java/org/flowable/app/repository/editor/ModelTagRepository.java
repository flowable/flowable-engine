/**
 * 
 */
package org.flowable.app.repository.editor;

import java.util.List;

import org.flowable.app.domain.editor.Model;
import org.flowable.app.domain.editor.ModelTag;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */
public interface ModelTagRepository
{
  void save(ModelTag model);

  void delete(ModelTag model);
  
  ModelTag get(String id);

  List<ModelTag> findAll();
  
  Long countModelByTagId(String tagId);
}

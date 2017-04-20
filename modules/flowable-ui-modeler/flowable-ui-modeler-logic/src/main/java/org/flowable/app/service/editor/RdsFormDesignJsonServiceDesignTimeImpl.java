/**
 * 
 */
package org.flowable.app.service.editor;

import java.util.List;

import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.repository.editor.ModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import au.com.rds.schemaformbuilder.formdesignjson.FormDesignJsonServiceBase;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */

@Service
public class RdsFormDesignJsonServiceDesignTimeImpl extends FormDesignJsonServiceBase
{
  @Autowired
  ModelRepository modelRepo;
  
  @Override
  public String getFormDesignJsonByKey(String formKey)
  {
    List<Model> models =  modelRepo.findByKeyAndType(formKey, AbstractModel.MODEL_TYPE_FORM_RDS);
    Assert.isTrue(models.size()==1, "Should get 1 and only 1 rds_form by key, but now get " + models.size());
    Model model = models.get(0);
    return model.getModelEditorJson();
  }

}

/**
 *
 */
package org.flowable.app.service.editor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.repository.editor.ModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import au.com.rds.schemaformbuilder.formdesignjson.FormDesignJsonServiceBase;
import au.com.rds.schemaformbuilder.util.JsonUtils;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */

@Service
public class FormDesignJsonServiceDesignTimeImpl extends FormDesignJsonServiceBase
{
  @Autowired
  ModelRepository modelRepo;

  @Override
  public String getFormDesignJsonByKey(String formKey)
  {
    List<Model> models =  modelRepo.findByKeyAndType(formKey, AbstractModel.MODEL_TYPE_FORM_RDS);
    if (models.size() != 1)
    {
      throw new IllegalArgumentException(
          "Should get 1 and only 1 rds_form by key [" + formKey + "], but now get " + models.size());
    }
    Model model = models.get(0);
    return model.getModelEditorJson();
  }

  /* (non-Javadoc)
   * @see au.com.rds.schemaformbuilder.formdesignjson.FormDesignJsonService#findAllFormDesignKeyJsonPairs()
   */
  @Override
  public List<Pair<String, String>> findAllFormDesignKeyJsonPairs()
  {
    List<Model> models =  modelRepo.findByModelType(AbstractModel.MODEL_TYPE_FORM_RDS, null);
    List<Pair<String, String>> resultList = new LinkedList<Pair<String, String>>();
    for (Model model:models) {
      resultList.add(Pair.of(model.getKey(), model.getModelEditorJson()));
    }
    return resultList;
  }


  @Override
  public ObjectNode findByKeyWithReferencedBy(String formKey)
  {
    List<Model> models =  modelRepo.findByKeyAndType(formKey, AbstractModel.MODEL_TYPE_FORM_RDS);
    Assert.isTrue(models.size()==1, "Should get 1 and only 1 rds_form by key, but now get " + models.size());
    Model model = models.get(0);
    String formDesignJson = model.getModelEditorJson();

    ObjectNode result = JsonUtils.newObject();
    result.set("key", JsonUtils.textNode(formKey));
    result.set("definition", JsonUtils.textNode(formDesignJson));

    List<String> referencedByList = findFormKeysReferencingMe(formKey);
    ArrayNode referencedByArrayNode = JsonUtils.arrayNode();
    for (String referencedByKey : referencedByList)
    {
      referencedByArrayNode.add(referencedByKey);
    }
    result.set("referencedBy", referencedByArrayNode);

    result.set("lastUpdated", JsonUtils.numberNode(model.getLastUpdated().getTime()));
    result.set("name", JsonUtils.textNode(model.getName()));
    result.set("desc", JsonUtils.textNode(model.getDescription()));
    result.set("id", JsonUtils.textNode(model.getId()));
    return result;
  }

}

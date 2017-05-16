/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.app.rest.editor;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.util.Asserts;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.domain.editor.ModelInformation;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.editor.ModelRelationService;
import org.flowable.app.service.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import au.com.rds.schemaformbuilder.formdesignjson.FormDesignJsonService;

@RestController
public class ModelRelationResource {

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ModelRelationService modelRelationService;
    
    @Autowired
    protected ModelRepository modelRepository;
    
    @Autowired
    protected FormDesignJsonService formDesignJsonService;

    @RequestMapping(value = "/rest/models/{modelId}/parent-relations", method = RequestMethod.GET, produces = "application/json")
    public List<ModelInformation> getModelRelations(@PathVariable String modelId) {
        Model model = modelService.getModel(modelId);
        if (model == null) {
            throw new NotFoundException();
        }
        List<ModelInformation> referringForm = new ArrayList<ModelInformation>();
        if(model.getModelType().intValue() == AbstractModel.MODEL_TYPE_FORM_RDS){
          List<String> keys = this.formDesignJsonService.findFormKeysReferencingMe(model.getKey());
          for(String key: keys) {
            List<Model> models = modelRepository.findByKeyAndType(key, AbstractModel.MODEL_TYPE_FORM_RDS);
            Assert.isTrue(models.size()==1, "Should return 1 and only 1 result for form key " + key);
            Model referringModel = models.get(0);
            ModelInformation modelInfo = new ModelInformation(referringModel.getId(),referringModel.getName(),referringModel.getModelType());
            referringForm.add(modelInfo);
          }
        }
        referringForm.addAll(modelRelationService.findParentModels(modelId));
        return referringForm;
    }

}

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
package org.flowable.ui.modeler.rest.app;

import java.util.List;

import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.domain.ModelInformation;
import org.flowable.ui.modeler.service.ModelRelationService;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app")
public class ModelRelationResource {

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ModelRelationService modelRelationService;

    @RequestMapping(value = "/rest/models/{modelId}/parent-relations", method = RequestMethod.GET, produces = "application/json")
    public List<ModelInformation> getModelRelations(@PathVariable String modelId) {
        Model model = modelService.getModel(modelId);
        if (model == null) {
            throw new NotFoundException();
        }
        return modelRelationService.findParentModels(modelId);
    }

}

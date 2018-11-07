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
package org.flowable.ui.modeler.service;

import java.util.List;

import org.flowable.ui.modeler.domain.ModelInformation;
import org.flowable.ui.modeler.domain.ModelRelation;
import org.flowable.ui.modeler.repository.ModelRelationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Joram Barrez
 */
@Service
@Transactional
public class ModelRelationService {

    @Autowired
    private ModelRelationRepository modelRelationRepository;

    /**
     * Returns the models which have a {@link ModelRelation} with the given model, where the model references those directly or indirectly.
     * 
     * ie modelId here is the parent of the relation.
     */
    public List<ModelInformation> findReferencedModels(String modelId) {
        return modelRelationRepository.findModelInformationByParentModelId(modelId);
    }

    /**
     * Returns the models which have a {@link ModelRelation} with the given model, where the given model is the one that is referenced.
     * 
     * ie modelId is the child of the relation
     */
    public List<ModelInformation> findParentModels(String modelId) {
        return modelRelationRepository.findModelInformationByChildModelId(modelId);
    }

}

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

package org.flowable.rest.service.api.repository;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Frederik Heremans
 */
public class BaseModelResource {

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    /**
     * Returns the {@link Model} that is requested. Throws the right exceptions when bad request was made or model is not found.
     */
    protected Model getModelFromRequest(String modelId) {
        Model model = repositoryService.createModelQuery().modelId(modelId).singleResult();

        if (model == null) {
            throw new FlowableObjectNotFoundException("Could not find a model with id '" + modelId + "'.", ProcessDefinition.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessModelInfoById(model);
        }
        
        return model;
    }
}

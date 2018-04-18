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

package org.flowable.cmmn.rest.service.api.repository;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class BaseCaseDefinitionResource {

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnRepositoryService repositoryService;

    /**
     * Returns the {@link CaseDefinition} that is requested. Throws the right exceptions when bad request was made or definition is not found.
     */
    protected CaseDefinition getCaseDefinitionFromRequest(String caseDefinitionId) {
        CaseDefinition caseDefinition = repositoryService.getCaseDefinition(caseDefinitionId);

        if (caseDefinition == null) {
            throw new FlowableObjectNotFoundException("Could not find a case definition with id '" + caseDefinitionId + "'.", CaseDefinition.class);
        }
        return caseDefinition;
    }
}

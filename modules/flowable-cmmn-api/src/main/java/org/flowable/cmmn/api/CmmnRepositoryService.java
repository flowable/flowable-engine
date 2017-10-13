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
package org.flowable.cmmn.api;

import java.io.InputStream;
import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.api.repository.CmmnDeploymentQuery;
import org.flowable.cmmn.model.CmmnModel;

public interface CmmnRepositoryService {

    CmmnDeploymentBuilder createDeployment();

    List<String> getDeploymentResourceNames(String deploymentId);

    InputStream getResourceAsStream(String deploymentId, String resourceName);
    
    CaseDefinition getCaseDefinition(String caseDefinitionId);

    CmmnModel getCmmnModel(String caseDefinitionId);
    
    void deleteDeployment(String deploymentId, boolean cascade);
    
    CmmnDeploymentQuery createDeploymentQuery();
    
    CaseDefinitionQuery createCaseDefinitionQuery();

}

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
package org.flowable.cmmn.engine.impl.persistence.entity.data;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionQueryImpl;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public interface CaseDefinitionDataManager extends DataManager<CaseDefinitionEntity> {

    CaseDefinitionEntity findLatestCaseDefinitionByKey(String caseDefinitionKey);

    CaseDefinitionEntity findLatestCaseDefinitionByKeyAndTenantId(String caseDefinitionKey, String tenantId);

    void deleteCaseDefinitionsByDeploymentId(String deploymentId);

    CaseDefinitionEntity findCaseDefinitionByDeploymentAndKey(String deploymentId, String caseDefinitionKey);

    CaseDefinitionEntity findCaseDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String caseDefinitionKey, String tenantId);

    CaseDefinitionEntity findCaseDefinitionByKeyAndVersion(String caseDefinitionKey, Integer caseDefinitionVersion);

    CaseDefinitionEntity findCaseDefinitionByKeyAndVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId);

    void updateCaseDefinitionTenantIdForDeployment(String deploymentId, String newTenantId);
    
    List<CaseDefinition> findCaseDefinitionsByQueryCriteria(CaseDefinitionQueryImpl caseDefinitionQuery);

    long findCaseDefinitionCountByQueryCriteria(CaseDefinitionQueryImpl caseDefinitionQuery);

}

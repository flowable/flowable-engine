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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;

/**
 * @author Joram Barrez
 */
public interface CaseDefinitionEntityManager extends EntityManager<CaseDefinitionEntity> {

    CaseDefinitionEntity findLatestCaseDefinitionByKey(String caseDefinitionKey);

    CaseDefinitionEntity findLatestCaseDefinitionByKeyAndTenantId(String caseDefinitionKey, String tenantId);
    
    CaseDefinitionEntity findCaseDefinitionByDeploymentAndKey(String deploymentId, String caseDefinitionKey);

    CaseDefinitionEntity findCaseDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String caseDefinitionKey, String tenantId);

    CaseDefinition findCaseDefinitionByKeyAndVersionAndTenantId(String caseDefinitionKey, Integer caseDefinitionVersion, String tenantId);
    
    void deleteCaseDefinitionAndRelatedData(String caseDefinitionId, boolean cascadeHistory);
    
    CaseDefinitionQuery createCaseDefinitionQuery();
    
    List<CaseDefinition> findCaseDefinitionsByQueryCriteria(CaseDefinitionQuery caseDefinitionQuery);

    long findCaseDefinitionCountByQueryCriteria(CaseDefinitionQuery caseDefinitionQuery);

}
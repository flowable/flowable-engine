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
package org.flowable.dmn.engine.impl.persistence.entity.data;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.impl.DecisionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;

/**
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public interface DecisionDataManager extends DataManager<DecisionEntity> {

    DecisionEntity findLatestDecisionByKey(String definitionKey);

    DecisionEntity findLatestDecisionByKeyAndTenantId(String definitionKey, String tenantId);

    DecisionEntity findLatestDecisionByKeyAndParentDeploymentId(String definitionKey, String parentDeploymentId);

    DecisionEntity findLatestDecisionByKeyParentDeploymentIdAndTenantId(String definitionKey,
            String parentDeploymentId, String tenantId);

    void deleteDecisionsByDeploymentId(String deploymentId);

    List<DmnDecision> findDecisionsByQueryCriteria(DecisionQueryImpl DefinitionQuery);

    long findDecisionCountByQueryCriteria(DecisionQueryImpl DefinitionQuery);

    DecisionEntity findDecisionByDeploymentAndKey(String deploymentId, String definitionKey);

    DecisionEntity findDecisionByDeploymentAndKeyAndTenantId(String deploymentId, String definitionKey, String tenantId);

    DecisionEntity findDecisionByKeyAndVersion(String definitionKey, Integer definitionVersion);

    DecisionEntity findDecisionByKeyAndVersionAndTenantId(String definitionKey, Integer definitionVersion, String tenantId);

    List<DmnDecision> findDecisionsByNativeQuery(Map<String, Object> parameterMap);

    long findDecisionCountByNativeQuery(Map<String, Object> parameterMap);

    void updateDecisionTenantIdForDeployment(String deploymentId, String newTenantId);

}

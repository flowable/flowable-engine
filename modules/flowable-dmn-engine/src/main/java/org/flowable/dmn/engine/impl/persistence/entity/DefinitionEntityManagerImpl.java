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

package org.flowable.dmn.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DecisionQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.DecisionDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public class DefinitionEntityManagerImpl
    extends AbstractEngineEntityManager<DmnEngineConfiguration, DecisionEntity, DecisionDataManager>
    implements DecisionEntityManager {

    public DefinitionEntityManagerImpl(DmnEngineConfiguration dmnEngineConfiguration, DecisionDataManager DefinitionDataManager) {
        super(dmnEngineConfiguration, DefinitionDataManager);
    }

    @Override
    public DecisionEntity findLatestDecisionByKey(String DefinitionKey) {
        return dataManager.findLatestDecisionByKey(DefinitionKey);
    }

    @Override
    public DecisionEntity findLatestDecisionByKeyAndTenantId(String DefinitionKey, String tenantId) {
        return dataManager.findLatestDecisionByKeyAndTenantId(DefinitionKey, tenantId);
    }

    @Override
    public void deleteDecisionsByDeploymentId(String deploymentId) {
        dataManager.deleteDecisionsByDeploymentId(deploymentId);
    }

    @Override
    public List<DmnDecision> findDecisionsByQueryCriteria(DecisionQueryImpl DefinitionQuery) {
        return dataManager.findDecisionsByQueryCriteria(DefinitionQuery);
    }

    @Override
    public long findDecisionCountByQueryCriteria(DecisionQueryImpl DefinitionQuery) {
        return dataManager.findDecisionCountByQueryCriteria(DefinitionQuery);
    }

    @Override
    public DecisionEntity findDecisionByDeploymentAndKey(String deploymentId, String DefinitionKey) {
        return dataManager.findDecisionByDeploymentAndKey(deploymentId, DefinitionKey);
    }

    @Override
    public DecisionEntity findDecisionByDeploymentAndKeyAndTenantId(String deploymentId, String decisionKey, String tenantId) {
        return dataManager.findDecisionByDeploymentAndKeyAndTenantId(deploymentId, decisionKey, tenantId);
    }

    @Override
    public DecisionEntity findDecisionByKeyAndVersionAndTenantId(String definitionKey, Integer definitionVersion, String tenantId) {
        if (tenantId == null || DmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return dataManager.findDecisionByKeyAndVersion(definitionKey, definitionVersion);
        } else {
            return dataManager.findDecisionByKeyAndVersionAndTenantId(definitionKey, definitionVersion, tenantId);
        }
    }

    @Override
    public List<DmnDecision> findDecisionsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDecisionsByNativeQuery(parameterMap);
    }

    @Override
    public long findDecisionCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDecisionCountByNativeQuery(parameterMap);
    }

    @Override
    public void updateDecisionTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateDecisionTenantIdForDeployment(deploymentId, newTenantId);
    }

}

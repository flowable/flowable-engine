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
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.DecisionTableQueryImpl;
import org.flowable.dmn.engine.impl.persistence.entity.data.DecisionTableDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DecisionTableEntityManagerImpl
    extends AbstractEngineEntityManager<DmnEngineConfiguration, DecisionTableEntity, DecisionTableDataManager>
    implements DecisionTableEntityManager {

    public DecisionTableEntityManagerImpl(DmnEngineConfiguration dmnEngineConfiguration, DecisionTableDataManager decisionTableDataManager) {
        super(dmnEngineConfiguration, decisionTableDataManager);
    }

    @Override
    public DecisionTableEntity findLatestDecisionTableByKey(String decisionTableKey) {
        return dataManager.findLatestDecisionTableByKey(decisionTableKey);
    }

    @Override
    public DecisionTableEntity findLatestDecisionTableByKeyAndTenantId(String decisionTableKey, String tenantId) {
        return dataManager.findLatestDecisionTableByKeyAndTenantId(decisionTableKey, tenantId);
    }

    @Override
    public void deleteDecisionTablesByDeploymentId(String deploymentId) {
        dataManager.deleteDecisionTablesByDeploymentId(deploymentId);
    }

    @Override
    public List<DmnDecisionTable> findDecisionTablesByQueryCriteria(DecisionTableQueryImpl decisionTableQuery) {
        return dataManager.findDecisionTablesByQueryCriteria(decisionTableQuery);
    }

    @Override
    public long findDecisionTableCountByQueryCriteria(DecisionTableQueryImpl decisionTableQuery) {
        return dataManager.findDecisionTableCountByQueryCriteria(decisionTableQuery);
    }

    @Override
    public DecisionTableEntity findDecisionTableByDeploymentAndKey(String deploymentId, String decisionTableKey) {
        return dataManager.findDecisionTableByDeploymentAndKey(deploymentId, decisionTableKey);
    }

    @Override
    public DecisionTableEntity findDecisionTableByDeploymentAndKeyAndTenantId(String deploymentId, String decisionTableKey, String tenantId) {
        return dataManager.findDecisionTableByDeploymentAndKeyAndTenantId(deploymentId, decisionTableKey, tenantId);
    }

    @Override
    public DecisionTableEntity findDecisionTableByKeyAndVersionAndTenantId(String decisionTableKey, Integer decisionTableVersion, String tenantId) {
        if (tenantId == null || DmnEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return dataManager.findDecisionTableByKeyAndVersion(decisionTableKey, decisionTableVersion);
        } else {
            return dataManager.findDecisionTableByKeyAndVersionAndTenantId(decisionTableKey, decisionTableVersion, tenantId);
        }
    }

    @Override
    public List<DmnDecisionTable> findDecisionTablesByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDecisionTablesByNativeQuery(parameterMap);
    }

    @Override
    public long findDecisionTableCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDecisionTableCountByNativeQuery(parameterMap);
    }

    @Override
    public void updateDecisionTableTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateDecisionTableTenantIdForDeployment(deploymentId, newTenantId);
    }

}

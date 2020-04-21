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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionDataManager;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */
public class ProcessDefinitionEntityManagerImpl
    extends AbstractProcessEngineEntityManager<ProcessDefinitionEntity, ProcessDefinitionDataManager>
    implements ProcessDefinitionEntityManager {

    public ProcessDefinitionEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, ProcessDefinitionDataManager processDefinitionDataManager) {
        super(processEngineConfiguration, processDefinitionDataManager);
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKey(String processDefinitionKey) {
        return dataManager.findLatestProcessDefinitionByKey(processDefinitionKey);
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        return dataManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
    }
    
    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKey(String processDefinitionKey) {
        return dataManager.findLatestDerivedProcessDefinitionByKey(processDefinitionKey);
    }

    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        return dataManager.findLatestDerivedProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
    }

    @Override
    public void deleteProcessDefinitionsByDeploymentId(String deploymentId) {
        dataManager.deleteProcessDefinitionsByDeploymentId(deploymentId);
    }

    @Override
    public List<ProcessDefinition> findProcessDefinitionsByQueryCriteria(ProcessDefinitionQueryImpl processDefinitionQuery) {
        return dataManager.findProcessDefinitionsByQueryCriteria(processDefinitionQuery);
    }

    @Override
    public long findProcessDefinitionCountByQueryCriteria(ProcessDefinitionQueryImpl processDefinitionQuery) {
        return dataManager.findProcessDefinitionCountByQueryCriteria(processDefinitionQuery);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKey(String deploymentId, String processDefinitionKey) {
        return dataManager.findProcessDefinitionByDeploymentAndKey(deploymentId, processDefinitionKey);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String processDefinitionKey, String tenantId) {
        return dataManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(deploymentId, processDefinitionKey, tenantId);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByParentDeploymentAndKey(String parentDeploymentId, String processDefinitionKey) {
        return dataManager.findProcessDefinitionByParentDeploymentAndKey(parentDeploymentId, processDefinitionKey);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByParentDeploymentAndKeyAndTenantId(String parentDeploymentId, String processDefinitionKey, String tenantId) {
        return dataManager.findProcessDefinitionByParentDeploymentAndKeyAndTenantId(parentDeploymentId, processDefinitionKey, tenantId);
    }

    @Override
    public ProcessDefinition findProcessDefinitionByKeyAndVersionAndTenantId(String processDefinitionKey, Integer processDefinitionVersion, String tenantId) {
        if (tenantId == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(tenantId)) {
            return dataManager.findProcessDefinitionByKeyAndVersion(processDefinitionKey, processDefinitionVersion);
        } else {
            return dataManager.findProcessDefinitionByKeyAndVersionAndTenantId(processDefinitionKey, processDefinitionVersion, tenantId);
        }
    }

    @Override
    public List<ProcessDefinition> findProcessDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findProcessDefinitionsByNativeQuery(parameterMap);
    }

    @Override
    public long findProcessDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findProcessDefinitionCountByNativeQuery(parameterMap);
    }

    @Override
    public void updateProcessDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        dataManager.updateProcessDefinitionTenantIdForDeployment(deploymentId, newTenantId);
    }

    @Override
    public void updateProcessDefinitionVersionForProcessDefinitionId(String processDefinitionId, int version) {
        dataManager.updateProcessDefinitionVersionForProcessDefinitionId(processDefinitionId, version);
    }

}

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

import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.engine.impl.DeploymentQueryImpl;
import org.flowable.engine.impl.ModelQueryImpl;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.data.DeploymentDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DeploymentEntityManagerImpl
    extends AbstractProcessEngineEntityManager<DeploymentEntity, DeploymentDataManager>
    implements DeploymentEntityManager {

    public DeploymentEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, DeploymentDataManager deploymentDataManager) {
        super(processEngineConfiguration, deploymentDataManager);
    }

    @Override
    public void insert(DeploymentEntity deployment) {
        insert(deployment, false);

        for (EngineResource resource : deployment.getResources().values()) {
            resource.setDeploymentId(deployment.getId());
            getResourceEntityManager().insert((ResourceEntity) resource);
        }
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl().deploymentId(deploymentId).list();

        updateRelatedModels(deploymentId);

        if (cascade) {
            deleteProcessInstancesForProcessDefinitions(processDefinitions);
            deleteHistoricTaskEventLogEntriesForProcessDefinitions(processDefinitions);
        }

        for (ProcessDefinition processDefinition : processDefinitions) {
            engineConfiguration.getProcessDefinitionDeploymentDeletionManager()
                    .deleteDefinitionForDeployment(processDefinition, deploymentId);
        }

        deleteProcessDefinitionsForDeployment(deploymentId);
        getResourceEntityManager().deleteResourcesByDeploymentId(deploymentId);
        delete(findById(deploymentId), false);
    }

    protected void updateRelatedModels(String deploymentId) {
        // Remove the deployment link from any model.
        // The model will still exists, as a model is a source for a deployment model and has a different lifecycle
        List<Model> models = new ModelQueryImpl().deploymentId(deploymentId).list();
        for (Model model : models) {
            ModelEntity modelEntity = (ModelEntity) model;
            modelEntity.setDeploymentId(null);
            getModelEntityManager().updateModel(modelEntity);
        }
    }

    protected void deleteProcessDefinitionsForDeployment(String deploymentId) {
        getProcessDefinitionEntityManager().deleteProcessDefinitionsByDeploymentId(deploymentId);
    }

    protected void deleteProcessInstancesForProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        for (ProcessDefinition processDefinition : processDefinitions) {
            getExecutionEntityManager().deleteProcessInstancesByProcessDefinition(processDefinition.getId(), "deleted deployment", true);
        }
    }

    protected void deleteHistoricTaskEventLogEntriesForProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        for (ProcessDefinition processDefinition : processDefinitions) {
            engineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForProcessDefinition(processDefinition.getId());
        }
    }

    @Override
    public long findDeploymentCountByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        return dataManager.findDeploymentCountByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<Deployment> findDeploymentsByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        return dataManager.findDeploymentsByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return dataManager.getDeploymentResourceNames(deploymentId);
    }

    @Override
    public List<Deployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDeploymentsByNativeQuery(parameterMap);
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        return dataManager.findDeploymentCountByNativeQuery(parameterMap);
    }

    protected ResourceEntityManager getResourceEntityManager() {
        return engineConfiguration.getResourceEntityManager();
    }

    protected ModelEntityManager getModelEntityManager() {
        return engineConfiguration.getModelEntityManager();
    }

    protected ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
        return engineConfiguration.getProcessDefinitionEntityManager();
    }

    protected ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager() {
        return engineConfiguration.getProcessDefinitionInfoEntityManager();
    }

    protected ExecutionEntityManager getExecutionEntityManager() {
        return engineConfiguration.getExecutionEntityManager();
    }

}

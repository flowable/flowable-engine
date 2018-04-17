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

package org.flowable.engine.impl.persistence.deploy;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.app.AppModel;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Joram Barrez
 */
public class DeploymentManager {

    protected DeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache;
    protected ProcessDefinitionInfoCache processDefinitionInfoCache;
    protected DeploymentCache<Object> appResourceCache;
    protected DeploymentCache<Object> knowledgeBaseCache; // Needs to be object to avoid an import to Drools in this core class
    protected List<EngineDeployer> deployers;

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected ProcessDefinitionEntityManager processDefinitionEntityManager;
    protected DeploymentEntityManager deploymentEntityManager;

    public void deploy(DeploymentEntity deployment) {
        deploy(deployment, null);
    }

    public void deploy(DeploymentEntity deployment, Map<String, Object> deploymentSettings) {
        for (EngineDeployer deployer : deployers) {
            deployer.deploy(deployment, deploymentSettings);
        }
    }

    public ProcessDefinition findDeployedProcessDefinitionById(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Invalid process definition id : null");
        }

        // first try the cache
        ProcessDefinitionCacheEntry cacheEntry = processDefinitionCache.get(processDefinitionId);
        ProcessDefinition processDefinition = cacheEntry != null ? cacheEntry.getProcessDefinition() : null;

        if (processDefinition == null) {
            processDefinition = processDefinitionEntityManager.findById(processDefinitionId);
            if (processDefinition == null) {
                throw new FlowableObjectNotFoundException("no deployed process definition found with id '" + processDefinitionId + "'", ProcessDefinition.class);
            }
            processDefinition = resolveProcessDefinition(processDefinition).getProcessDefinition();
        }
        return processDefinition;
    }

    public ProcessDefinition findDeployedLatestProcessDefinitionByKey(String processDefinitionKey) {
        ProcessDefinition processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKey(processDefinitionKey);

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("no processes deployed with key '" + processDefinitionKey + "'", ProcessDefinition.class);
        }
        processDefinition = resolveProcessDefinition(processDefinition).getProcessDefinition();
        return processDefinition;
    }

    public ProcessDefinition findDeployedLatestProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        ProcessDefinition processDefinition = processDefinitionEntityManager.findLatestProcessDefinitionByKeyAndTenantId(processDefinitionKey, tenantId);
        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("no processes deployed with key '" + processDefinitionKey + "' for tenant identifier '" + tenantId + "'", ProcessDefinition.class);
        }
        processDefinition = resolveProcessDefinition(processDefinition).getProcessDefinition();
        return processDefinition;
    }

    public ProcessDefinition findDeployedProcessDefinitionByKeyAndVersionAndTenantId(String processDefinitionKey, Integer processDefinitionVersion, String tenantId) {
        ProcessDefinition processDefinition = (ProcessDefinitionEntity) processDefinitionEntityManager
                .findProcessDefinitionByKeyAndVersionAndTenantId(processDefinitionKey, processDefinitionVersion, tenantId);
        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("no processes deployed with key = '" + processDefinitionKey + "' and version = '" + processDefinitionVersion + "'", ProcessDefinition.class);
        }
        processDefinition = resolveProcessDefinition(processDefinition).getProcessDefinition();
        return processDefinition;
    }

    /**
     * Resolving the process definition will fetch the BPMN 2.0, parse it and store the {@link BpmnModel} in memory.
     */
    public ProcessDefinitionCacheEntry resolveProcessDefinition(ProcessDefinition processDefinition) {
        String processDefinitionId = processDefinition.getId();
        String deploymentId = processDefinition.getDeploymentId();

        ProcessDefinitionCacheEntry cachedProcessDefinition = processDefinitionCache.get(processDefinitionId);

        if (cachedProcessDefinition == null) {
            if (Flowable5Util.isFlowable5ProcessDefinition(processDefinition, processEngineConfiguration)) {
                return Flowable5Util.getFlowable5CompatibilityHandler().resolveProcessDefinition(processDefinition);
            }

            DeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
            deployment.setNew(false);
            deploy(deployment, null);
            cachedProcessDefinition = processDefinitionCache.get(processDefinitionId);

            if (cachedProcessDefinition == null) {
                throw new FlowableException("deployment '" + deploymentId + "' didn't put process definition '" + processDefinitionId + "' in the cache");
            }
        }
        return cachedProcessDefinition;
    }

    public Object getAppResourceObject(String deploymentId) {
        Object appResourceObject = appResourceCache.get(deploymentId);

        if (appResourceObject == null) {
            boolean appResourcePresent = false;
            List<String> deploymentResourceNames = getDeploymentEntityManager().getDeploymentResourceNames(deploymentId);
            for (String deploymentResourceName : deploymentResourceNames) {
                if (deploymentResourceName.endsWith(".app")) {
                    appResourcePresent = true;
                    break;
                }
            }

            if (appResourcePresent) {
                DeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
                deployment.setNew(false);
                deploy(deployment, null);

            } else {
                throw new FlowableException("No .app resource found for deployment '" + deploymentId + "'");
            }

            appResourceObject = appResourceCache.get(deploymentId);
            if (appResourceObject == null) {
                throw new FlowableException("deployment '" + deploymentId + "' didn't put an app resource in the cache");
            }
        }

        return appResourceObject;
    }

    public AppModel getAppResourceModel(String deploymentId) {
        Object appResourceObject = getAppResourceObject(deploymentId);
        if (!(appResourceObject instanceof AppModel)) {
            throw new FlowableException("App resource is not of type AppModel");
        }

        return (AppModel) appResourceObject;
    }

    public void removeDeployment(String deploymentId, boolean cascade) {

        DeploymentEntity deployment = deploymentEntityManager.findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.", DeploymentEntity.class);
        }

        if (Flowable5Util.isFlowable5Deployment(deployment, processEngineConfiguration)) {
            processEngineConfiguration.getFlowable5CompatibilityHandler().deleteDeployment(deploymentId, cascade);
            return;
        }

        // Remove any process definition from the cache
        List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl().deploymentId(deploymentId).list();
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher();

        for (ProcessDefinition processDefinition : processDefinitions) {

            // Since all process definitions are deleted by a single query, we should dispatch the events in this loop
            if (eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, processDefinition));
            }
        }

        // Delete data
        deploymentEntityManager.deleteDeployment(deploymentId, cascade);

        // Since we use a delete by query, delete-events are not automatically dispatched
        if (eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, deployment));
        }

        for (ProcessDefinition processDefinition : processDefinitions) {
            processDefinitionCache.remove(processDefinition.getId());
            processDefinitionInfoCache.remove(processDefinition.getId());
        }

        appResourceCache.remove(deploymentId);
        knowledgeBaseCache.remove(deploymentId);
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public List<EngineDeployer> getDeployers() {
        return deployers;
    }

    public void setDeployers(List<EngineDeployer> deployers) {
        this.deployers = deployers;
    }

    public DeploymentCache<ProcessDefinitionCacheEntry> getProcessDefinitionCache() {
        return processDefinitionCache;
    }

    public void setProcessDefinitionCache(DeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache) {
        this.processDefinitionCache = processDefinitionCache;
    }

    public ProcessDefinitionInfoCache getProcessDefinitionInfoCache() {
        return processDefinitionInfoCache;
    }

    public void setProcessDefinitionInfoCache(ProcessDefinitionInfoCache processDefinitionInfoCache) {
        this.processDefinitionInfoCache = processDefinitionInfoCache;
    }

    public DeploymentCache<Object> getKnowledgeBaseCache() {
        return knowledgeBaseCache;
    }

    public void setKnowledgeBaseCache(DeploymentCache<Object> knowledgeBaseCache) {
        this.knowledgeBaseCache = knowledgeBaseCache;
    }

    public DeploymentCache<Object> getAppResourceCache() {
        return appResourceCache;
    }

    public void setAppResourceCache(DeploymentCache<Object> appResourceCache) {
        this.appResourceCache = appResourceCache;
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
        return processDefinitionEntityManager;
    }

    public void setProcessDefinitionEntityManager(ProcessDefinitionEntityManager processDefinitionEntityManager) {
        this.processDefinitionEntityManager = processDefinitionEntityManager;
    }

    public DeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public void setDeploymentEntityManager(DeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
    }

}

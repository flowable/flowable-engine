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
package org.flowable.engine.impl.bpmn.deployer;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionInfoCacheObject;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Updates caches and artifacts for a deployment, its process definitions, and its process definition infos.
 */
public class CachingAndArtifactsManager {

    /**
     * Ensures that the process definition is cached in the appropriate places, including the deployment's collection of deployed artifacts and the deployment manager's cache, as well as caching any
     * ProcessDefinitionInfos.
     */
    public void updateCachingAndArtifacts(ParsedDeployment parsedDeployment) {
        CommandContext commandContext = Context.getCommandContext();
        final ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        DeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache = processEngineConfiguration.getDeploymentManager().getProcessDefinitionCache();
        DeploymentEntity deployment = parsedDeployment.getDeployment();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            BpmnModel bpmnModel = parsedDeployment.getBpmnModelForProcessDefinition(processDefinition);
            Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
            ProcessDefinitionCacheEntry cacheEntry = new ProcessDefinitionCacheEntry(processDefinition, bpmnModel, process);
            processDefinitionCache.add(processDefinition.getId(), cacheEntry);
            addDefinitionInfoToCache(processDefinition, processEngineConfiguration, commandContext);

            // Add to deployment for further usage
            deployment.addDeployedArtifact(processDefinition);
        }
    }
    
    /**
     * Ensures that the process definition is cached in the appropriate places.
     */
    public void updateProcessDefinitionCache(ParsedDeployment parsedDeployment) {
        final ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        DeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache = processEngineConfiguration.getDeploymentManager().getProcessDefinitionCache();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            BpmnModel bpmnModel = parsedDeployment.getBpmnModelForProcessDefinition(processDefinition);
            Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
            ProcessDefinitionCacheEntry cacheEntry = new ProcessDefinitionCacheEntry(processDefinition, bpmnModel, process);
            processDefinitionCache.add(processDefinition.getId(), cacheEntry);
        }
    }

    protected void addDefinitionInfoToCache(ProcessDefinitionEntity processDefinition,
            ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext) {

        if (!processEngineConfiguration.isEnableProcessDefinitionInfoCache()) {
            return;
        }

        DeploymentManager deploymentManager = processEngineConfiguration.getDeploymentManager();
        ProcessDefinitionInfoEntityManager definitionInfoEntityManager = CommandContextUtil.getProcessDefinitionInfoEntityManager(commandContext);
        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();
        ProcessDefinitionInfoEntity definitionInfoEntity = definitionInfoEntityManager.findProcessDefinitionInfoByProcessDefinitionId(processDefinition.getId());

        ObjectNode infoNode = null;
        if (definitionInfoEntity != null && definitionInfoEntity.getInfoJsonId() != null) {
            byte[] infoBytes = definitionInfoEntityManager.findInfoJsonById(definitionInfoEntity.getInfoJsonId());
            if (infoBytes != null) {
                try {
                    infoNode = (ObjectNode) objectMapper.readTree(infoBytes);
                } catch (Exception e) {
                    throw new FlowableException("Error deserializing json info for process definition " + processDefinition.getId());
                }
            }
        }

        ProcessDefinitionInfoCacheObject definitionCacheObject = new ProcessDefinitionInfoCacheObject();
        if (definitionInfoEntity == null) {
            definitionCacheObject.setRevision(0);
        } else {
            definitionCacheObject.setId(definitionInfoEntity.getId());
            definitionCacheObject.setRevision(definitionInfoEntity.getRevision());
        }

        if (infoNode == null) {
            infoNode = objectMapper.createObjectNode();
        }
        definitionCacheObject.setInfoNode(infoNode);

        deploymentManager.getProcessDefinitionInfoCache().add(processDefinition.getId(), definitionCacheObject);
    }
}

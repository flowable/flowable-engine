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
package org.flowable.engine.impl.util;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * A utility class that hides the complexity of {@link ProcessDefinitionEntity} and {@link Process} lookup. Use this class rather than accessing the process definition cache or
 * {@link DeploymentManager} directly.
 * 
 * @author Joram Barrez
 */
public class ProcessDefinitionUtil {

    public static ProcessDefinition getProcessDefinition(String processDefinitionId) {
        return getProcessDefinition(processDefinitionId, false);
    }

    public static ProcessDefinition getProcessDefinition(String processDefinitionId, boolean checkCacheOnly) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (checkCacheOnly) {
            ProcessDefinitionCacheEntry cacheEntry = processEngineConfiguration.getProcessDefinitionCache().get(processDefinitionId);
            if (cacheEntry != null) {
                return cacheEntry.getProcessDefinition();
            }
            return null;

        } else {
            // This will check the cache in the findDeployedProcessDefinitionById method
            return processEngineConfiguration.getDeploymentManager().findDeployedProcessDefinitionById(processDefinitionId);
        }
    }

    public static Process getProcess(String processDefinitionId) {
        if (Context.getCommandContext() == null) {
            throw new FlowableException("Cannot get process model: no current command context is active");
            
        } else if (CommandContextUtil.getProcessEngineConfiguration() == null) {
            return Flowable5Util.getFlowable5CompatibilityHandler().getProcessDefinitionProcessObject(processDefinitionId);

        } else {
            DeploymentManager deploymentManager = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager();

            // This will check the cache in the findDeployedProcessDefinitionById and resolveProcessDefinition method
            ProcessDefinition processDefinitionEntity = deploymentManager.findDeployedProcessDefinitionById(processDefinitionId);
            return deploymentManager.resolveProcessDefinition(processDefinitionEntity).getProcess();
        }
    }

    public static BpmnModel getBpmnModel(String processDefinitionId) {
        if (CommandContextUtil.getProcessEngineConfiguration() == null) {
            return Flowable5Util.getFlowable5CompatibilityHandler().getProcessDefinitionBpmnModel(processDefinitionId);

        } else {
            DeploymentManager deploymentManager = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager();

            // This will check the cache in the findDeployedProcessDefinitionById and resolveProcessDefinition method
            ProcessDefinition processDefinitionEntity = deploymentManager.findDeployedProcessDefinitionById(processDefinitionId);
            return deploymentManager.resolveProcessDefinition(processDefinitionEntity).getBpmnModel();
        }
    }

    public static BpmnModel getBpmnModelFromCache(String processDefinitionId) {
        ProcessDefinitionCacheEntry cacheEntry = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionCache().get(processDefinitionId);
        if (cacheEntry != null) {
            return cacheEntry.getBpmnModel();
        }
        return null;
    }

    public static boolean isProcessDefinitionSuspended(String processDefinitionId) {
        ProcessDefinitionEntity processDefinition = getProcessDefinitionFromDatabase(processDefinitionId);
        return processDefinition.isSuspended();
    }

    public static ProcessDefinitionEntity getProcessDefinitionFromDatabase(String processDefinitionId) {
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionEntityManager();
        ProcessDefinitionEntity processDefinition = processDefinitionEntityManager.findById(processDefinitionId);
        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("No process definition found with id " + processDefinitionId);
        }

        return processDefinition;
    }
}

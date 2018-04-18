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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Joram Barrez
 */
public class ChangeDeploymentTenantIdCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String deploymentId;
    protected String newTenantId;

    public ChangeDeploymentTenantIdCmd(String deploymentId, String newTenantId) {
        this.deploymentId = deploymentId;
        this.newTenantId = newTenantId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("deploymentId is null");
        }

        // Update all entities

        DeploymentEntity deployment = CommandContextUtil.getDeploymentEntityManager(commandContext).findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find deployment with id " + deploymentId, Deployment.class);
        }

        if (Flowable5Util.isFlowable5Deployment(deployment, commandContext)) {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getFlowable5CompatibilityHandler().changeDeploymentTenantId(deploymentId, newTenantId);
            return null;
        }

        String oldTenantId = deployment.getTenantId();
        deployment.setTenantId(newTenantId);

        // Doing process instances, executions and tasks with direct SQL updates
        // (otherwise would not be performant)
        CommandContextUtil.getProcessDefinitionEntityManager(commandContext).updateProcessDefinitionTenantIdForDeployment(deploymentId, newTenantId);
        CommandContextUtil.getExecutionEntityManager(commandContext).updateExecutionTenantIdForDeployment(deploymentId, newTenantId);
        CommandContextUtil.getTaskService().updateTaskTenantIdForDeployment(deploymentId, newTenantId);
        CommandContextUtil.getJobService().updateAllJobTypesTenantIdForDeployment(deploymentId, newTenantId);
        CommandContextUtil.getEventSubscriptionEntityManager(commandContext).updateEventSubscriptionTenantId(oldTenantId, newTenantId);

        // Doing process definitions in memory, cause we need to clear the process definition cache
        List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl().deploymentId(deploymentId).list();
        for (ProcessDefinition processDefinition : processDefinitions) {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessDefinitionCache().remove(processDefinition.getId());
        }

        // Clear process definition cache
        CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessDefinitionCache().clear();

        return null;

    }

}

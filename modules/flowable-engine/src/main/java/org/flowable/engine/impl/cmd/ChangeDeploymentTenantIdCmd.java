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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.repository.AddAsNewDeploymentMergeStrategy;
import org.flowable.engine.impl.repository.AddAsOldDeploymentMergeStrategy;
import org.flowable.engine.impl.repository.MergeByDateDeploymentMergeStrategy;
import org.flowable.engine.impl.repository.VerifyDeploymentMergeStrategy;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentMergeStrategy;
import org.flowable.engine.repository.MergeMode;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.service.JobService;

/**
 * @author Joram Barrez
 */
public class ChangeDeploymentTenantIdCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String deploymentId;
    protected String newTenantId;
    protected DeploymentMergeStrategy deploymentMergeStrategy;

    public ChangeDeploymentTenantIdCmd(String deploymentId, String newTenantId) {
        this(deploymentId, newTenantId, MergeMode.VERIFY);
    }

    public ChangeDeploymentTenantIdCmd(String deploymentId, String newTenantId, MergeMode mergeMode) {
        this.deploymentId = deploymentId;
        this.newTenantId = newTenantId;
        switch (mergeMode) {
            case VERIFY:
                deploymentMergeStrategy = new VerifyDeploymentMergeStrategy();
                break;
            case AS_NEW:
                deploymentMergeStrategy = new AddAsNewDeploymentMergeStrategy();
                break;
            case AS_OLD:
                deploymentMergeStrategy = new AddAsOldDeploymentMergeStrategy();
                break;
            case BY_DATE:
                deploymentMergeStrategy = new MergeByDateDeploymentMergeStrategy();
                break;
            default:
                throw new FlowableException("Merge mode '" + mergeMode + "' not found.");
        }
    }

    public ChangeDeploymentTenantIdCmd(String deploymentId, String newTenantId, DeploymentMergeStrategy deploymentMergeStrategy) {
        this.deploymentId = deploymentId;
        this.newTenantId = newTenantId;
        this.deploymentMergeStrategy = deploymentMergeStrategy;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("deploymentId is null");
        }

        // Update all entities
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        DeploymentEntity deployment = processEngineConfiguration.getDeploymentEntityManager().findById(deploymentId);
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find deployment with id " + deploymentId, Deployment.class);
        }

        if (Flowable5Util.isFlowable5Deployment(deployment, commandContext)) {
            processEngineConfiguration.getFlowable5CompatibilityHandler().changeDeploymentTenantId(deploymentId, newTenantId);
            return null;
        }

        deploymentMergeStrategy.prepareMerge(commandContext, deploymentId, newTenantId);
        String oldTenantId = deployment.getTenantId();
        deployment.setTenantId(newTenantId);

        // Doing process instances, executions and tasks with direct SQL updates
        // (otherwise would not be performant)
        processEngineConfiguration.getProcessDefinitionEntityManager().updateProcessDefinitionTenantIdForDeployment(deploymentId, newTenantId);
        processEngineConfiguration.getExecutionEntityManager().updateExecutionTenantIdForDeployment(deploymentId, newTenantId);
        processEngineConfiguration.getTaskServiceConfiguration().getTaskService().updateTaskTenantIdForDeployment(deploymentId, newTenantId);
        JobService jobService = processEngineConfiguration.getJobServiceConfiguration().getJobService();
        jobService.updateAllJobTypesTenantIdForDeployment(deploymentId, newTenantId);
        processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService().updateEventSubscriptionTenantId(oldTenantId, newTenantId);

        deploymentMergeStrategy.finalizeMerge(commandContext, deploymentId, newTenantId);

        // Doing process definitions in memory, cause we need to clear the process definition cache
        List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl().deploymentId(deploymentId).list();
        for (ProcessDefinition processDefinition : processDefinitions) {
            processEngineConfiguration.getProcessDefinitionCache().remove(processDefinition.getId());
        }

        // Clear process definition cache
        processEngineConfiguration.getProcessDefinitionCache().clear();

        return null;

    }

}

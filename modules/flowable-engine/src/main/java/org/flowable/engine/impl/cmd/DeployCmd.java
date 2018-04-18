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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.DeploymentQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.repository.DeploymentBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentProperties;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DeployCmd<T> implements Command<Deployment>, Serializable {

    private static final long serialVersionUID = 1L;
    protected DeploymentBuilderImpl deploymentBuilder;

    public DeployCmd(DeploymentBuilderImpl deploymentBuilder) {
        this.deploymentBuilder = deploymentBuilder;
    }

    @Override
    public Deployment execute(CommandContext commandContext) {

        // Backwards compatibility with v5
        if (deploymentBuilder.getDeploymentProperties() != null
                && deploymentBuilder.getDeploymentProperties().containsKey(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION)
                && deploymentBuilder.getDeploymentProperties().get(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION).equals(Boolean.TRUE)) {

            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            if (processEngineConfiguration.isFlowable5CompatibilityEnabled() && processEngineConfiguration.getFlowable5CompatibilityHandler() != null) {
                return deployAsFlowable5ProcessDefinition(commandContext);
            } else {
                throw new FlowableException("Can't deploy a v5 deployment with no flowable 5 compatibility enabled or no compatibility handler on the classpath");
            }
        }

        return executeDeploy(commandContext);
    }

    protected Deployment executeDeploy(CommandContext commandContext) {
        DeploymentEntity deployment = deploymentBuilder.getDeployment();

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        deployment.setDeploymentTime(processEngineConfiguration.getClock().getCurrentTime());

        if (deploymentBuilder.isDuplicateFilterEnabled()) {

            List<Deployment> existingDeployments = new ArrayList<>();
            if (deployment.getTenantId() == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(deployment.getTenantId())) {
                List<Deployment> deploymentEntities = new DeploymentQueryImpl(processEngineConfiguration.getCommandExecutor())
                        .deploymentName(deployment.getName())
                        .orderByDeploymenTime().desc()
                        .listPage(0, 1);
                if (!deploymentEntities.isEmpty()) {
                    existingDeployments.add(deploymentEntities.get(0));
                }
                
            } else {
                List<Deployment> deploymentList = processEngineConfiguration.getRepositoryService().createDeploymentQuery().deploymentName(deployment.getName())
                        .deploymentTenantId(deployment.getTenantId()).orderByDeploymentId().desc().list();

                if (!deploymentList.isEmpty()) {
                    existingDeployments.addAll(deploymentList);
                }
            }

            DeploymentEntity existingDeployment = null;
            if (!existingDeployments.isEmpty()) {
                existingDeployment = (DeploymentEntity) existingDeployments.get(0);
            }

            if (existingDeployment != null && !deploymentsDiffer(deployment, existingDeployment)) {
                return existingDeployment;
            }
        }

        deployment.setNew(true);

        // Save the data
        CommandContextUtil.getDeploymentEntityManager(commandContext).insert(deployment);

        if (processEngineConfiguration.getEventDispatcher().isEnabled()) {
            processEngineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, deployment));
        }

        // Deployment settings
        Map<String, Object> deploymentSettings = new HashMap<>();
        deploymentSettings.put(DeploymentSettings.IS_BPMN20_XSD_VALIDATION_ENABLED, deploymentBuilder.isBpmn20XsdValidationEnabled());
        deploymentSettings.put(DeploymentSettings.IS_PROCESS_VALIDATION_ENABLED, deploymentBuilder.isProcessValidationEnabled());

        // Actually deploy
        processEngineConfiguration.getDeploymentManager().deploy(deployment, deploymentSettings);

        if (deploymentBuilder.getProcessDefinitionsActivationDate() != null) {
            scheduleProcessDefinitionActivation(commandContext, deployment);
        }

        if (processEngineConfiguration.getEventDispatcher().isEnabled()) {
            processEngineConfiguration.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, deployment));
        }

        return deployment;
    }

    protected Deployment deployAsFlowable5ProcessDefinition(CommandContext commandContext) {
        Flowable5CompatibilityHandler flowable5CompatibilityHandler = CommandContextUtil.getProcessEngineConfiguration(commandContext).getFlowable5CompatibilityHandler();
        if (flowable5CompatibilityHandler == null) {
            throw new FlowableException("Found Flowable 5 process definition, but no compatibility handler on the classpath. "
                    + "Cannot use the deployment property " + DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION);
        }
        return flowable5CompatibilityHandler.deploy(deploymentBuilder);
    }

    protected boolean deploymentsDiffer(DeploymentEntity deployment, DeploymentEntity saved) {

        if (deployment.getResources() == null || saved.getResources() == null) {
            return true;
        }

        Map<String, EngineResource> resources = deployment.getResources();
        Map<String, EngineResource> savedResources = saved.getResources();

        for (String resourceName : resources.keySet()) {
            EngineResource savedResource = savedResources.get(resourceName);

            if (savedResource == null)
                return true;

            if (!savedResource.isGenerated()) {
                EngineResource resource = resources.get(resourceName);

                byte[] bytes = resource.getBytes();
                byte[] savedBytes = savedResource.getBytes();
                if (!Arrays.equals(bytes, savedBytes)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void scheduleProcessDefinitionActivation(CommandContext commandContext, DeploymentEntity deployment) {
        for (ProcessDefinitionEntity processDefinitionEntity : deployment.getDeployedArtifacts(ProcessDefinitionEntity.class)) {

            // If activation date is set, we first suspend all the process
            // definition
            SuspendProcessDefinitionCmd suspendProcessDefinitionCmd = new SuspendProcessDefinitionCmd(processDefinitionEntity, false, null, deployment.getTenantId());
            suspendProcessDefinitionCmd.execute(commandContext);

            // And we schedule an activation at the provided date
            ActivateProcessDefinitionCmd activateProcessDefinitionCmd = new ActivateProcessDefinitionCmd(processDefinitionEntity, false, deploymentBuilder.getProcessDefinitionsActivationDate(),
                    deployment.getTenantId());
            activateProcessDefinitionCmd.execute(commandContext);
        }
    }

}

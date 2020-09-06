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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.Deployment;

/**
 * @author Tijs Rademakers
 */
public class SetDeploymentCategoryCmd implements Command<Void> {

    protected String deploymentId;
    protected String category;

    public SetDeploymentCategoryCmd(String deploymentId, String category) {
        this.deploymentId = deploymentId;
        this.category = category;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("Deployment id is null");
        }

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        DeploymentEntity deployment = processEngineConfiguration.getDeploymentEntityManager().findById(deploymentId);

        if (deployment == null) {
            throw new FlowableObjectNotFoundException("No deployment found for id = '" + deploymentId + "'", Deployment.class);
        }

        if (Flowable5Util.isFlowable5Deployment(deployment, commandContext)) {
            processEngineConfiguration.getFlowable5CompatibilityHandler().setDeploymentCategory(deploymentId, category);
        }

        // Update category
        deployment.setCategory(category);

        FlowableEventDispatcher eventDispatcher = processEngineConfiguration.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, deployment),
                    processEngineConfiguration.getEngineCfgKey());
        }

        return null;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}

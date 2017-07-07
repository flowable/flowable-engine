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

import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.flowable.engine.impl.runtime.ProcessInstanceBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class StartProcessInstanceByMessageCmd implements Command<ProcessInstance> {

    protected String messageName;
    protected String businessKey;
    protected Map<String, Object> processVariables;
    protected Map<String, Object> transientVariables;
    protected String tenantId;

    public StartProcessInstanceByMessageCmd(String messageName, String businessKey, Map<String, Object> processVariables, String tenantId) {
        this.messageName = messageName;
        this.businessKey = businessKey;
        this.processVariables = processVariables;
        this.tenantId = tenantId;
    }

    public StartProcessInstanceByMessageCmd(ProcessInstanceBuilderImpl processInstanceBuilder) {
        this.messageName = processInstanceBuilder.getMessageName();
        this.businessKey = processInstanceBuilder.getBusinessKey();
        this.processVariables = processInstanceBuilder.getVariables();
        this.transientVariables = processInstanceBuilder.getTransientVariables();
        this.tenantId = processInstanceBuilder.getTenantId();
    }

    public ProcessInstance execute(CommandContext commandContext) {

        if (messageName == null) {
            throw new FlowableIllegalArgumentException("Cannot start process instance by message: message name is null");
        }

        MessageEventSubscriptionEntity messageEventSubscription = CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findMessageStartEventSubscriptionByName(messageName, tenantId);

        if (messageEventSubscription == null) {
            throw new FlowableObjectNotFoundException("Cannot start process instance by message: no subscription to message with name '" + messageName + "' found.", MessageEventSubscriptionEntity.class);
        }

        String processDefinitionId = messageEventSubscription.getConfiguration();
        if (processDefinitionId == null) {
            throw new FlowableException("Cannot start process instance by message: subscription to message with name '" + messageName + "' is not a message start event.");
        }

        DeploymentManager deploymentCache = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager();

        ProcessDefinition processDefinition = deploymentCache.findDeployedProcessDefinitionById(processDefinitionId);
        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("No process definition found for id '" + processDefinitionId + "'", ProcessDefinition.class);
        }

        ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
        ProcessInstance processInstance = processInstanceHelper.createAndStartProcessInstanceByMessage(
                processDefinition, messageName, businessKey, processVariables, transientVariables);

        return processInstance;
    }

}

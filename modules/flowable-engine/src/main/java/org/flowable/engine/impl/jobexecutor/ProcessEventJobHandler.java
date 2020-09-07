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

package org.flowable.engine.impl.jobexecutor;

import java.util.List;
import java.util.Optional;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EventSubscriptionUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class ProcessEventJobHandler implements JobHandler {

    public static final String TYPE = "event";
    public static final String PAYLOAD_VARIABLE_NAME = "payload";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {

        EventSubscriptionService eventSubscriptionService = CommandContextUtil.getEventSubscriptionService(commandContext);

        // lookup subscription:
        EventSubscriptionEntity eventSubscriptionEntity = eventSubscriptionService.findById(configuration);

        // if event subscription is null, ignore
        if (eventSubscriptionEntity != null) {
            VariableInstanceEntity payloadVariable = getPayloadVariable(job);
            Object payloadValue = null;
            if (payloadVariable != null) {
                payloadValue = payloadVariable.getValue();
                removeVariable(payloadVariable);
            }
            EventSubscriptionUtil.eventReceived(eventSubscriptionEntity, payloadValue, false);
        }
    }

    protected void removeVariable(VariableInstanceEntity variable) {
        CommandContextUtil.getVariableService().deleteVariableInstance(variable);
    }

    protected VariableInstanceEntity getPayloadVariable(JobEntity job) {
        List<VariableInstanceEntity> variableInstanceByScopeIdAndScopeType = CommandContextUtil.getVariableService()
                .findVariableInstanceByScopeIdAndScopeType(job.getId(), ScopeTypes.JOB);
        Optional<VariableInstanceEntity> payload = variableInstanceByScopeIdAndScopeType.stream()
                .filter(variableInstance -> PAYLOAD_VARIABLE_NAME.equals(variableInstance.getName())).findAny();
        return payload.orElse(null);
    }
}

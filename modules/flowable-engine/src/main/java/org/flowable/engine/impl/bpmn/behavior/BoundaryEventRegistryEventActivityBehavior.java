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
package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CorrelationUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Tijs Rademakers
 */
public class BoundaryEventRegistryEventActivityBehavior extends BoundaryEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected String eventDefinitionKey;

    public BoundaryEventRegistryEventActivityBehavior(String eventDefinitionKey, boolean interrupting) {
        super(interrupting);
        this.eventDefinitionKey = eventDefinitionKey;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        EventDefinition eventDefinition = getEventDefinition(commandContext, executionEntity);
        
        EventSubscriptionEntity eventSubscription = (EventSubscriptionEntity) CommandContextUtil.getEventSubscriptionService(commandContext).createEventSubscriptionBuilder()
                        .eventType(eventDefinition.getKey())
                        .executionId(executionEntity.getId())
                        .processInstanceId(executionEntity.getProcessInstanceId())
                        .activityId(executionEntity.getCurrentActivityId())
                        .processDefinitionId(executionEntity.getProcessDefinitionId())
                        .scopeType(ScopeTypes.BPMN)
                        .tenantId(executionEntity.getTenantId())
                        .configuration(CorrelationUtil.getCorrelationKey(commandContext, executionEntity))
                        .create();
        
        CountingEntityUtil.handleInsertEventSubscriptionEntityCount(eventSubscription);
        executionEntity.getEventSubscriptions().add(eventSubscription);
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();

        if (boundaryEvent.isCancelActivity()) {
            EventSubscriptionService eventSubscriptionService = CommandContextUtil.getEventSubscriptionService();
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();
            
            CommandContext commandContext = Context.getCommandContext();
            EventDefinition eventDefinition = getEventDefinition(commandContext, executionEntity);
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (Objects.equals(eventDefinition.getKey(), eventSubscription.getEventType())) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                }
            }
        }

        super.trigger(executionEntity, triggerName, triggerData);
    }
    
    protected EventDefinition getEventDefinition(CommandContext commandContext, ExecutionEntity executionEntity) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        String key = null;
        if (StringUtils.isNotEmpty(eventDefinitionKey)) {
            Expression expression = processEngineConfiguration.getExpressionManager().createExpression(eventDefinitionKey);
            key = expression.getValue(executionEntity).toString();
        }

        EventDefinition eventDefinition = processEngineConfiguration.getEventRegistry().getEventDefinition(key);
        if (eventDefinition == null) {
            throw new FlowableException("Could not find event definition for key " +key);
        }
        return eventDefinition;
    }
}
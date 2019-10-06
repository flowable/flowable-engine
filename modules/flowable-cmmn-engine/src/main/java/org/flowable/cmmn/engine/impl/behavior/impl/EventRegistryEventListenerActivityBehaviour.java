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
package org.flowable.cmmn.engine.impl.behavior.impl;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Joram Barrez
 */
public class EventRegistryEventListenerActivityBehaviour extends CoreCmmnTriggerableActivityBehavior implements PlanItemActivityBehavior {

    protected String eventDefinitionKey;

    public EventRegistryEventListenerActivityBehaviour(String eventDefinitionKey) {
        this.eventDefinitionKey = eventDefinitionKey;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {

    }

    protected EventDefinition getEventDefinition(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);

        String key = null;
        if (StringUtils.isNotEmpty(eventDefinitionKey)) {
            Expression expression = cmmnEngineConfiguration.getExpressionManager().createExpression(eventDefinitionKey);
            key = expression.getValue(planItemInstanceEntity).toString();
        }

        EventDefinition eventDefinition = cmmnEngineConfiguration.getEventRegistry().getEventDefinition(key);
        if (eventDefinition == null) {
            throw new FlowableException("Could not find event definition for key " +key);
        }
        return eventDefinition;
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {

        EventSubscriptionService eventSubscriptionService = CommandContextUtil.getEventSubscriptionService(commandContext);
        EventDefinition eventDefinition = getEventDefinition(commandContext, planItemInstanceEntity);

        List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsBySubScopeId(planItemInstanceEntity.getId());
        for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
            if (Objects.equals(eventDefinition.getKey(), eventSubscription.getEventType())) {
                eventSubscriptionService.deleteEventSubscription(eventSubscription);
            }
        }
        
        CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
    }


    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemTransition.TERMINATE.equals(transition)
            || PlanItemTransition.EXIT.equals(transition)
            || PlanItemTransition.DISMISS.equals(transition)) {

            EventSubscriptionService eventSubscriptionService = CommandContextUtil.getEventSubscriptionService(commandContext);
            List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsBySubScopeId(planItemInstance.getId());
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                eventSubscriptionService.deleteEventSubscription(eventSubscription);
            }

        } else if (PlanItemTransition.CREATE.equals(transition)) {
            createEventSubscription(commandContext, (PlanItemInstanceEntity) planItemInstance);

        }
    }

    protected void createEventSubscription(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        EventDefinition eventDefinition = getEventDefinition(commandContext, planItemInstanceEntity);

        CommandContextUtil.getEventSubscriptionService(commandContext).createEventSubscriptionBuilder()
            .eventType(eventDefinition.getKey())
            .subScopeId(planItemInstanceEntity.getId())
            .scopeId(planItemInstanceEntity.getCaseInstanceId())
            .scopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId())
            .scopeType(ScopeTypes.CMMN)
            .tenantId(planItemInstanceEntity.getTenantId())
            .create();
    }

}

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EventInstanceCmmnUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceUtil;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.constant.EventConstants;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class EventRegistryEventListenerActivityBehaviour extends CoreCmmnTriggerableActivityBehavior implements PlanItemActivityBehavior {

    protected Expression eventDefinitionKeyExpression;

    public EventRegistryEventListenerActivityBehaviour(Expression eventDefinitionKeyExpression) {
        this.eventDefinitionKeyExpression = eventDefinitionKeyExpression;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {

    }

    protected String resolveEventDefinitionKey(PlanItemInstanceEntity planItemInstanceEntity) {
        Object key = null;

        if (eventDefinitionKeyExpression != null) {
            key = eventDefinitionKeyExpression.getValue(planItemInstanceEntity);
        }

        if (key == null) {
            throw new FlowableException("Could not resolve key from expression: " + eventDefinitionKeyExpression);
        }

        return key.toString();
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        EventInstance eventInstance = (EventInstance) planItemInstanceEntity.getTransientVariable(EventConstants.EVENT_INSTANCE);
        if (eventInstance != null) {
            handleEventInstance(planItemInstanceEntity, eventInstance);
        }
        
        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(planItemInstanceEntity);
        if (repetitionRule != null) {
            PlanItemInstanceEntity eventPlanItemInstanceEntity = PlanItemInstanceUtil.copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, false, false);
            eventPlanItemInstanceEntity.setState(PlanItemInstanceState.AVAILABLE);
            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            agenda.planCreatePlanItemInstanceWithoutEvaluationOperation(eventPlanItemInstanceEntity);
            agenda.planOccurPlanItemInstanceOperation(eventPlanItemInstanceEntity);
            
            CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper().executeLifecycleListeners(
                    commandContext, planItemInstanceEntity, null, PlanItemInstanceState.AVAILABLE);
            
        } else {
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            EventSubscriptionService eventSubscriptionService = cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            String eventDefinitionKey = resolveEventDefinitionKey(planItemInstanceEntity);

            List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsBySubScopeId(planItemInstanceEntity.getId());
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (Objects.equals(eventDefinitionKey, eventSubscription.getEventType())) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                }
            }
            
            CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
        }
    }

    protected void handleEventInstance(PlanItemInstanceEntity planItemInstanceEntity, EventInstance eventInstance) {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
        if (planItemDefinition != null) {
            EventInstanceCmmnUtil.handleEventInstanceOutParameters(planItemInstanceEntity, planItemDefinition, eventInstance);
        }
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemTransition.TERMINATE.equals(transition)
                || PlanItemTransition.EXIT.equals(transition)
                || PlanItemTransition.DISMISS.equals(transition)) {

            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            EventSubscriptionService eventSubscriptionService = cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsBySubScopeId(planItemInstance.getId());
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                eventSubscriptionService.deleteEventSubscription(eventSubscription);
            }

        } else if (PlanItemTransition.CREATE.equals(transition)) {
            createEventSubscription(commandContext, (PlanItemInstanceEntity) planItemInstance);

        }
    }

    protected void createEventSubscription(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        String eventDefinitionKey = resolveEventDefinitionKey(planItemInstanceEntity);

        String correlationKey = getCorrelationKey(commandContext, planItemInstanceEntity);

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService().createEventSubscriptionBuilder()
            .eventType(eventDefinitionKey)
            .subScopeId(planItemInstanceEntity.getId())
            .scopeId(planItemInstanceEntity.getCaseInstanceId())
            .scopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId())
            .scopeType(ScopeTypes.CMMN)
            .tenantId(planItemInstanceEntity.getTenantId())
            .configuration(correlationKey)
            .create();
    }

    protected String getCorrelationKey(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        String correlationKey;
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItemDefinition();
        if (planItemDefinition != null) {
            List<ExtensionElement> eventCorrelations = planItemDefinition.getExtensionElements()
                .getOrDefault(CmmnXmlConstants.ELEMENT_EVENT_CORRELATION_PARAMETER, Collections.emptyList());
            if (!eventCorrelations.isEmpty()) {
                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                ExpressionManager expressionManager = cmmnEngineConfiguration.getExpressionManager();

                Map<String, Object> correlationParameters = new HashMap<>();
                for (ExtensionElement eventCorrelation : eventCorrelations) {
                    String name = eventCorrelation.getAttributeValue(null, "name");
                    String valueExpression = eventCorrelation.getAttributeValue(null, "value");
                    if (StringUtils.isNotEmpty(valueExpression)) {
                        Object value = expressionManager.createExpression(valueExpression).getValue(planItemInstanceEntity);
                        correlationParameters.put(name, value);
                    } else {
                        correlationParameters.put(name, null);
                    }
                }

                correlationKey = CommandContextUtil.getEventRegistry().generateKey(correlationParameters);
            } else {
                correlationKey = null;
            }
        } else {
            correlationKey = null;
        }
        return correlationKey;
    }

}

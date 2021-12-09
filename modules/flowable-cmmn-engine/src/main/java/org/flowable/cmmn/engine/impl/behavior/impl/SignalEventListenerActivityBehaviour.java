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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.SignalEventListener;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;

/**
 * {@link CmmnActivityBehavior} implementation for the CMMN extension Signal Event Listener.
 */
public class SignalEventListenerActivityBehaviour extends CoreCmmnTriggerableActivityBehavior implements PlanItemActivityBehavior {

    protected String signalRef;

    public SignalEventListenerActivityBehaviour(SignalEventListener signalEventListener) {
        this.signalRef = signalEventListener.getSignalRef();
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {

        // Remove event subscription when moving to a terminal state
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition) || PlanItemTransition.DISMISS.equals(transition)) {
            EventSubscriptionService eventSubscriptionService = cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
            List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsBySubScopeId(planItemInstance.getId());
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                eventSubscriptionService.deleteEventSubscription(eventSubscription);
            }

        } else if (PlanItemTransition.CREATE.equals(transition)) {
            String signalName = null;
            if (StringUtils.isNotEmpty(signalRef)) {
                Expression signalExpression = CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                    .getExpressionManager().createExpression(signalRef);
                signalName = signalExpression.getValue(planItemInstance).toString();
            }

            cmmnEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService().createEventSubscriptionBuilder()
                .eventType(SignalEventSubscriptionEntity.EVENT_TYPE)
                .eventName(signalName)
                .subScopeId(planItemInstance.getId())
                .scopeId(planItemInstance.getCaseInstanceId())
                .scopeDefinitionId(planItemInstance.getCaseDefinitionId())
                .scopeType(ScopeTypes.CMMN)
                .tenantId(planItemInstance.getTenantId())
                .create();

        }
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        // Nothing to do, logic happens on state transition
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
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

            String signalName = null;
            if (StringUtils.isNotEmpty(signalRef)) {
                Expression signalExpression = CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                                .getExpressionManager().createExpression(signalRef);
                signalName = signalExpression.getValue(planItemInstanceEntity).toString();
            }

            List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findEventSubscriptionsBySubScopeId(planItemInstanceEntity.getId());
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (eventSubscription instanceof SignalEventSubscriptionEntity && eventSubscription.getEventName().equals(signalName)) {
                    eventSubscriptionService.deleteEventSubscription(eventSubscription);
                }
            }
            
            CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
        }
    }

}

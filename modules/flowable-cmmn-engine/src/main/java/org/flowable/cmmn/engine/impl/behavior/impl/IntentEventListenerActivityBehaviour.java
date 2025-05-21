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

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceUtil;
import org.flowable.cmmn.model.IntentEventListener;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * {@link CmmnActivityBehavior} implementation for the CMMN extension Intent Event Listener.
 */
public class IntentEventListenerActivityBehaviour extends CoreCmmnTriggerableActivityBehavior implements PlanItemActivityBehavior {

    protected IntentEventListener intentEventListener;

    public IntentEventListenerActivityBehaviour(IntentEventListener intentEventListener) {
        this.intentEventListener = intentEventListener;
    }

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {
        // No logic is needed
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        // Nothing to do, logic happens on state transition
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(planItemInstanceEntity);
        if (repetitionRule != null && ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstanceEntity, planItemInstanceEntity.getStagePlanItemInstanceEntity())) {
            PlanItemInstanceEntity eventPlanItemInstanceEntity = PlanItemInstanceUtil.copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, false, false);
            CmmnEngineAgenda agenda = CommandContextUtil.getAgenda(commandContext);
            agenda.planCreatePlanItemInstanceWithoutEvaluationOperation(eventPlanItemInstanceEntity);
            agenda.planOccurPlanItemInstanceOperation(eventPlanItemInstanceEntity);
            
            CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper().executeLifecycleListeners(
                    commandContext, planItemInstanceEntity, PlanItemInstanceState.ACTIVE, PlanItemInstanceState.AVAILABLE);
            
        } else {            
            CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
        }
    }

}

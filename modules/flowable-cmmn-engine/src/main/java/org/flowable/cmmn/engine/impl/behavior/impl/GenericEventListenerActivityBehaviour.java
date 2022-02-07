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
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * {@link CmmnActivityBehavior} implementation for the CMMN Event Listener.
 *
 * @author Tijs Rademakers
 */
public class GenericEventListenerActivityBehaviour extends CoreCmmnTriggerableActivityBehavior implements PlanItemActivityBehavior {

    @Override
    public void onStateTransition(CommandContext commandContext, DelegatePlanItemInstance planItemInstance, String transition) {

    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
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
            CommandContextUtil.getAgenda(commandContext).planOccurPlanItemInstanceOperation(planItemInstanceEntity);
        }
    }

    @Override
    public void trigger(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        execute(commandContext, planItemInstanceEntity);
    }

}

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
package org.flowable.cmmn.engine.impl.agenda.operation;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.ManualActivationRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.Task;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class ActivatePlanItemInstanceOperation extends AbstractPlanItemInstanceOperation {

    public ActivatePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public void run() {
        // When it's an asynchronous task, a new activate operation is planned asynchronously.
        if (isAsync() && !PlanItemInstanceState.ASYNC_ACTIVE.equals(planItemInstanceEntity.getState())) {
            CommandContextUtil.getAgenda(commandContext).planActivateAsyncPlanItemInstanceOperation(planItemInstanceEntity);
        } else {
            // Sentries are not needed to be kept around, as the plan item is being enabled
            deleteSentryPartInstances();
            // Evaluate manual activation rule. If one is defined and it evaluates to true, the plan item becomes enabled.
            // Otherwise, the plan item instance is started and becomes active
            boolean isManuallyActivated = evaluateManualActivationRule();
            if (isManuallyActivated) {
                CommandContextUtil.getAgenda(commandContext).planEnablePlanItemInstanceOperation(planItemInstanceEntity);
            } else {
                CommandContextUtil.getAgenda(commandContext).planStartPlanItemInstanceOperation(planItemInstanceEntity);
            }
        }
    }

    protected boolean evaluateManualActivationRule() {
        PlanItemControl planItemControl = planItemInstanceEntity.getPlanItem().getItemControl();
        if (planItemControl != null && planItemControl.getManualActivationRule() != null) {
            ManualActivationRule manualActivationRule = planItemControl.getManualActivationRule();

            if (StringUtils.isNotEmpty(manualActivationRule.getCondition())) {
                return evaluateBooleanExpression(commandContext, planItemInstanceEntity, manualActivationRule.getCondition());
            } else {
                return true; // Having a manual activation rule without condition, defaults to true.
            }
        }
        return false;
    }

    protected boolean isAsync() {
        if (planItemInstanceEntity.getPlanItem().getPlanItemDefinition() instanceof Task) {
            Task task = (Task) planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
            if (task.isAsync()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Activate PlanItem] ");
        if (planItem.getName() != null) {
            stringBuilder.append(planItem.getName());
            stringBuilder.append(" (");
            stringBuilder.append(planItem.getId());
            stringBuilder.append(")");
        } else {
            stringBuilder.append(planItem.getId());
        }
        return stringBuilder.toString();
    }
}

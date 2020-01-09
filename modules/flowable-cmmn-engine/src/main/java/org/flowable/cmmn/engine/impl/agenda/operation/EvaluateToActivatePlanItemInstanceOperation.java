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

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.agenda.PlanItemEvaluationResult;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.interceptor.CommandContext;

public class EvaluateToActivatePlanItemInstanceOperation extends AbstractEvaluationCriteriaOperation {

    protected PlanItemInstanceEntity planItemInstanceEntity;

    public EvaluateToActivatePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity.getCaseInstanceId(), null, null);
        this.planItemInstanceEntity = planItemInstanceEntity;
    }

    @Override
    public void run() {
        PlanItemEvaluationResult evaluationResult = new PlanItemEvaluationResult();
        evaluateForActivation(planItemInstanceEntity, planItemInstanceEntity, evaluationResult);
        if (!evaluationResult.isCriteriaChanged()) {
            // the plan item was not (yet) activated, so set its state to available as there must be a entry sentry with a condition which might be satisfied later
            planItemInstanceEntity.setState(PlanItemInstanceState.AVAILABLE);
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Evaluate To Activate] plan item intance ");
        stringBuilder.append(planItemInstanceEntity.getId());

        if (planItemLifeCycleEvent != null) {
            stringBuilder.append(" with transition '").append(planItemLifeCycleEvent.getTransition()).append("' having fired");
            if (planItemLifeCycleEvent.getPlanItem() != null) {
                stringBuilder.append(" for ").append(planItemLifeCycleEvent.getPlanItem());
            }
        }

        return stringBuilder.toString();
    }
}

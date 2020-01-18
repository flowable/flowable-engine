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

import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Criterion;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class EvaluateCriteriaOperation extends AbstractEvaluationCriteriaOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateCriteriaOperation.class);

    public EvaluateCriteriaOperation(CommandContext commandContext, String caseInstanceEntityId) {
        super(commandContext, caseInstanceEntityId, null, null);
    }

    public EvaluateCriteriaOperation(CommandContext commandContext, String caseInstanceEntityId, PlanItemLifeCycleEvent planItemLifeCycleEvent) {
        super(commandContext, caseInstanceEntityId, null, planItemLifeCycleEvent);
    }

    @Override
    public void run() {
        super.run();

        if (caseInstanceEntity.isDeleted()) {
            markAsNoop();
            return;
        }

        // when evaluating an exit sentry, we take the optional exit event type and exit type into account as well, when terminating the case instance
        Criterion satisfiedExitCriterion = evaluateExitCriteria(caseInstanceEntity, getPlanModel(caseInstanceEntity));
        if (satisfiedExitCriterion != null) {
            // propagate the exit event type and exit type, if provided with the exit sentry / criterion
            CommandContextUtil.getAgenda(commandContext).planTerminateCaseInstanceOperation(caseInstanceEntity.getId(), satisfiedExitCriterion.getId(),
                            satisfiedExitCriterion.getExitType(), satisfiedExitCriterion.getExitEventType());

        } else {
            boolean criteriaChangeOrActiveChildren = evaluatePlanItemsCriteria(caseInstanceEntity);
            if (evaluateStagesAndCaseInstanceCompletion
                    && evaluatePlanModelComplete()
                    && !criteriaChangeOrActiveChildren
                    && !CaseInstanceState.END_STATES.contains(caseInstanceEntity.getState())
                    ) {
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No active plan items found for plan model, completing case instance");
                }
                CommandContextUtil.getAgenda(commandContext).planCompleteCaseInstanceOperation(caseInstanceEntity);

            } else {
                markAsNoop();
            }

        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Evaluate Criteria] case instance ");
        if (caseInstanceEntity != null) {
            stringBuilder.append(caseInstanceEntity.getId());
        } else {
            stringBuilder.append(caseInstanceEntityId);
        }

        if (planItemLifeCycleEvent != null) {
            stringBuilder.append(" with transition '").append(planItemLifeCycleEvent.getTransition()).append("' having fired");
            if (planItemLifeCycleEvent.getPlanItem() != null) {
                stringBuilder.append(" for ").append(planItemLifeCycleEvent.getPlanItem());
            }
        }

        return stringBuilder.toString();
    }
    
}

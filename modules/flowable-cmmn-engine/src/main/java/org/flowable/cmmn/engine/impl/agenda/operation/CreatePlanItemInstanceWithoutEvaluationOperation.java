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

import static org.flowable.cmmn.engine.impl.variable.CmmnAggregation.groupAggregationsByTarget;

import java.util.Map;

import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceUtil;
import org.flowable.cmmn.engine.impl.variable.CmmnAggregation;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CreatePlanItemInstanceWithoutEvaluationOperation extends AbstractPlanItemInstanceOperation {

    public CreatePlanItemInstanceWithoutEvaluationOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public void run() {
        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(planItemInstanceEntity);
        if (repetitionRule != null) {
            //Increase repetition counter, value is kept from the previous instance of the repetition
            //@see CmmOperation.copyAndInsertPlanItemInstance used by @see EvaluateCriteriaOperation and @see AbstractDeletePlanItemInstanceOperation
            //Or if its the first instance of the repetition, this call sets the counter to 1
            int repetitionCounter = PlanItemInstanceUtil.getRepetitionCounter(planItemInstanceEntity);
            if (repetitionCounter == 0 && repetitionRule.getAggregations() != null) {
                // This is the first repetition counter so we need to create the aggregated overview values
                // If there are aggregations we need to create an overview variable for every aggregation
                Map<String, VariableAggregationDefinition> aggregationsByTarget = groupAggregationsByTarget(planItemInstanceEntity,
                        repetitionRule.getAggregations().getOverviewAggregations(), CommandContextUtil.getCmmnEngineConfiguration(commandContext));

                for (String variableName : aggregationsByTarget.keySet()) {
                    CmmnAggregation bpmnAggregation = new CmmnAggregation(planItemInstanceEntity.getId());
                    planItemInstanceEntity.getParentVariableScope().setVariable(variableName, bpmnAggregation);
                }
            }
            setRepetitionCounter(planItemInstanceEntity, repetitionCounter + 1);
        }

        CmmnHistoryManager cmmnHistoryManager = CommandContextUtil.getCmmnHistoryManager(commandContext);
        cmmnHistoryManager.recordPlanItemInstanceCreated(planItemInstanceEntity);

        planItemInstanceEntity.setLastAvailableTime(getCurrentTime(commandContext));
        cmmnHistoryManager.recordPlanItemInstanceAvailable(planItemInstanceEntity);
    }
}

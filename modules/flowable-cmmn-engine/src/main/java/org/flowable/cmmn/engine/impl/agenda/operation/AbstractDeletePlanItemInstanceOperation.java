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
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractDeletePlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    public AbstractDeletePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public void run() {
        super.run();
        
        boolean isRepeating = verifyRepetitionRule();
        if (isRepeating) {
            
            // Create new repeating instance
            PlanItemInstanceEntity newPlanItemInstanceEntity = createAndInsertPlanItemInstance(commandContext, 
                    planItemInstanceEntity.getPlanItem(), 
                    planItemInstanceEntity.getCaseDefinitionId(), 
                    planItemInstanceEntity.getCaseInstanceId(), 
                    planItemInstanceEntity.getStageInstanceId(), 
                    planItemInstanceEntity.getTenantId());
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());
            caseInstanceEntity.getChildPlanItemInstances().add(newPlanItemInstanceEntity);
            
            // Set repetition counter
            int counter = getRepetitionCounter(planItemInstanceEntity);
            setRepetitionCounter(newPlanItemInstanceEntity, ++counter);
            
            // Plan item doesn't have entry criteria and immediately goes to ACTIVE
            CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstance(newPlanItemInstanceEntity);
        }
        
        deleteSentryPartInstances();
        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).delete(planItemInstanceEntity);
    }

    protected boolean verifyRepetitionRule() {
        // If there are not entry criteria and the repetition rule evaluates to true, a new instance needs to be created.
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        if (isEvaluateRepetitionRule() && isPlanItemRepeatableOnComplete(planItem)) {
            String repetitionCondition = planItem.getItemControl().getRepetitionRule().getCondition();
            boolean isRepeating = false;
            if (StringUtils.isNotEmpty(repetitionCondition)) {
                Expression repetitionExpression = CommandContextUtil.getExpressionManager(commandContext).createExpression(repetitionCondition);
                Object evaluationResult = repetitionExpression.getValue(planItemInstanceEntity);
                if (evaluationResult instanceof Boolean) {
                    isRepeating = (boolean) evaluationResult;
                } else if (evaluationResult instanceof String) {
                    isRepeating = ((String) evaluationResult).toLowerCase().equals("true");
                } else {
                    throw new FlowableException("Repetition condition " + repetitionCondition + " did not evaluate to a boolean value");
                }
            } else {
                isRepeating = true; // no condition set, but a repetition rule defined is assumed to be defaulting to true
            }
            return isRepeating;
        }
        return false;
    }
    
    protected abstract boolean isEvaluateRepetitionRule();
    
}

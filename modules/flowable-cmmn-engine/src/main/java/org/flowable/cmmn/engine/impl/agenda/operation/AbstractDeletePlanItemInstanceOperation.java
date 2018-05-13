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
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.StateTransition;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

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
        
        boolean isRepeating = isRepeatingOnDelete();
        if (isRepeating) {

            // Create new repeating instance
            PlanItemInstanceEntity newPlanItemInstanceEntity = copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, true);
            newPlanItemInstanceEntity.setState(PlanItemInstanceState.WAITING_FOR_REPETITION);
            // Plan item creation "for Repetition"
            CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(newPlanItemInstanceEntity);
            // Plan item doesn't have entry criteria (checked in the if condition) and immediately goes to ACTIVE
            CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(newPlanItemInstanceEntity);
        }
        
        deleteSentryPartInstances();
        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).delete(planItemInstanceEntity);
    }

    /**
     * Implementing classes should be aware that unlike extending from AbstractChangePlanItemInstanceStateOperation, this
     * method will be executed just before the deleting the entity
     */
    @Override
    protected abstract void internalExecute();

    protected boolean isRepeatingOnDelete() {
        
        // If there are not entry criteria and the repetition rule evaluates to true, 
        // a new instance needs to be created.
        
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        if (isEvaluateRepetitionRule() && isPlanItemRepeatableOnComplete(planItem)) {
            return evaluateRepetitionRule(planItemInstanceEntity);
        }
        return false;
    }

    protected void exitChildPlanItemInstances() {
        for (PlanItemInstanceEntity child : planItemInstanceEntity.getChildPlanItemInstances()) {
            if (StateTransition.isPossible(child, PlanItemTransition.EXIT)) {
                CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(child);
            }
        }
    }
    
    protected abstract boolean isEvaluateRepetitionRule();
    
}

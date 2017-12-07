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
import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class ActivatePlanItemInstanceOperation extends AbstractPlanItemInstanceOperation {
    
    public ActivatePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    @Override
    public void run() {
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstanceEntity.getState())) {
            
            // Sentries are not needed to be kept around, as the plan item is being activated
            deleteSentryPartInstances();
            
            planItemInstanceEntity.setState(PlanItemInstanceState.ACTIVE);
            executeActivityBehavior();
            
            // For the first instance of a repeatable plan item, the counter variable needs to be set.
            if (isPlanItemRepeatableOnComplete(planItemInstanceEntity.getPlanItem()) 
                    && getRepetitionCounter(planItemInstanceEntity) == 0) {
                setRepetitionCounter(planItemInstanceEntity, 1);
            }
        }
    }
    
    protected void executeActivityBehavior() {
        CmmnActivityBehavior activityBehavior = (CmmnActivityBehavior) planItemInstanceEntity.getPlanItem().getBehavior();
        if (activityBehavior instanceof CoreCmmnActivityBehavior) {
            ((CoreCmmnActivityBehavior) activityBehavior).execute(commandContext, planItemInstanceEntity);
        } else {
            activityBehavior.execute(planItemInstanceEntity);
        }
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

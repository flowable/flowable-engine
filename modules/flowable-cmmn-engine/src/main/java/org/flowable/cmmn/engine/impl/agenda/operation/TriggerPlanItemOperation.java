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

import org.flowable.cmmn.engine.impl.behavior.CmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class TriggerPlanItemOperation extends AbstractPlanItemInstanceOperation {
    
    public TriggerPlanItemOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    @Override
    public void run() {
        if (PlanItemInstanceState.ACTIVE.equals(planItemInstanceEntity.getState())) {
            Object behaviorObject = planItemInstanceEntity.getPlanItem().getBehavior();
            if (!(behaviorObject instanceof CmmnTriggerableActivityBehavior)) {
                throw new FlowableException("Cannot trigger a plan item which activity behavior does not implement the " 
                        + CmmnTriggerableActivityBehavior.class + " interface");
            }
            CmmnTriggerableActivityBehavior behavior = (CmmnTriggerableActivityBehavior) planItemInstanceEntity.getPlanItem().getBehavior();
            behavior.trigger(planItemInstanceEntity);
        }
    }
    
    @Override
    public String toString() {
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Trigger PlanItem] ");
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

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

import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.StateTransition;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractChangePlanItemInstanceStateOperation extends AbstractPlanItemInstanceOperation {

    public AbstractChangePlanItemInstanceStateOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    public void run() {
        if (planItemInstanceEntity.getPlanItem() != null) { // can be null for the plan model
            Object behavior = planItemInstanceEntity.getPlanItem().getBehavior();
            if (behavior instanceof PlanItemActivityBehavior
                    && StateTransition.isPossible(planItemInstanceEntity, getLifeCycleTransition())) {
                ((PlanItemActivityBehavior) behavior).onStateTransition(commandContext, planItemInstanceEntity, getLifeCycleTransition());
            }
        }

        planItemInstanceEntity.setState(getNewState());
        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteriaOperation(planItemInstanceEntity.getCaseInstanceId(), createPlanItemLifeCycleEvent());
        
        internalExecute();
    }
    
    protected abstract void internalExecute();

    protected PlanItemLifeCycleEvent createPlanItemLifeCycleEvent() {
        return new PlanItemLifeCycleEvent(planItemInstanceEntity.getPlanItem(), getLifeCycleTransition());
    }

    protected abstract String getNewState();

    protected abstract String getLifeCycleTransition();

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        stringBuilder.append("[Change PlanItem state] ");
        if (planItem != null) {
            if (planItem.getName() != null) {
                stringBuilder.append(planItem.getName());
                stringBuilder.append(" (id: ");
                stringBuilder.append(planItem.getId());
                stringBuilder.append(")");
            } else {
                stringBuilder.append(planItem.getId());
            }
        } else {
            stringBuilder.append("(plan item instance with id ").append(planItemInstanceEntity.getId()).append(")");
        }
        stringBuilder.append(", ");
        stringBuilder.append("new state: [").append(getNewState()).append("]");
        stringBuilder.append(" with transition [");
        stringBuilder.append(getLifeCycleTransition());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

}

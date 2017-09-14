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

import java.util.List;

import org.flowable.cmmn.engine.impl.behavior.PlanItemActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.runtime.StateTransition;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.DelegatePlanItemInstance;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Stage;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class StageActivityBehavior implements PlanItemActivityBehavior {
    
    protected Stage stage;
    
    public StageActivityBehavior(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void execute(DelegatePlanItemInstance delegatePlanItemInstance) {
        PlanItemInstanceEntity stagePlanItemInstanceEntity = (PlanItemInstanceEntity) delegatePlanItemInstance;
        stagePlanItemInstanceEntity.setName(delegatePlanItemInstance.getPlanItem().getName());
        CommandContextUtil.getAgenda().planInitStageOperation(stagePlanItemInstanceEntity);
    }
    
    @Override
    public void trigger(DelegatePlanItemInstance planItemInstance) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) planItemInstance;
        List<PlanItemInstanceEntity> childPlanItemInstances = planItemInstanceEntity.getChildren();
        if (childPlanItemInstances != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : childPlanItemInstances) {
                if (StateTransition.isPossible(planItemInstance, PlanItemTransition.COMPLETE)) {
                    CommandContextUtil.getAgenda().planCompletePlanItem(childPlanItemInstance);
                }
            }
        }
        CommandContextUtil.getAgenda(commandContext).planCompletePlanItem((PlanItemInstanceEntity) planItemInstance);
    }
    
    @Override
    public void onStateTransition(DelegatePlanItemInstance planItemInstance, String transition) {
        if (PlanItemTransition.TERMINATE.equals(transition) || PlanItemTransition.EXIT.equals(transition)) {
            handleChildPlanItemInstances(planItemInstance, transition);
        }
    }

    protected void handleChildPlanItemInstances(DelegatePlanItemInstance planItemInstance, String transition) {
        // The stage plan item will be deleted by the regular TerminatePlanItemOperation
        PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) planItemInstance;
        List<PlanItemInstanceEntity> childPlanItemInstances = planItemInstanceEntity.getChildren();
        if (childPlanItemInstances != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : childPlanItemInstances) {
                if (StateTransition.isPossible(planItemInstance, transition)) {
                    if (PlanItemTransition.TERMINATE.equals(transition)) {
                        CommandContextUtil.getAgenda().planTerminatePlanItem(childPlanItemInstance);
                    } else if (PlanItemTransition.EXIT.equals(transition)) {
                        CommandContextUtil.getAgenda().planExitPlanItem(childPlanItemInstance);
                    }
                }
            }
        }
    }

}

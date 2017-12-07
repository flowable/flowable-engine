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
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class ExitPlanItemInstanceOperation extends AbstractDeletePlanItemInstanceOperation {
    
    public ExitPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    @Override
    protected String getNewState() {
        return PlanItemInstanceState.TERMINATED;
    }
    
    @Override
    protected String getLifeCycleTransition() {
        return PlanItemTransition.EXIT;
    }
    
    @Override
    public void run() {
        if (isStage(planItemInstanceEntity)) {
            completeChildPlanItems();
        }
        super.run();
    }

    protected void completeChildPlanItems() {
        for (PlanItemInstanceEntity child : planItemInstanceEntity.getChildren()) {
            if (StateTransition.isPossible(planItemInstanceEntity, PlanItemTransition.EXIT)) {
                CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstance(child);
            }
        }
    }
    
    @Override
    protected boolean isEvaluateRepetitionRule() {
        return false;
    }
    
}

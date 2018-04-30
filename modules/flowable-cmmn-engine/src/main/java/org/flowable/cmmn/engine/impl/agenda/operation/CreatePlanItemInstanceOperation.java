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
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CreatePlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    public CreatePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    protected void internalExecute() {
        if (hasRepetitionRule(planItemInstanceEntity)) {
            //Increase repetition counter, value is kept from the previous instance of the repetition
            //@see CmmOpertion.copyAndInsertPlanItemInstance used by @see EvaluateCriteriaOperation and @see AbstractDeletePlanItemInstanceOperation
            //Or if its the first instance of the repetition, this call sets the counter to 1
            setRepetitionCounter(planItemInstanceEntity, getRepetitionCounter(planItemInstanceEntity) + 1);
        }
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceCreated(planItemInstanceEntity);
        //Extending classes might override getNewState
        if (getNewState().equals(PlanItemInstanceState.AVAILABLE)) {
            CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceAvailable(planItemInstanceEntity);
        }
    }

    @Override
    protected String getNewState() {
        return PlanItemInstanceState.AVAILABLE;
    }

    @Override
    protected String getLifeCycleTransition() {
        return PlanItemTransition.CREATE;
    }

}

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
 * A specific create operation for plan items being created out of a repetition where the repetition rule is actually treated as part of the main plan item
 * and not within the create operation as it is usually been treated.
 *
 * @author Micha Kiener
 */
public class CreateRepeatedPlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    public CreateRepeatedPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    protected void internalExecute() {
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceCreated(planItemInstanceEntity);
        planItemInstanceEntity.setLastAvailableTime(getCurrentTime(commandContext));
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceAvailable(planItemInstanceEntity);
    }

    @Override
    public String getNewState() {
        return PlanItemInstanceState.AVAILABLE;
    }

    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.CREATE;
    }

    @Override
    public String getOperationName() {
        return "[Create repeated plan item]";
    }

}

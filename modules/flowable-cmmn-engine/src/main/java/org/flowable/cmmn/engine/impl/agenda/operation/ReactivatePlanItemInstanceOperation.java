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
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.ReactivateEventListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Reactivates a plan item as part of a case reactivation which is pretty similar to the {@link CreatePlanItemInstanceOperation}, but uses a different transition
 * for the listeners as the newly created plan item instance is created as part of a reactivation process.
 *
 * @author Micha Kiener
 */
public class ReactivatePlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    public ReactivatePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }

    @Override
    protected void internalExecute() {
        CmmnHistoryManager cmmnHistoryManager = CommandContextUtil.getCmmnHistoryManager(commandContext);
        cmmnHistoryManager.recordPlanItemInstanceReactivated(planItemInstanceEntity);

        // Extending classes might override getNewState, so need to check the available state again
        if (getNewState().equals(PlanItemInstanceState.AVAILABLE)) {
            planItemInstanceEntity.setLastAvailableTime(getCurrentTime(commandContext));
            cmmnHistoryManager.recordPlanItemInstanceAvailable(planItemInstanceEntity);
        }
    }

    @Override
    public String getNewState() {
        if (isEventListenerWithAvailableCondition(planItemInstanceEntity.getPlanItem())) {
            // We need a specific treatment of the reactivation event listener as it also has a condition to avoid being available at active state of the case,
            // but when reactivating its plan item to actually trigger a case reactivation, we need to ignore that condition and directly go to available state
            // to trigger the case reactivation
            if (planItemInstanceEntity.getPlanItemDefinition() != null && planItemInstanceEntity.getPlanItemDefinition() instanceof ReactivateEventListener) {
                return PlanItemInstanceState.AVAILABLE;
            }
            return PlanItemInstanceState.UNAVAILABLE;
        } else {
            return PlanItemInstanceState.AVAILABLE;
        }
    }

    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.REACTIVATE;
    }

    @Override
    public String getOperationName() {
        return "[Reactivate plan item]";
    }

}

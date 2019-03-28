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
package org.flowable.cmmn.engine.impl.util;

import java.util.Collection;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceContainerUtil {

    public static boolean isEndStateReachedForAllChildPlanItems(PlanItemInstanceContainer planItemInstanceContainer) {
        return isEndStateReachedForAllChildPlanItems(planItemInstanceContainer, null);
    }

    public static boolean isEndStateReachedForAllChildPlanItems(PlanItemInstanceContainer planItemInstanceContainer, Collection<String> planItemInstanceIdsToIgnore) {
        if (planItemInstanceContainer.getChildPlanItemInstances() != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : planItemInstanceContainer.getChildPlanItemInstances()) {
                if (planItemInstanceIdsToIgnore == null || !planItemInstanceIdsToIgnore.contains(childPlanItemInstance.getId())) {
                    if (!PlanItemInstanceState.END_STATES.contains(childPlanItemInstance.getState())) {
                        return false;
                    }
                    boolean allChildChildsEndStateReached = isEndStateReachedForAllChildPlanItems(childPlanItemInstance);
                    if (!allChildChildsEndStateReached) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isEndStateReachedForAllRequiredChildPlanItems(PlanItemInstanceContainer planItemInstanceContainer) {
        return isEndStateReachedForAllRequiredChildPlanItems(CommandContextUtil.getCommandContext(), planItemInstanceContainer);
    }

    public static boolean isEndStateReachedForAllRequiredChildPlanItems(CommandContext commandContext, PlanItemInstanceContainer planItemInstanceContainer) {
        return isEndStateReachedForAllRequiredChildPlanItems(commandContext, planItemInstanceContainer, null);
    }

    public static boolean isEndStateReachedForAllRequiredChildPlanItems(PlanItemInstanceContainer planItemInstanceContainer, Collection<String> planItemInstanceIdsToIgnore) {
        return isEndStateReachedForAllRequiredChildPlanItems(CommandContextUtil.getCommandContext(), planItemInstanceContainer, planItemInstanceIdsToIgnore);
    }

    public static boolean isEndStateReachedForAllRequiredChildPlanItems(CommandContext commandContext,
            PlanItemInstanceContainer planItemInstanceContainer, Collection<String> planItemInstanceIdsToIgnore) {

        if (planItemInstanceContainer.getChildPlanItemInstances() != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : planItemInstanceContainer.getChildPlanItemInstances()) {
                if (planItemInstanceIdsToIgnore == null || !planItemInstanceIdsToIgnore.contains(childPlanItemInstance.getId())) {
                    if (PlanItemInstanceState.END_STATES.contains(childPlanItemInstance.getState())) {
                        continue;
                    }
                    if (ExpressionUtil.isRequiredPlanItemInstance(commandContext, childPlanItemInstance)) {
                        return false;
                    }
                    if (PlanItemInstanceState.ACTIVE.equals(childPlanItemInstance.getState())) {
                        return false;
                    }
                    if (!PlanItemInstanceContainerUtil.isEndStateReachedForAllChildPlanItems(childPlanItemInstance)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

}

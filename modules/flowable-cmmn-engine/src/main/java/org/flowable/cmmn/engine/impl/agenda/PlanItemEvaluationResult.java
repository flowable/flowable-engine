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
package org.flowable.cmmn.engine.impl.agenda;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;

/**
 * This objects holds the result from a loop through a list of plan items evaluating their entry criteria, repetition rule and activation.
 *
 * @author Micha Kiener
 */
public class PlanItemEvaluationResult {

    /** The list with all the child plan items, including completed ones as they might be needed when evaluating parent completion rules. */
    List<PlanItemInstanceEntity> allChildPlanItemInstances;

    /**
     * The number of child plan items becoming active throughout the evaluation loop.
     */
    int activeChildren = 0;

    /**
     * Turns to true, as soon as at least some criteria changed while evaluating, which will trigger yet another round of evaluation, as some conditions
     * might become true afterwards.
     */
    boolean criteriaChanged = false;

    /**
     * We need to store new child plan item instances in a list until the evaluation loop is done, to avoid concurrent modifications.
     */
    List<PlanItemInstanceEntity> newChildPlanItemInstances = null;

    public PlanItemEvaluationResult() {
    }

    public PlanItemEvaluationResult(List<PlanItemInstanceEntity> allChildPlanItemInstances) {
        this.allChildPlanItemInstances = allChildPlanItemInstances;
    }

    public void increaseActiveChildren() {
        activeChildren++;
    }

    public void addChildPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        if (newChildPlanItemInstances == null) {
            newChildPlanItemInstances = new ArrayList<>();
        }
        newChildPlanItemInstances.add(planItemInstanceEntity);
    }

    public void markCriteriaChanged() {
        criteriaChanged = true;
    }

    public int getActiveChildren() {
        return activeChildren;
    }

    public boolean isCriteriaChanged() {
        return criteriaChanged;
    }

    public boolean hasNewChildPlanItemInstances() {
        return newChildPlanItemInstances != null && newChildPlanItemInstances.size() > 0;
    }

    public List<PlanItemInstanceEntity> getNewChildPlanItemInstances() {
        return newChildPlanItemInstances;
    }

    public boolean criteriaChangedOrNewActiveChildren() {
        return criteriaChanged || activeChildren > 0;
    }

    public List<PlanItemInstanceEntity> getAllChildPlanItemInstances() {
        return allChildPlanItemInstances;
    }

    /**
     * Returns true, if the given plan item instance has at least one instance in completed state (only possible of course for repetition based plan items).
     *
     * @param planItemInstance the plan item instance to check for a completed instance of the same plan item
     * @return true, if a completed instance was found, false otherwise
     */
    public boolean hasCompletedPlanItemInstance(PlanItemInstanceEntity planItemInstance) {
        if (allChildPlanItemInstances == null || allChildPlanItemInstances.size() == 0) {
            return false;
        }

        for (PlanItemInstanceEntity childPlanItemInstance : allChildPlanItemInstances) {
            if (childPlanItemInstance.getPlanItemDefinitionId().equals(planItemInstance.getPlanItemDefinitionId()) && PlanItemInstanceState.COMPLETED
                .equals(childPlanItemInstance.getState())) {
                return true;
            }
        }
        return false;
    }
}

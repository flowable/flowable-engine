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

import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.ReactivateEventListener;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * This operation reactivates a case model by first setting its reactivation event listener to active and triggering it, then in a second phase step through
 * all plan items having a reactivation sentry and reactivate them before the model is completely re-evaluated again for reactivation of the case.
 *
 * @author Micha Kiener
 */
public class ReactivateCaseInstanceOperation extends AbstractCaseInstanceOperation {

    protected CaseInstanceEntity caseInstanceEntity;

    public ReactivateCaseInstanceOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, null, caseInstanceEntity);
        this.caseInstanceEntity = caseInstanceEntity;
    }

    @Override
    public void run() {
        super.run();

        ReactivateEventListener reactivateEventListener = CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getReactivateEventListener();
        List<PlanItemInstanceEntity> planItems = caseInstanceEntity.getChildPlanItemInstances();

        // we first search for the reactivation event, set it to available and then actually trigger it
        for (PlanItemInstanceEntity planItem : planItems) {
            if (planItem.getPlanItemDefinitionId().equals(reactivateEventListener.getId())) {
                // reactivate the listener and direct dependencies of it so they can be triggered later
                reactivatePlanItem(planItem, PlanItemInstanceState.AVAILABLE);
                reactivateDependingPlanItems(planItem, planItems);

                CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(planItem);
            }
        }

        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteriaOperation(caseInstanceEntity.getId());
    }

    /**
     * Reactivates all plan items having a dependency on the reactivation listener in order to become active once we trigger the reactivation listener.
     * This is necessary as we don't want to force modelers to use repetition just for reactivation to work, but reactivate depending plan items
     * automatically, even though they might have been active before.
     *
     * @param reactivateEventListener the listener to activate depending plan items for
     * @param planItems the list of plan items of the case to search for depending ones
     */
    protected void reactivateDependingPlanItems(PlanItemInstanceEntity reactivateEventListener, List<PlanItemInstanceEntity> planItems) {
        // search for all the direct dependencies the reactivation listener has as we need to (re-)activate them as well in order to be triggered
        // by the listener, which is what we call first phase of reactivation
        List<PlanItem> entryDependentPlanItems = reactivateEventListener.getPlanItem().getEntryDependentPlanItems();
        if (entryDependentPlanItems != null) {
            for (PlanItem entryDependentPlanItem : entryDependentPlanItems) {
                reactivatePlanItem(searchPlanItemInstance(entryDependentPlanItem, planItems), PlanItemInstanceState.AVAILABLE);
            }
        }
    }

    /**
     * Reactivates the given plan item into the provided new state. It will also reset all necessary data like ending times to prepare the plan item
     * being active again once the listener is triggered.
     *
     * @param planItem the plan item to be reactivated
     * @param newState the new state to set
     */
    protected void reactivatePlanItem(PlanItemInstanceEntity planItem, String newState) {
        planItem.setState(newState);
        planItem.setEndedTime(null);
        planItem.setCompletedTime(null);
        planItem.setExitTime(null);

        CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).update(planItem);
    }

    protected PlanItemInstanceEntity searchPlanItemInstance(PlanItem planItem, List<PlanItemInstanceEntity> planItemInstances) {
        for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
            if (planItemInstance.getPlanItemDefinitionId().equals(planItem.getPlanItemDefinition().getId())) {
                return planItemInstance;
            }
        }
        throw new FlowableIllegalArgumentException(
            "Could not find plan item instance for plan item with definition id " + planItem.getPlanItemDefinition().getId());
    }

    @Override
    public String toString() {
        return "[Init Plan Model] initializing plan model for case instance " + caseInstanceEntity.getId();
    }

}

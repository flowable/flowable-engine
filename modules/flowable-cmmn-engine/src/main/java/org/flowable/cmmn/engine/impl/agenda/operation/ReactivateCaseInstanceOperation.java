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

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
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
        List<PlanItemInstanceEntity> planItemInstances = caseInstanceEntity.getChildPlanItemInstances();

        // we first search for the reactivation event, set it to available and then actually trigger it
        PlanItemInstanceEntity reactivationListenerPlanItemInstance = searchPlanItemInstance(reactivateEventListener.getId(), planItemInstances);
        if (reactivationListenerPlanItemInstance == null) {
            throw new FlowableIllegalArgumentException("Could not find reactivation listener plan item instance in case " + caseInstanceEntity.getId());
        }

        // reactivate the listener and direct dependencies of it so they can be triggered later
        // we don't reuse the existing plan item instance but rather create a new one so we don't lose its history and data
        PlanItemInstanceEntity reactivationListener = reactivatePlanItem(reactivationListenerPlanItemInstance);
        CommandContextUtil.getAgenda(commandContext).planReactivatePlanItemInstanceOperation(reactivationListener);

        // all directly depending plan items need to be reactivated as well to be in the correct state before the listener is triggered
        reactivateDependingPlanItems(reactivationListener, planItemInstances);

        // now as all depending plan items have been reactivated, trigger the reactivation event listener to start the reactivation of the case
        CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(reactivationListener);
    }

    /**
     * Reactivates all plan items having a dependency on the reactivation listener in order to become active once we trigger the reactivation listener.
     * This is necessary as we don't want to force modelers to use repetition just for reactivation to work, but reactivate depending plan items
     * automatically, even though they might have been active before.
     *
     * @param reactivateEventListener the listener to activate depending plan items for
     * @param planItemInstances the list of plan items of the case to search for depending ones
     */
    protected void reactivateDependingPlanItems(PlanItemInstanceEntity reactivateEventListener, List<PlanItemInstanceEntity> planItemInstances) {
        // search for all the direct dependencies the reactivation listener has as we need to (re-)activate them as well in order to be triggered
        // by the listener, which is what we call first phase of reactivation
        List<PlanItem> entryDependentPlanItems = reactivateEventListener.getPlanItem().getEntryDependentPlanItems();
        if (entryDependentPlanItems != null) {
            for (PlanItem entryDependentPlanItem : entryDependentPlanItems) {
                PlanItemInstanceEntity planItemInstance = reactivatePlanItem(searchPlanItemInstance(entryDependentPlanItem.getPlanItemDefinition().getId(), planItemInstances));
                CommandContextUtil.getAgenda(commandContext).planReactivatePlanItemInstanceOperation(planItemInstance);
            }
        }
    }

    /**
     * Reactivates the given plan item by creating a new instance with the same data, but of course no timestamps yet set so we keep the original one in place
     * with all its information. After this reactivation make sure to plan its reactivation using the agenda for further processing of the reactivation.
     *
     * @param planItemInstance the plan item to be reactivated
     * @return the newly reactivated plan item instance
     */
    protected PlanItemInstanceEntity reactivatePlanItem(PlanItemInstanceEntity planItemInstance) {
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        PlanItemInstance stagePlanItem = planItemInstance.getStagePlanItemInstanceEntity();
        if (stagePlanItem == null && planItemInstance.getStageInstanceId() != null) {
            stagePlanItem = planItemInstanceEntityManager.findById(planItemInstance.getStageInstanceId());
        }

        return planItemInstanceEntityManager
            .createPlanItemInstanceEntityBuilder()
            .planItem(planItemInstance.getPlanItem())
            .caseDefinitionId(planItemInstance.getCaseDefinitionId())
            .caseInstanceId(planItemInstance.getCaseInstanceId())
            .stagePlanItemInstance(stagePlanItem)
            .tenantId(planItemInstance.getTenantId())
            .addToParent(true)
            .silentNameExpressionEvaluation(false)
            .create();
    }

    protected PlanItemInstanceEntity searchPlanItemInstance(String planItemDefinitionId, List<PlanItemInstanceEntity> planItemInstances) {
        for (PlanItemInstanceEntity planItemInstance : planItemInstances) {
            if (planItemInstance.getPlanItemDefinitionId().equals(planItemDefinitionId)) {
                return planItemInstance;
            }
        }
        throw new FlowableIllegalArgumentException("Could not find plan item instance for plan item with definition id " + planItemDefinitionId);
    }

    @Override
    public String toString() {
        return "[Init Plan Model] initializing plan model for case instance " + caseInstanceEntity.getId();
    }

}

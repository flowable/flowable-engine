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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CaseInstanceUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public abstract class AbstractPlanItemInstanceOperation extends CmmnOperation {
    
    protected PlanItemInstanceEntity planItemInstanceEntity;

    public AbstractPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext);
        this.planItemInstanceEntity = planItemInstanceEntity;
    }
    
    public PlanItemInstanceEntity getPlanItemInstanceEntity() {
        return planItemInstanceEntity;
    }

    public void setPlanItemInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity) {
        this.planItemInstanceEntity = planItemInstanceEntity;
    }
    
    protected void removeSentryRelatedData() {
        deleteSentryPartInstances();
        deleteOrphanEventListeners();
    }

    /**
     * Deletes any part instance of a sentry that was satisfied before.
     */
    protected void deleteSentryPartInstances() {
        SentryPartInstanceEntityManager sentryPartInstanceEntityManager = CommandContextUtil.getSentryPartInstanceEntityManager(commandContext);
        if (planItemInstanceEntity.getPlanItem() != null
                && (!planItemInstanceEntity.getPlanItem().getEntryCriteria().isEmpty()
                        || !planItemInstanceEntity.getPlanItem().getExitCriteria().isEmpty())) {
            if (planItemInstanceEntity.getSatisfiedSentryPartInstances() != null) {
                for (SentryPartInstanceEntity sentryPartInstanceEntity : planItemInstanceEntity.getSatisfiedSentryPartInstances()) {
                    sentryPartInstanceEntityManager.delete(sentryPartInstanceEntity);
                }
            }
        }
    }

    /**
     * Event listeners can become 'orphaned': when they reference sentries on plan item instances
     * that have moved to a terminal state, they would occur without anything listening to them
     * (and block completion of the parent stage). In that situation, they need to be removed.
     */
    protected void deleteOrphanEventListeners() {

        /*
         * 'Orphan' event listeners are event listeners that are no longer referenced by any plan item instance that is not in a terminal state.
         * Said simpler: when they occur, nothing is listening to them anymore. As such, they have no value to keep around.
         * For example, imagine a user event listener is used to exit a stage. When the stage goes into a terminal state (via another way),
         * there is no purpose for the user event listener anymore. At that point, it is removed.
         *
         * Note that this needs to be done both on activating and terminating a plan item instance, as orphans can happen in both cases.
         *
         * The way these orphan event listeners are determined is as follows:
         * 1. Get all dependencies of the plan item that's currently being moved into a terminal state.
         *    (dependencies are all other plan items that are references through its sentries)
         * 2. Filter out the event listeners from that list.
         * 3. For those event listeners, get the dependent plan items.
         *    These dependents are the plan items that depend in their sentries on this event listener.
         * 4. Fetch the plan item instances for these dependent plan items.
         *    If they are all in a terminal state, the user event listener is an orphan and can be deleted.
         */

        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        if (planItem != null) {

            // Step 1 and 2
            List<PlanItem> eventListenerDependencies = new ArrayList<>();
            eventListenerDependencies.addAll(planItem.getEntryDependencies().stream()
                .filter(p -> p.getPlanItemDefinition() instanceof EventListener).collect(Collectors.toList()));
            eventListenerDependencies.addAll(planItem.getExitDependencies().stream()
                .filter(p -> p.getPlanItemDefinition() instanceof EventListener).collect(Collectors.toList()));

            // Special case: if the current plan item is a stage, we need to also verify all event listeners
            // that reference a child plan item of this stage. Normally this will happen automatically, unless
            // the child plan item instances haven't been created yet (e.g with nested stages).
            // As such, we need to check all plan items for which there hasn't been a plan item instance yet.

            if (planItem.getPlanItemDefinition() instanceof PlanFragment
                && PlanItemInstanceState.isInTerminalState(planItemInstanceEntity)) { //  This is only true when stopping, not when a plan item gets activated (in that case, the child plan items can still be created and no special care is needed)

                List<PlanItem> childPlanItemsWithDependencies = getChildPlanItemsWithDependencies((PlanFragment) planItem.getPlanItemDefinition());

                CaseInstanceEntity caseInstanceEntity = CommandContextUtil
                    .getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());
                Map<String, List<PlanItemInstanceEntity>> childPlanItemInstancesMap = CaseInstanceUtil
                    .findChildPlanItemInstancesMap(caseInstanceEntity, childPlanItemsWithDependencies);

                for (PlanItem childPlanItemWithDependencies : childPlanItemsWithDependencies) {
                    if (!childPlanItemInstancesMap.containsKey(childPlanItemWithDependencies.getId())) {
                        eventListenerDependencies.addAll(childPlanItemWithDependencies.getEntryDependencies().stream()
                            .filter(p -> p.getPlanItemDefinition() instanceof EventListener).collect(Collectors.toList()));
                        eventListenerDependencies.addAll(childPlanItemWithDependencies.getExitDependencies().stream()
                            .filter(p -> p.getPlanItemDefinition() instanceof EventListener).collect(Collectors.toList()));
                    }
                }
            }

            // Step 3
            if (!eventListenerDependencies.isEmpty()) {
                CaseInstanceEntity caseInstanceEntity = CommandContextUtil
                    .getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());
                for (PlanItem eventListenerPlanItem : eventListenerDependencies) {

                    List<PlanItemInstanceEntity> eventListenerPlanItemInstance = CaseInstanceUtil
                        .findNonTerminalChildPlanItemInstances(caseInstanceEntity, eventListenerPlanItem);

                    if (!eventListenerPlanItemInstance.isEmpty()) {

                        List<PlanItem> dependentPlanItems = eventListenerPlanItem.getDependentPlanItems();
                        List<PlanItemInstanceEntity> dependentPlanItemInstances = CaseInstanceUtil
                            .findChildPlanItemInstances(caseInstanceEntity, dependentPlanItems);

                        // Step 4
                        Optional<PlanItemInstanceEntity> nonTerminalPlanItemInstance = dependentPlanItemInstances.stream()
                            .filter(p -> !PlanItemInstanceState.TERMINAL_STATES.contains(p.getState())).findFirst();
                        if (!nonTerminalPlanItemInstance.isPresent()) {
                            for (PlanItemInstanceEntity eventListenerPlanItemInstanceEntity : eventListenerPlanItemInstance) {
                                CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(eventListenerPlanItemInstanceEntity);
                            }
                        }
                    }
                }
            }
        }

     }

    protected boolean isPlanItemRepeatableOnComplete(PlanItem planItem) {
        return  planItem != null
                && planItem.getEntryCriteria().isEmpty()
                && planItem.getItemControl() != null
                && planItem.getItemControl().getRepetitionRule() != null;
    }

    protected Date getCurrentTime(CommandContext commandContext) {
        return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime();
    }

    protected List<PlanItem> getChildPlanItemsWithDependencies(PlanFragment planFragment) {
        List<PlanItem> childPlanItemsWithDependencies = new ArrayList<>();
        internalGetChildPlanItemsWithDependencies(planFragment, childPlanItemsWithDependencies);
        return childPlanItemsWithDependencies;
    }

    protected void internalGetChildPlanItemsWithDependencies(PlanFragment planFragment, List<PlanItem> childPlanItemsWithDependencies) {
        for (PlanItem planItem : planFragment.getPlanItems()) {
            if (!planItem.getEntryDependencies().isEmpty() || !planItem.getExitDependencies().isEmpty()) {
                childPlanItemsWithDependencies.add(planItem);

                if (planItem.getPlanItemDefinition() instanceof PlanFragment) {
                    internalGetChildPlanItemsWithDependencies((PlanFragment) planItem.getPlanItemDefinition(), childPlanItemsWithDependencies);
                }
            }
        }
    }
    
}

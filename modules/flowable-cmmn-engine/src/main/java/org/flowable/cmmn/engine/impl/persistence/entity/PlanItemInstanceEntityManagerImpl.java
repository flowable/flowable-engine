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

package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceQuery;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.converter.util.PlanItemDependencyUtil;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.runtime.PlanItemInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.util.CaseInstanceUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManager;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceEntityManagerImpl
    extends AbstractEngineEntityManager<CmmnEngineConfiguration, PlanItemInstanceEntity, PlanItemInstanceDataManager>
    implements PlanItemInstanceEntityManager {
    
    public PlanItemInstanceEntityManagerImpl(CmmnEngineConfiguration cmmnEngineConfiguration, PlanItemInstanceDataManager planItemInstanceDataManager) {
        super(cmmnEngineConfiguration, planItemInstanceDataManager);
    }
    
    @Override
    public PlanItemInstanceEntity createChildPlanItemInstance(PlanItem planItem, String caseDefinitionId, String caseInstanceId, 
            String stagePlanItemInstanceId, String tenantId, boolean addToParent) {
        
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ExpressionManager expressionManager = engineConfiguration.getExpressionManager();
        CaseInstanceEntity caseInstanceEntity = getCaseInstanceEntityManager().findById(caseInstanceId);
        
        PlanItemInstanceEntity planItemInstanceEntity = create();
        planItemInstanceEntity.setCaseDefinitionId(caseDefinitionId);
        planItemInstanceEntity.setCaseInstanceId(caseInstanceId);
        if (planItem.getName() != null) {
            Expression nameExpression = expressionManager.createExpression(planItem.getName());
            planItemInstanceEntity.setName(nameExpression.getValue(caseInstanceEntity).toString());
        }
        planItemInstanceEntity.setCreateTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        planItemInstanceEntity.setElementId(planItem.getId());
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        if (planItemDefinition != null) {
            planItemInstanceEntity.setPlanItemDefinitionId(planItemDefinition.getId());

            String planItemDefinitionType = planItemDefinition.getClass().getSimpleName().toLowerCase();
            planItemInstanceEntity.setPlanItemDefinitionType(planItemDefinitionType);
            planItemInstanceEntity.setStage(PlanItemDefinitionType.STAGE.equals(planItemDefinitionType));
        } else {
            planItemInstanceEntity.setStage(false);
        }
        planItemInstanceEntity.setStageInstanceId(stagePlanItemInstanceId);
        planItemInstanceEntity.setTenantId(tenantId);
       
        insert(planItemInstanceEntity);
        
        if (addToParent) {
            addPlanItemInstanceToParent(commandContext, planItemInstanceEntity);
        }
        
        return planItemInstanceEntity;
    }
    
    protected void addPlanItemInstanceToParent(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        if (planItemInstanceEntity.getStageInstanceId() != null) {
            PlanItemInstanceEntity stagePlanItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                    .findById(planItemInstanceEntity.getStageInstanceId());
            stagePlanItemInstanceEntity.getChildPlanItemInstances().add(planItemInstanceEntity);
        } else {
            CaseInstanceEntity caseInstanceEntity = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());
            caseInstanceEntity.getChildPlanItemInstances().add(planItemInstanceEntity);
        }
    }

    @Override
    public void deleteSentryRelatedData(String planItemId) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        PlanItemInstanceEntity planItemInstanceEntity = dataManager.findById(planItemId);

        deleteSentryPartInstances(commandContext, planItemInstanceEntity);
        deleteOrphanEventListeners(commandContext, planItemInstanceEntity);
    }

    /**
     * Deletes any part instance of a sentry that was satisfied before to clean it up for further evaluation cycles.
     */
    protected void deleteSentryPartInstances(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        SentryPartInstanceEntityManager sentryPartInstanceEntityManager = CommandContextUtil.getSentryPartInstanceEntityManager(commandContext);
        if (planItemInstanceEntity.getPlanItem() != null
            && (!planItemInstanceEntity.getPlanItem().getEntryCriteria().isEmpty()
            || !planItemInstanceEntity.getPlanItem().getExitCriteria().isEmpty())) {
            if (planItemInstanceEntity.getSatisfiedSentryPartInstances() != null) {
                for (SentryPartInstanceEntity sentryPartInstanceEntity : planItemInstanceEntity.getSatisfiedSentryPartInstances()) {
                    sentryPartInstanceEntityManager.delete(sentryPartInstanceEntity);
                }
                // reset the internal satisfied sentry list, so it is not re-used again
                planItemInstanceEntity.setSatisfiedSentryPartInstances(null);
            }
        }
    }

    /**
     * Event listeners can become 'orphaned': when they reference sentries on plan item instances
     * that have moved to a terminal state, they would occur without anything listening to them
     * (and block completion of the parent stage). In that situation, they need to be removed.
     */
    protected void deleteOrphanEventListeners(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {

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
         *    - If the event listener is used for an entry criterion: if they are all in a state different from available, it is an orphan.
         *    - If the event listener is used for an exit criterion: if they are all in a terminal state, the user event listener is an orphan and can be deleted.
         *    Note: if the plan item instance has a parent that is non-terminal, the plan item instance can still become active later and the event listener still is needed
         */

        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        if (planItem != null) {

            // Step 1 and 2 (gather and filter)
            List<PlanItem> eventListenerDependencies = gatherEventListenerDependencies(commandContext, planItem, planItemInstanceEntity);

            // Step 3 and 4 (determine dependent plan items for the event listener and check if it's orphaned)
            if (!eventListenerDependencies.isEmpty()) {
                terminateOrphanedEventListeners(commandContext, planItemInstanceEntity, eventListenerDependencies);
            }
        }
    }


    protected List<PlanItem> gatherEventListenerDependencies(CommandContext commandContext, PlanItem planItem, PlanItemInstanceEntity planItemInstanceEntity) {

        List<PlanItem> eventListenerDependencies = Stream.concat(
            planItem.getEntryDependencies().stream().filter(p -> p.getPlanItemDefinition() instanceof EventListener),
            planItem.getExitDependencies().stream().filter(p -> p.getPlanItemDefinition() instanceof EventListener))
            .collect(Collectors.toList());

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

        return eventListenerDependencies;
    }

    protected void terminateOrphanedEventListeners(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,  List<PlanItem> eventListenerDependencies) {

        CaseInstanceEntity caseInstanceEntity = CommandContextUtil
            .getCaseInstanceEntityManager(commandContext).findById(planItemInstanceEntity.getCaseInstanceId());

        for (PlanItem eventListenerPlanItem : eventListenerDependencies) {

            List<PlanItemInstanceEntity> eventListenerPlanItemInstance = CaseInstanceUtil
                .findNonTerminalChildPlanItemInstances(caseInstanceEntity, eventListenerPlanItem);

            // Step 3
            if (!eventListenerPlanItemInstance.isEmpty()) {

                List<PlanItem> dependentPlanItems = eventListenerPlanItem.getAllDependentPlanItems();
                Map<String, List<PlanItemInstanceEntity>> dependentPlanItemInstancesMap = CaseInstanceUtil
                    .findChildPlanItemInstancesMap(caseInstanceEntity, dependentPlanItems);

                // Step 4
                boolean isOrphan = true;
                Iterator<PlanItem> planItemIterator = dependentPlanItems.iterator();
                while (isOrphan && planItemIterator.hasNext()) {

                    PlanItem dependentPlanItem = planItemIterator.next();

                    List<PlanItemInstanceEntity> dependentPlanItemInstances = dependentPlanItemInstancesMap.get(dependentPlanItem.getId());
                    if (dependentPlanItemInstances != null && !dependentPlanItemInstances.isEmpty()) {

                        // In case there are instances, check the state

                        for (PlanItemInstanceEntity dependentPlanItemInstance : dependentPlanItemInstances) {
                            if (PlanItemDependencyUtil.isEntryDependency(dependentPlanItemInstance.getPlanItem(), eventListenerPlanItem)) {
                                if (PlanItemInstanceState.AVAILABLE.equals(dependentPlanItemInstance.getState())
                                    || PlanItemInstanceState.WAITING_FOR_REPETITION.equals(dependentPlanItemInstance.getState())) {
                                    isOrphan = false;
                                }
                            }

                            if (PlanItemDependencyUtil.isExitDependency(dependentPlanItemInstance.getPlanItem(), eventListenerPlanItem)) {
                                if (!PlanItemInstanceState.TERMINAL_STATES.contains(dependentPlanItemInstance.getState())) {
                                    isOrphan = false;
                                }
                            }
                        }

                    } else {

                        // In case there are no instances, we need to check potential parents.
                        // If there is a nonterminal parent, the event listener still might be needed and cannot be terminated

                        Stage parentStage = dependentPlanItem.getParentStage();
                        while (isOrphan && parentStage != null && !parentStage.isPlanModel()) {
                            List<PlanItemInstanceEntity> nonTerminalStagePlanItemInstances = CaseInstanceUtil
                                .findNonTerminalChildPlanItemInstances(caseInstanceEntity, parentStage.getPlanItem());
                            if (!nonTerminalStagePlanItemInstances.isEmpty()) {
                                isOrphan = false;
                            } else {
                                parentStage = parentStage.getParentStage();
                            }
                        }

                    }

                }

                if (isOrphan) {
                    for (PlanItemInstanceEntity eventListenerPlanItemInstanceEntity : eventListenerPlanItemInstance) {
                        // don't propagate the exit event type and exit type, if used on orphaned child plan items
                        CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(eventListenerPlanItemInstanceEntity,
                            null, null);
                    }
                }

            }
        }
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

    @Override
    public void deleteByCaseDefinitionId(String caseDefinitionId) {
        dataManager.deleteByCaseDefinitionId(caseDefinitionId);
    }
    
    @Override
    public void deleteByStageInstanceId(String stageInstanceId) {
        dataManager.deleteByStageInstanceId(stageInstanceId);
    }
    
    @Override
    public void deleteByCaseInstanceId(String caseInstanceId) {
        dataManager.deleteByCaseInstanceId(caseInstanceId);
    }
    
    @Override
    public PlanItemInstanceQuery createPlanItemInstanceQuery() {
        return new PlanItemInstanceQueryImpl(engineConfiguration.getCommandExecutor());
    }

    @Override
    public long countByCriteria(PlanItemInstanceQuery planItemInstanceQuery) {
        return dataManager.countByCriteria((PlanItemInstanceQueryImpl) planItemInstanceQuery);
    }

    @Override
    public List<PlanItemInstance> findByCriteria(PlanItemInstanceQuery planItemInstanceQuery) {
        return dataManager.findByCriteria((PlanItemInstanceQueryImpl) planItemInstanceQuery);
    }
    
    @Override
    public List<PlanItemInstanceEntity> findByCaseInstanceId(String caseInstanceId) {
        return dataManager.findByCaseInstanceId(caseInstanceId);
    }

    @Override
    public List<PlanItemInstanceEntity> findByStagePlanItemInstanceId(String stagePlanItemInstanceId) {
        return dataManager.findByStagePlanItemInstanceId(stagePlanItemInstanceId);
    }

    @Override
    public List<PlanItemInstanceEntity> findByCaseInstanceIdAndPlanItemId(String caseInstanceId, String planitemId) {
        return dataManager.findByCaseInstanceIdAndPlanItemId(caseInstanceId, planitemId);
    }

    @Override
    public void delete(PlanItemInstanceEntity planItemInstanceEntity, boolean fireEvent) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        
        CountingPlanItemInstanceEntity countingPlanItemInstanceEntity = (CountingPlanItemInstanceEntity) planItemInstanceEntity;
        
        // Variables
        if (countingPlanItemInstanceEntity.getVariableCount() > 0) {
            VariableInstanceEntityManager variableInstanceEntityManager 
                = CommandContextUtil.getVariableServiceConfiguration(commandContext).getVariableInstanceEntityManager();
            List<VariableInstanceEntity> variableInstanceEntities = variableInstanceEntityManager
                    .findVariableInstanceBySubScopeIdAndScopeType(planItemInstanceEntity.getId(), ScopeTypes.CMMN);
            for (VariableInstanceEntity variableInstanceEntity : variableInstanceEntities) {
                variableInstanceEntityManager.delete(variableInstanceEntity);
            }
        }
        
        if (planItemInstanceEntity.isStage()) {
            if (planItemInstanceEntity.getChildPlanItemInstances() != null && !planItemInstanceEntity.getChildPlanItemInstances().isEmpty()) {
                for (PlanItemInstanceEntity childPlanItem : planItemInstanceEntity.getChildPlanItemInstances()) {
                    delete(childPlanItem, fireEvent);
                }
            }
        }
        
        if (planItemInstanceEntity.getPlanItemDefinitionType().equals(PlanItemDefinitionType.TIMER_EVENT_LISTENER)) {
            TimerJobEntityManager timerJobEntityManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobEntityManager();
            List<TimerJobEntity> timerJobsEntities = timerJobEntityManager
                .findJobsByScopeIdAndSubScopeId(planItemInstanceEntity.getCaseInstanceId(), planItemInstanceEntity.getId());
            for (TimerJobEntity timerJobEntity : timerJobsEntities) {
                timerJobEntityManager.delete(timerJobEntity);
            }
        }

        if (planItemInstanceEntity.getPlanItemDefinitionType().equals(PlanItemDefinitionType.CASE_PAGE_TASK)) {
            IdentityLinkEntityManager identityLinkEntityManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
            List<IdentityLinkEntity> identityLinkEntities = identityLinkEntityManager
                .findIdentityLinksBySubScopeIdAndType(planItemInstanceEntity.getId(), ScopeTypes.PLAN_ITEM);
            for (IdentityLinkEntity identityLinkEntity : identityLinkEntities) {
                identityLinkEntityManager.delete(identityLinkEntity);
            }
        }
        
        getDataManager().delete(planItemInstanceEntity);
    }

    protected CaseInstanceEntityManager getCaseInstanceEntityManager() {
        return engineConfiguration.getCaseInstanceEntityManager();
    }
    
}

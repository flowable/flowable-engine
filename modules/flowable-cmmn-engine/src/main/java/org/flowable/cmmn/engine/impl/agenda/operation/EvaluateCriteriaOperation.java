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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.converter.util.CriterionUtil;
import org.flowable.cmmn.converter.util.PlanItemUtil;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.listener.PlanItemLifeCycleListenerUtil;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CountingPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.EntityWithSentryPartInstances;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CaseInstanceUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.CompletionEvaluationResult;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceContainerUtil;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.SignalEventListener;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class EvaluateCriteriaOperation extends AbstractCaseInstanceOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateCriteriaOperation.class);

    protected PlanItemLifeCycleEvent planItemLifeCycleEvent;

    // only the last evaluation planned on the agenda operation will have this true
    protected boolean evaluateCaseInstanceCompleted;

    public EvaluateCriteriaOperation(CommandContext commandContext, String caseInstanceEntityId) {
        super(commandContext, caseInstanceEntityId, null);
    }

    public EvaluateCriteriaOperation(CommandContext commandContext, String caseInstanceEntityId, PlanItemLifeCycleEvent planItemLifeCycleEvent) {
        super(commandContext, caseInstanceEntityId, null);
        this.planItemLifeCycleEvent = planItemLifeCycleEvent;
    }

    @Override
    public void run() {
        super.run();

        if (caseInstanceEntity.isDeleted()) {
            return;
        }

        // when evaluating an exit sentry, we take the optional exit event type and exit type into account as well, when terminating the case instance
        Criterion satisfiedExitCriterion = evaluateExitCriteria(caseInstanceEntity, getPlanModel(caseInstanceEntity));
        if (satisfiedExitCriterion != null) {
            // propagate the exit event type and exit type, if provided with the exit sentry / criterion
            CommandContextUtil.getAgenda(commandContext).planTerminateCaseInstanceOperation(caseInstanceEntity.getId(), satisfiedExitCriterion.getId(),
                satisfiedExitCriterion.getExitType(), satisfiedExitCriterion.getExitEventType());

        } else {
            boolean criteriaChangeOrActiveChildren = evaluatePlanItemsCriteria(caseInstanceEntity);
            if (evaluateCaseInstanceCompleted
                    && evaluatePlanModelComplete()
                    && !criteriaChangeOrActiveChildren
                    && !CaseInstanceState.END_STATES.contains(caseInstanceEntity.getState())){
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No active plan items found for plan model, completing case instance");
                }
                CommandContextUtil.getAgenda(commandContext).planCompleteCaseInstanceOperation(caseInstanceEntity);
            }

        }
    }

    /**
     * Evaluates the entry/exit criteria for the given plan item instances
     * and plans new operations when its criteria are satisfied.
     * <p>
     * Returns true if any (part of a) sentry has fired (and didn't fire before)
     * or if any of the passed plan items are still active.
     * <p>
     * Returns false if no sentry changes happened and none of the passed plan item instances are active.
     * This means that the parent of these plan item instances also now can change its state.
     */
    protected boolean evaluatePlanItemsCriteria(PlanItemInstanceContainer planItemInstanceContainer) {
        List<PlanItemInstanceEntity> planItemInstances = planItemInstanceContainer.getChildPlanItemInstances();

        int activeChildren = 0;
        boolean criteriaChanged = false;
        
        // Need to store new child plan item instances in a list until the loop is done, to avoid concurrentmodifications
        List<PlanItemInstanceEntity> newChildPlanItemInstances = null;

        // Check the existing plan item instances: this means the plan items that have been created and became available.
        // This does not include the plan items which haven't been created (for example because they're part of a stage which isn't active yet).
        for (int planItemInstanceIndex = 0; planItemInstanceIndex < planItemInstances.size(); planItemInstanceIndex++) {

            PlanItemInstanceEntity planItemInstanceEntity = planItemInstances.get(planItemInstanceIndex);
            PlanItem planItem = planItemInstanceEntity.getPlanItem();
            String state = planItemInstanceEntity.getState();

            if (PlanItemInstanceState.EVALUATE_ENTRY_CRITERIA_STATES.contains(state)) {
                
                Criterion satisfiedEntryCriterion = evaluateEntryCriteria(planItemInstanceEntity, planItem);
                if (planItem.getEntryCriteria().isEmpty() || satisfiedEntryCriterion != null) {
                    boolean activatePlanItemInstance = true;
                    if (!planItem.getEntryCriteria().isEmpty() && ExpressionUtil.hasRepetitionRule(planItemInstanceEntity)) {
                        boolean isRepeating = ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstanceEntity);
                        if (isRepeating) {

                            PlanItemInstanceEntity childPlanItemInstanceEntity = createPlanItemInstanceDuplicateForRepetition(planItemInstanceEntity);

                            if (newChildPlanItemInstances == null) {
                                newChildPlanItemInstances = new ArrayList<>(1);
                            }
                            newChildPlanItemInstances.add(childPlanItemInstanceEntity);

                        } else {
                            activatePlanItemInstance = false;
                        }
                    }

                    if (planItem.getPlanItemDefinition() instanceof EventListener && !(planItem.getPlanItemDefinition() instanceof SignalEventListener)) {
                        activatePlanItemInstance = false; // event listeners occur, they don't become active
                    }

                    if (activatePlanItemInstance) {
                        criteriaChanged = true;
                        CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(planItemInstanceEntity,
                            satisfiedEntryCriterion != null ? satisfiedEntryCriterion.getId() : null);
                    }

                }

            }
            
            if (!PlanItemInstanceState.END_STATES.contains(state)) {
                
                Criterion satisfiedExitCriterion = evaluateExitCriteria(planItemInstanceEntity, planItem);
                if (satisfiedExitCriterion != null) {
                    criteriaChanged = true;

                    // if we have a satisfied exit sentry, we also pass on its optional exit event type and exit type which has an effect on how the exit
                    // sentry gets executed and if the plan item is terminated (might transition using the complete event and be left in completion or by
                    // default, will transition using exit and be left as terminated
                    CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(planItemInstanceEntity, satisfiedExitCriterion.getId(),
                        satisfiedExitCriterion.getExitType(), satisfiedExitCriterion.getExitEventType());

                } else if (planItem.getPlanItemDefinition() instanceof Stage) {

                    if (PlanItemInstanceState.ACTIVE.equals(state)) {
                        boolean criteriaChangeOrActiveChildrenForStage = evaluatePlanItemsCriteria(planItemInstanceEntity);
                        if (criteriaChangeOrActiveChildrenForStage) {
                            criteriaChanged = true;
                            planItemInstanceEntity.setCompletable(false); // an active child = stage cannot be completed anymore
                        } else {
                            Stage stage = (Stage) planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
                            if (isStageCompletable(planItemInstanceEntity, stage)) {
                                criteriaChanged = true;
                                CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
                            }
                        }
                    }
                } else if (PlanItemInstanceState.ACTIVE.equals(state)) {
                    if (planItem.getItemControl() != null && planItem.getItemControl().getParentCompletionRule() != null) {
                        ParentCompletionRule parentCompletionRule = planItem.getItemControl().getParentCompletionRule();
                        if (ParentCompletionRule.IGNORE.equals(parentCompletionRule.getType())) {
                            continue;
                        }
                    }
                    
                    activeChildren++;
                }
            }

            if (planItemInstanceEntity.getState() == null) {
                // plan item is still being created
                criteriaChanged = true;
            }
        }

        // There are potentially plan items with an 'available condition' that haven't been created before
        if (evaluatePlanItemsWithAvailableCondition(planItemInstanceContainer)) {
            criteriaChanged = true;
        }

        // The direct child plan item instance have been checked.
        // However, the event which triggered this evaluation could also impact cross border dependencies.
        evaluateDependentPlanItems();

        // After the loop, the newly created plan item instances can be added
        if (newChildPlanItemInstances != null) {
            for (PlanItemInstanceEntity newChildPlanItemInstance : newChildPlanItemInstances) {
                planItemInstanceContainer.getChildPlanItemInstances().add(newChildPlanItemInstance);
            }
        }

        return criteriaChanged || activeChildren > 0;
    }

    protected Criterion evaluateEntryCriteria(PlanItemInstanceEntity planItemInstanceEntity, PlanItem planItem) {
        List<Criterion> criteria = planItem.getEntryCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            return evaluateCriteria(planItemInstanceEntity, criteria);
        }
        return null;
    }

    // EntityWithSentryPartInstances -> can be used for both case instance and plan item instance
    protected Criterion evaluateExitCriteria(EntityWithSentryPartInstances entityWithSentryPartInstances, HasExitCriteria hasExitCriteria) {
        List<Criterion> criteria = hasExitCriteria.getExitCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            return evaluateCriteria(entityWithSentryPartInstances, criteria);
        }
        return null;
    }

    /**
     * @return Returns the criterion that is satisfied. If none is satisfied, null is returned.
     */
    protected Criterion evaluateCriteria(EntityWithSentryPartInstances entityWithSentryPartInstances, List<Criterion> criteria) {
        for (Criterion criterion : criteria) {

            Sentry sentry = criterion.getSentry();

            // There can be zero or more on parts and zero or one if part.
            // All defined parts need to be satisfied for the sentry to trigger.

            if (sentry.getOnParts().size() == 1 && sentry.getSentryIfPart() == null) { // Only one one part and no if part: no need to fetch the previously satisfied onparts
                if (planItemLifeCycleEvent != null) {
                    SentryOnPart sentryOnPart = sentry.getOnParts().get(0);
                    if (sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("{}: single onPart matches life cycle event: [{}]", criterion, planItemLifeCycleEvent);
                        }

                        return criterion;
                    }
                }

            } else if (sentry.getOnParts().isEmpty() && sentry.getSentryIfPart() != null) { // Only an if part: simply evaluate the if part
                if (evaluateSentryIfPart(sentry, entityWithSentryPartInstances)) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("{}: single ifPart has evaluated to true", criterion);
                    }

                    return criterion;
                }
                
            } else {

                boolean isDefaultTriggerMode = sentry.isDefaultTriggerMode();

                boolean sentryIfPartSatisfied = false;
                Set<String> satisfiedSentryOnPartIds = new HashSet<>(1);

                // Go through the previously satisfied sentry parts and see if the ifPart was already satisfied
                // and collect the ids of all previously satisfied onParts
                for (SentryPartInstanceEntity sentryPartInstanceEntity : entityWithSentryPartInstances.getSatisfiedSentryPartInstances()) {
                    if (sentryPartInstanceEntity.getOnPartId() != null) {
                        satisfiedSentryOnPartIds.add(sentryPartInstanceEntity.getOnPartId());
                    } else if (sentryPartInstanceEntity.getIfPartId() != null
                        && sentryPartInstanceEntity.getIfPartId().equals(sentry.getSentryIfPart().getId())) {
                        sentryIfPartSatisfied = true;
                    }
                }

                // Verify if the onParts which are not yet satisfied, become satisifed due to the new event
                for (SentryOnPart sentryOnPart : sentry.getOnParts()) {
                    if (!satisfiedSentryOnPartIds.contains(sentryOnPart.getId())) {
                        if (planItemLifeCycleEvent != null && sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {

                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("{}: onPart matches life cycle event [{}]", criterion, planItemLifeCycleEvent);
                            }

                            createSentryPartInstanceEntity(entityWithSentryPartInstances, sentry, sentryOnPart, null);
                            satisfiedSentryOnPartIds.add(sentryOnPart.getId());
                        }
                    }
                }

                boolean allOnPartsSatisfied = (satisfiedSentryOnPartIds.size() == sentry.getOnParts().size());

                if (allOnPartsSatisfied && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{}: all onParts have been satisfied", criterion);
                }

                // Evaluate the if part of the sentry:
                // In the onEvent triggerMode all onParts need to be satisfied before the if is evaluated
                if (sentry.getSentryIfPart() != null && !sentryIfPartSatisfied
                        && (isDefaultTriggerMode || (sentry.isOnEventTriggerMode() && allOnPartsSatisfied) )) {

                    if (evaluateSentryIfPart(sentry, entityWithSentryPartInstances)) {

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("{}: ifPart evaluates to true", criterion);
                        }

                        createSentryPartInstanceEntity(entityWithSentryPartInstances, sentry, null, sentry.getSentryIfPart());
                        sentryIfPartSatisfied = true;
                    }

                }

                if (allOnPartsSatisfied && (sentryIfPartSatisfied || sentry.getSentryIfPart() == null)) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("{}: all onParts and ifParts are satisfied", criterion);
                    }

                    return criterion;
                }
            }
        }

        return null;
    }

    public boolean sentryOnPartMatchesCurrentLifeCycleEvent(SentryOnPart sentryOnPart) {
        return planItemLifeCycleEvent.getPlanItem().getId().equals(sentryOnPart.getSourceRef())
                && planItemLifeCycleEvent.getTransition().equals(sentryOnPart.getStandardEvent());
    }

    protected SentryPartInstanceEntity createSentryPartInstanceEntity(EntityWithSentryPartInstances entityWithSentryPartInstances, Sentry sentry,
            SentryOnPart sentryOnPart, SentryIfPart sentryIfPart) {

        SentryPartInstanceEntityManager sentryPartInstanceEntityManager = CommandContextUtil.getSentryPartInstanceEntityManager(commandContext);
        SentryPartInstanceEntity sentryPartInstanceEntity = sentryPartInstanceEntityManager.create();
        sentryPartInstanceEntity.setTimeStamp(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        
        if (sentryOnPart != null) {
            sentryPartInstanceEntity.setOnPartId(sentryOnPart.getId());
        } else if (sentryIfPart != null) {
            sentryPartInstanceEntity.setIfPartId(sentryIfPart.getId());
        }

        if (entityWithSentryPartInstances instanceof CaseInstanceEntity) {
            sentryPartInstanceEntity.setCaseInstanceId(((CaseInstanceEntity) entityWithSentryPartInstances).getId());
            sentryPartInstanceEntity.setCaseDefinitionId(((CaseInstanceEntity) entityWithSentryPartInstances).getCaseDefinitionId());
        } else if (entityWithSentryPartInstances instanceof PlanItemInstanceEntity) {
            PlanItemInstanceEntity planItemInstanceEntity = (PlanItemInstanceEntity) entityWithSentryPartInstances;
            sentryPartInstanceEntity.setCaseInstanceId(planItemInstanceEntity.getCaseInstanceId());
            sentryPartInstanceEntity.setCaseDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
            sentryPartInstanceEntity.setPlanItemInstanceId(planItemInstanceEntity.getId());
            
            // Update relationship count
            if (sentry.isDefaultTriggerMode() && entityWithSentryPartInstances instanceof CountingPlanItemInstanceEntity) {
                CountingPlanItemInstanceEntity countingPlanItemInstanceEntity = (CountingPlanItemInstanceEntity) planItemInstanceEntity;
                countingPlanItemInstanceEntity.setSentryPartInstanceCount(countingPlanItemInstanceEntity.getSentryPartInstanceCount() + 1);
            }
        }

        // In the default triggerMode satisfied parts are remembered for subsequent evaluation cycles.
        // In the onEvent triggerMode, they are stored for the duration of the transaction (which is the same as one evaluation cycle) but not inserted.
        if (sentry.isDefaultTriggerMode()) {
            sentryPartInstanceEntityManager.insert(sentryPartInstanceEntity);
        }

        entityWithSentryPartInstances.getSatisfiedSentryPartInstances().add(sentryPartInstanceEntity);
        return sentryPartInstanceEntity;
    }
    
    protected boolean evaluateSentryIfPart(Sentry sentry, VariableContainer variableContainer) {
        Expression conditionExpression = CommandContextUtil.getExpressionManager(commandContext).createExpression(sentry.getSentryIfPart().getCondition());
        Object result = conditionExpression.getValue(variableContainer);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Evaluation of sentry if condition {} results in '{}'", sentry.getSentryIfPart().getCondition(), result);
        }

        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return false;
    }

    protected boolean isStageCompletable(PlanItemInstanceEntity stagePlanItemInstanceEntity, Stage stage) {
        boolean autoComplete = ExpressionUtil.evaluateAutoComplete(commandContext, stagePlanItemInstanceEntity, stage);
        CompletionEvaluationResult completionEvaluationResult = PlanItemInstanceContainerUtil
            .shouldPlanItemContainerComplete(commandContext, stagePlanItemInstanceEntity, autoComplete);

        if (completionEvaluationResult.isCompletable()) {
            stagePlanItemInstanceEntity.setCompletable(true);
        }

        return completionEvaluationResult.shouldBeCompleted();
    }

    protected boolean evaluatePlanModelComplete() {
        boolean isAutoComplete = ExpressionUtil.evaluateAutoComplete(commandContext, caseInstanceEntity,
            CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel());

        CompletionEvaluationResult completionEvaluationResult = PlanItemInstanceContainerUtil
            .shouldPlanItemContainerComplete(commandContext, caseInstanceEntity, isAutoComplete);

        // update the completion state on the case and check, if it was changed
        boolean previousCompletableState = caseInstanceEntity.isCompletable();
        caseInstanceEntity.setCompletable(completionEvaluationResult.isCompletable());

        // When the case entity changes, the plan items with an available condition can be become ready for creation
        if (previousCompletableState != caseInstanceEntity.isCompletable()) {
            boolean planItemInstancesChanged = evaluatePlanItemsWithAvailableCondition(caseInstanceEntity);
            if (planItemInstancesChanged) {
                // If new plan items have changed, this could lead to changing of the fact that the case instance entity completable state is changed again
                completionEvaluationResult = PlanItemInstanceContainerUtil.shouldPlanItemContainerComplete(commandContext, caseInstanceEntity, isAutoComplete);
                if (completionEvaluationResult.isCompletable() != caseInstanceEntity.isCompletable()) {
                    caseInstanceEntity.setCompletable(completionEvaluationResult.isCompletable());
                }
            }
        }

        return completionEvaluationResult.shouldBeCompleted();
    }

    protected boolean evaluatePlanItemsWithAvailableCondition(PlanItemInstanceContainer planItemInstanceContainer) {
        if (!planItemInstanceContainer.getPlanItems().isEmpty()) {

            // Find event listeners with an available condition to become available
            List<PlanItemInstanceEntity> planItemInstanceToInitiate = findChangedEventListenerInstances(planItemInstanceContainer,
                PlanItemInstanceState.UNAVAILABLE, true);
            if (!planItemInstanceToInitiate.isEmpty()) {
                for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstanceToInitiate) {
                    CommandContextUtil.getAgenda(commandContext).planInitiatePlanItemInstanceOperation(planItemInstanceEntity);
                }
                return true;
            }

            // Find event listeners with an available condition to become unavailable
            List<PlanItemInstanceEntity> planItemInstanceToDismiss = findChangedEventListenerInstances(planItemInstanceContainer,
                PlanItemInstanceState.AVAILABLE, false);
            if (!planItemInstanceToDismiss.isEmpty()) {
                for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstanceToDismiss) {
                    CommandContextUtil.getAgenda(commandContext).planDismissPlanItemInstanceOperation(planItemInstanceEntity);
                }
                return true;
            }

        }

        return false;
    }

    protected List<PlanItemInstanceEntity> findChangedEventListenerInstances(PlanItemInstanceContainer planItemInstanceContainer, String state, boolean conditionValueToChange) {
        return planItemInstanceContainer.getChildPlanItemInstances().stream()
                    .filter(planItemInstance -> state.equals(planItemInstance.getState())
                        && isEventListenerWithAvailableCondition(planItemInstance.getPlanItem())
                        && conditionValueToChange == evaluateAvailableCondition(commandContext, planItemInstance))
                    .collect(Collectors.toList());
    }

    protected boolean evaluateAvailableCondition(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        if (isEventListenerWithAvailableCondition(planItem)) {
            EventListener eventListener = (EventListener) planItem.getPlanItemDefinition();
            if (StringUtils.isNotEmpty(eventListener.getAvailableConditionExpression())) {
                Expression expression = CommandContextUtil.getExpressionManager(commandContext).createExpression(eventListener.getAvailableConditionExpression());
                Object result = expression.getValue(planItemInstanceEntity);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Evaluation of available condition {} results in '{}'", eventListener.getAvailableConditionExpression(), result);
                }

                if (result instanceof Boolean) {
                    return (Boolean) result;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    protected void evaluateDependentPlanItems() {

        if (planItemLifeCycleEvent == null) {
            return;
        }

        // The plan item instances that have passed or are at the available state have been evaluated.
        // The plan items that have not yet been created but have an entry sentry that crosses the outer stage border
        // are evaluated to see if they need to become available(see table 8.7 in the CMMN 1.1 spec).

        List<PlanItem> entryDependentPlanItems = planItemLifeCycleEvent.getPlanItem().getEntryDependentPlanItems();
        for (PlanItem entryDependentPlanItem : entryDependentPlanItems) {
            // Only needed for sentries that cross the outer stage border
            if (!planItemsShareDirectParentStage(entryDependentPlanItem, planItemLifeCycleEvent.getPlanItem())
                    && CriterionUtil.planItemHasOneEntryCriterionDependingOnPlanItem(entryDependentPlanItem, planItemLifeCycleEvent.getPlanItem(), planItemLifeCycleEvent.getTransition())) {

                PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
                List<PlanItemInstanceEntity> childPlanItemInstances = CaseInstanceUtil.findChildPlanItemInstances(caseInstanceEntity, entryDependentPlanItem);
                List<PlanItemInstanceEntity> potentialTerminatedPlanItemInstances = planItemInstanceEntityManager
                    .findByCaseInstanceIdAndPlanItemId(caseInstanceEntity.getId(), entryDependentPlanItem.getId());

                if (childPlanItemInstances.isEmpty() // runtime state
                        && (potentialTerminatedPlanItemInstances.isEmpty()
                                // (terminated state) the plan item instance should not have been created anytime before
                            || (ExpressionUtil.hasRepetitionRule(entryDependentPlanItem) && ExpressionUtil.evaluateRepetitionRule(commandContext, caseInstanceEntity, entryDependentPlanItem.getItemControl().getRepetitionRule().getCondition())))) {

                    // If the sentry satisfied, the plan item becomes active and all parent stages that are not yet activate are made active
                    Criterion satisfiedCriterion = evaluateDependentPlanItemEntryCriteria(entryDependentPlanItem);
                    if (satisfiedCriterion != null) {

                        // Creating plan item instances for all parent stages that do not exist yet
                        List<PlanItem> parentPlanItems = PlanItemUtil.getAllParentPlanItems(entryDependentPlanItem);
                        Map<String, List<PlanItemInstanceEntity>> existingPlanItemInstancesMap = CaseInstanceUtil
                            .findChildPlanItemInstancesMap(caseInstanceEntity, parentPlanItems);
                        List<PlanItemInstanceEntity> parentPlanItemInstancesToActivate = new ArrayList<>();

                        PlanItemInstance previousParentPlanItemInstance = null;
                        for (int i = parentPlanItems.size() - 1; i >= 0; i--) { // going from outermost to direct parent

                            PlanItem parentPlanItem = parentPlanItems.get(i);
                            List<PlanItemInstanceEntity> parentPlanItemInstances = existingPlanItemInstancesMap.get(parentPlanItem.getId());

                            if (parentPlanItemInstances == null || parentPlanItemInstances.isEmpty()) {
                                PlanItemInstanceEntity parentPlanItemInstance = planItemInstanceEntityManager.createChildPlanItemInstance(
                                    parentPlanItem,
                                    caseInstanceEntity.getCaseDefinitionId(),
                                    caseInstanceEntity.getId(),
                                    previousParentPlanItemInstance != null ? previousParentPlanItemInstance.getId() : null,
                                    caseInstanceEntity.getTenantId(),
                                    true);
                                parentPlanItemInstancesToActivate.add(parentPlanItemInstance);

                                previousParentPlanItemInstance = parentPlanItemInstance;

                            } else {

                                for (PlanItemInstanceEntity parentPlanItemInstance : parentPlanItemInstances) {
                                    if (!PlanItemInstanceState.ACTIVE.equals(parentPlanItemInstance.getState())) {
                                        parentPlanItemInstancesToActivate.add(parentPlanItemInstance);
                                    }
                                }

                                previousParentPlanItemInstance = parentPlanItemInstances.get(0); // in case there are multiple parent plan item instances, select the first

                            }

                        }

                        // Creating plan item instance for the activated plan item
                        PlanItemInstanceEntity entryDependentPlanItemInstance = planItemInstanceEntityManager.createChildPlanItemInstance(
                            entryDependentPlanItem,
                            caseInstanceEntity.getCaseDefinitionId(),
                            caseInstanceEntity.getId(),
                            previousParentPlanItemInstance != null ? previousParentPlanItemInstance.getId() : null,
                            // previous is closest parent stage plan item instance
                            caseInstanceEntity.getTenantId(),
                            true);
                        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(entryDependentPlanItemInstance);

                        // Special care needed in case the plan item instance is repeating
                        if (!entryDependentPlanItem.getEntryCriteria().isEmpty() && ExpressionUtil.hasRepetitionRule(entryDependentPlanItemInstance)) {
                            if (ExpressionUtil.evaluateRepetitionRule(commandContext, entryDependentPlanItemInstance)) {
                                createPlanItemInstanceDuplicateForRepetition(entryDependentPlanItemInstance);
                            }
                        }

                        // All plan item instances are created. Now activate them.
                        CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(entryDependentPlanItemInstance, satisfiedCriterion.getId());
                        for (int i = parentPlanItemInstancesToActivate.size() - 1; i >= 0; i--) {
                            PlanItemInstanceEntity parentPlanItemInstance = parentPlanItemInstancesToActivate.get(i);
                            if (parentPlanItemInstance == null) { // newly created one
                                CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(parentPlanItemInstance);
                            }
                            CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(parentPlanItemInstance, null); // null -> no sentry satisfied, activation is because of child activation
                        }
                    }

                }
            }
        }
    }

    protected Criterion evaluateDependentPlanItemEntryCriteria(PlanItem entryDependentPlanItem) {
        List<Criterion> entryCriteria = entryDependentPlanItem.getEntryCriteria();
        if (!entryCriteria.isEmpty()) {

            // According to the spec, only the sentries that actually reference the planitem of which the event happens should be evaluated
            List<Criterion> matchingCriteria = entryCriteria.stream()
                .filter(criterion -> CriterionUtil
                    .criterionHasOnPartDependingOnPlanItem(criterion, planItemLifeCycleEvent.getPlanItem(), planItemLifeCycleEvent.getTransition()))
                .collect(Collectors.toList());

            if (!matchingCriteria.isEmpty()) {
                return evaluateCriteria(caseInstanceEntity, matchingCriteria);// Resolved against case entity as there's no plan item instance yet
            }
        }

        return null;
    }

    protected PlanItemInstanceEntity createPlanItemInstanceDuplicateForRepetition(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemInstanceEntity childPlanItemInstanceEntity = copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, false);

        String oldState = childPlanItemInstanceEntity.getState();
        String newState = PlanItemInstanceState.WAITING_FOR_REPETITION;
        childPlanItemInstanceEntity.setState(newState);
        PlanItemLifeCycleListenerUtil.callLifecycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

        // createPlanItemInstance operations will also sync planItemInstance history
        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(childPlanItemInstanceEntity);
        return childPlanItemInstanceEntity;
    }

    protected boolean planItemsShareDirectParentStage(PlanItem planItemOne, PlanItem planItemTwo) {
        Stage parentStage = planItemOne.getParentStage();
        return parentStage.findPlanItemInPlanFragmentOrDownwards(planItemTwo.getId()) != null;
    }

    public PlanItemLifeCycleEvent getPlanItemLifeCycleEvent() {
        return planItemLifeCycleEvent;
    }

    public void setPlanItemLifeCycleEvent(PlanItemLifeCycleEvent planItemLifeCycleEvent) {
        this.planItemLifeCycleEvent = planItemLifeCycleEvent;
    }

    public boolean isEvaluateCaseInstanceCompleted() {
        return evaluateCaseInstanceCompleted;
    }

    public void setEvaluateCaseInstanceCompleted(boolean evaluateCaseInstanceCompleted) {
        this.evaluateCaseInstanceCompleted = evaluateCaseInstanceCompleted;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Evaluate Criteria] case instance ");
        if (caseInstanceEntity != null) {
            stringBuilder.append(caseInstanceEntity.getId());
        } else {
            stringBuilder.append(caseInstanceEntityId);
        }

        if (planItemLifeCycleEvent != null) {
            stringBuilder.append(" with transition '").append(planItemLifeCycleEvent.getTransition()).append("' having fired");
            if (planItemLifeCycleEvent.getPlanItem() != null) {
                stringBuilder.append(" for ").append(planItemLifeCycleEvent.getPlanItem());
            }
        }

        return stringBuilder.toString();
    }
    
}

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.converter.util.CriterionUtil;
import org.flowable.cmmn.converter.util.PlanItemUtil;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.PlanItemEvaluationResult;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
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
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.CompletionEvaluationResult;
import org.flowable.cmmn.engine.impl.util.ExpressionUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceContainerUtil;
import org.flowable.cmmn.engine.impl.util.PlanItemInstanceUtil;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for operations using criteria evaluation like entry / exit sentries, repetition rule and parent completion rule.
 *
 * @author Micha Kiener
 */
public abstract class AbstractEvaluationCriteriaOperation extends AbstractCaseInstanceOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEvaluationCriteriaOperation.class);

    protected PlanItemLifeCycleEvent planItemLifeCycleEvent;

    /** only the last evaluation planned on the agenda operation will have this true. */
    protected boolean evaluateStagesAndCaseInstanceCompletion;

    public AbstractEvaluationCriteriaOperation(CommandContext commandContext, String caseInstanceId, CaseInstanceEntity caseInstanceEntity, PlanItemLifeCycleEvent planItemLifeCycleEvent) {
        super(commandContext, caseInstanceId, caseInstanceEntity);
        this.planItemLifeCycleEvent = planItemLifeCycleEvent;
    }

    /**
     * Evaluates the given plan item for activation by looking at its entry criteria, repetition rule and whether the plan item is a special one like an
     * event listener (they occur and will never actually be active).
     *
     * @param planItemInstanceEntity the plan item instance to evaluate
     * @param planItemInstanceContainer the parent container of the plan item instance
     * @param evaluationResult the object holding evaluation results, will be modified inside this method with gained information
     */
    public void evaluateForActivation(PlanItemInstanceEntity planItemInstanceEntity, PlanItemInstanceContainer planItemInstanceContainer,
        PlanItemEvaluationResult evaluationResult) {
        PlanItem planItem = planItemInstanceEntity.getPlanItem();

        // evaluate the entry criteria of the plan item and return it, if at least one was satisfied
        Criterion satisfiedEntryCriterion = evaluateEntryCriteria(planItemInstanceEntity, planItem);
        if (planItem.getEntryCriteria().isEmpty() || satisfiedEntryCriterion != null) {
            // entry criteria is satisfied for this plan item instance, so we can basically activate it, but we need to check further options like
            // repetition

            // evaluate the repetition rule, if any, will also create new child plan items and add them to the result, if necessary
            boolean activatePlanItemInstance = evaluateRepetitionRule(planItemInstanceEntity, satisfiedEntryCriterion, planItemInstanceContainer,
                evaluationResult);

            if (planItem.getPlanItemDefinition() instanceof EventListener) {
                activatePlanItemInstance = false; // event listeners occur, they don't become active
            }

            // if we need to activate the plan item, mark the result as some criteria changed and plan the activation of the plan item by adding
            // this as an operation to the agenda
            if (activatePlanItemInstance) {
                evaluationResult.markCriteriaChanged();
                CommandContextUtil.getAgenda(commandContext)
                    .planActivatePlanItemInstanceOperation(planItemInstanceEntity, satisfiedEntryCriterion != null ? satisfiedEntryCriterion.getId() : null);
            }
        }
    }

    /**
     * Evaluates an optional repetition rule on the given plan item and handles it. This might also include handling of a repetition condition or repetition
     * based on a collection variable with optional local item and item index variables to be set on the newly created plan item instances for repetition.
     *
     * @param planItemInstanceEntity the plan item instance to test for a repetition rule
     * @param satisfiedEntryCriterion the optional, satisfied entry criterion activating the plan item, might be null
     * @param planItemInstanceContainer the parent container of the given plan item
     * @param evaluationResult the evaluation result used to collect information during the evaluation of a list of plan items, will be modified inside this
     *          method to reflect gained information about further evaluation as well as any newly created plan item instances for repetition
     * @return true, if the plan item must be activated, false otherwise
     */
    protected boolean evaluateRepetitionRule(PlanItemInstanceEntity planItemInstanceEntity, Criterion satisfiedEntryCriterion,
        PlanItemInstanceContainer planItemInstanceContainer, PlanItemEvaluationResult evaluationResult) {

        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        boolean activatePlanItemInstance = true;

        // check for a repetition rule on the plan item
        if (ExpressionUtil.hasRepetitionRule(planItemInstanceEntity)) {
            boolean noEntryCriteria = planItem.getEntryCriteria().isEmpty();

            // first check, if we run on a collection variable for repetition and if so, we ignore the max instance count and any other repetition
            // condition and just use the collection to create plan item instances accordingly
            if (ExpressionUtil.hasRepetitionOnCollection(planItemInstanceEntity)) {
                // the plan item should be repeated based on a collection variable
                // evaluate the variable content and check, if we need to start creating instances accordingly
                Iterable<Object> collection = ExpressionUtil.evaluateRepetitionCollectionVariableValue(commandContext, planItemInstanceEntity);

                // if the collection is null (meaning it is not yet available) and we don't have any on-part criteria (e.g an on-part or even combined with
                // an if-part), we don't handle the repetition yet, but wait for its collection to become available
                // but if we have an on-part, we always handle the collection, even if it is null or empty
                if (collection == null && !ExpressionUtil.hasOnParts(planItem)) {
                    // keep this plan item in its current state and don't activate it or handle the repetition collection yet as it is not available yet
                    activatePlanItemInstance = false;
                    
                } else {

                    if (collection != null) {
                        RepetitionRule repetitionRule = ExpressionUtil.getRepetitionRule(planItemInstanceEntity);
                        int index = 0;
                        for (Object item : collection) {
                            // create and activate a new plan item instance for each item in the collection
                            PlanItemInstanceEntity childPlanItemInstanceEntity = createPlanItemInstanceDuplicateForCollectionRepetition(
                                repetitionRule, planItemInstanceEntity, null, item, index++);

                            evaluationResult.addChildPlanItemInstance(childPlanItemInstanceEntity);
                        }
                    }

                    // we handled the collection, now we need to make sure that evaluation does not trigger again as it might get evaluated again
                    // before it is terminated or made available again, so we remove the sentry related data of the plan item
                    CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).deleteSentryRelatedData(planItemInstanceEntity.getId());

                    // don't activate this plan item instance, but keep it in available or waiting for repetition state for the next on-part triggering,
                    // if there is an on-part, otherwise we will directly terminate it without having activated it, it was only used to wait in available
                    // state until all criteria was satisfied, including having the collection variable
                    activatePlanItemInstance = false;

                    // if there is an on-part, we keep the current plan item instance for further triggering the on-part and evaluating the collection again
                    // otherwise we terminate the plan item
                    if (!ExpressionUtil.hasOnParts(planItem)) {
                        // if there is no on-part, we don't need this plan item instance anymore, so terminate it
                        CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity, null, null);
                    }
                }
                
            } else if (!noEntryCriteria) {
                // check the plan item to be repeating by evaluating its repetition rule
                if (ExpressionUtil.evaluateRepetitionRule(commandContext, planItemInstanceEntity, planItemInstanceContainer)) {
                    // create a new duplicated plan item instance in waiting for repetition as this one is becoming active
                    evaluationResult.addChildPlanItemInstance(createPlanItemInstanceDuplicateForRepetition(planItemInstanceEntity));
                } else {
                    // the repetition rule does not evaluate to true, so we keep this instance in its current state and don't activate it
                    activatePlanItemInstance = false;
                }
            }
        }

        return activatePlanItemInstance;
    }

    /**
     * Evaluates the given plan item for completion or termination by looking at its state and exit criteria. If it is a stage, it will evaluate its child
     * plan items as well.
     *
     * @param planItemInstanceEntity the plan item instance to evaluate for completion or termination
     * @param evaluationResult the object holding evaluation results, will be modified inside this method with gained information
     * @return true, if further evaluation should be skipped as the plan item can be ignored for further processing, false otherwise
     */
    public boolean evaluateForCompletion(PlanItemInstanceEntity planItemInstanceEntity, PlanItemEvaluationResult evaluationResult) {
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        String state = planItemInstanceEntity.getState();

        // search and evaluate for exit criteria on the plan item, for at least one satisfied exit criterion
        Criterion satisfiedExitCriterion = evaluateExitCriteria(planItemInstanceEntity, planItem);
        if (satisfiedExitCriterion != null) {
            evaluationResult.markCriteriaChanged();

            // if we have a satisfied exit sentry, we also pass on its optional exit event type and exit type which has an effect on how the exit
            // sentry gets executed and if the plan item is terminated (might transition using the complete event and be left in completion or by
            // default, will transition using exit and be left as terminated
            CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(planItemInstanceEntity, satisfiedExitCriterion.getId(),
                satisfiedExitCriterion.getExitType(), satisfiedExitCriterion.getExitEventType());

        } else if (planItem.getPlanItemDefinition() instanceof Stage) {

            if (PlanItemInstanceState.ACTIVE.equals(state)) {
                boolean criteriaChangeOrActiveChildrenForStage = evaluatePlanItemsCriteria(planItemInstanceEntity);
                if (criteriaChangeOrActiveChildrenForStage) {
                    evaluationResult.markCriteriaChanged();
                    planItemInstanceEntity.setCompletable(false); // an active child = stage cannot be completed anymore
                } else {
                    Stage stage = (Stage) planItem.getPlanItemDefinition();
                    if (isStageCompletable(planItemInstanceEntity, stage)) {
                        evaluationResult.markCriteriaChanged();
                        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
                    }
                }
            }
        } else if (PlanItemInstanceState.ACTIVE.equals(state)) {
            // check, if the plan item can be ignored for further processing and if so, immediately return
            if (planItem.getItemControl() != null && planItem.getItemControl().getParentCompletionRule() != null) {
                ParentCompletionRule parentCompletionRule = planItem.getItemControl().getParentCompletionRule();

                // first check for the always ignore rule
                if (ParentCompletionRule.IGNORE.equals(parentCompletionRule.getType())) {
                    return true;
                }

                // now check, if we can ignore it because it was completed before
                if (ParentCompletionRule.IGNORE_AFTER_FIRST_COMPLETION.equals(parentCompletionRule.getType())) {
                    if (evaluationResult.hasCompletedPlanItemInstance(planItemInstanceEntity)) {
                        return true;
                    }
                }
            }

            evaluationResult.increaseActiveChildren();
        }
        return false;
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
        
        // needed because when doing case instance migration the child plan item instances can be null
        if (planItemInstances == null && planItemInstanceContainer instanceof CaseInstanceEntity) {
            PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
            CaseInstanceEntity caseInstance = (CaseInstanceEntity) planItemInstanceContainer;
            planItemInstances = planItemInstanceEntityManager.findByCaseInstanceId(caseInstance.getId());
            planItemInstanceContainer.setChildPlanItemInstances(planItemInstances);
        }

        // create an evaluation result object, holding all evaluation results as well as a list of newly created child plan items, as to avoid concurrent
        // modification, we add them at the end of the evaluation loop to the parent container
        PlanItemEvaluationResult evaluationResult = new PlanItemEvaluationResult(planItemInstances);

        // Check the existing plan item instances: this means the plan items that have been created and became available.
        // This does not include the plan items which haven't been created (for example because they're part of a stage which isn't active yet).
        for (int planItemInstanceIndex = 0; planItemInstanceIndex < planItemInstances.size(); planItemInstanceIndex++) {

            PlanItemInstanceEntity planItemInstanceEntity = planItemInstances.get(planItemInstanceIndex);
            String state = planItemInstanceEntity.getState();

            // check, if the plan item is in an evaluation state (e.g. available or waiting for repetition) to check for its activation
            if (PlanItemInstanceState.EVALUATE_ENTRY_CRITERIA_STATES.contains(state)) {
                evaluateForActivation(planItemInstanceEntity, planItemInstanceContainer, evaluationResult);
            }

            // check the plan item, if it is not yet in a final end state to see whether it can be completed or terminated
            if (!PlanItemInstanceState.END_STATES.contains(state)) {
                if (evaluateForCompletion(planItemInstanceEntity, evaluationResult)) {
                    continue;
                }
            }

            if (planItemInstanceEntity.getState() == null) {
                // plan item is still being created
                evaluationResult.markCriteriaChanged();
            }
        }

        // There are potentially plan items with an 'available condition' that haven't been created before
        if (evaluatePlanItemsWithAvailableCondition(planItemInstanceContainer)) {
            evaluationResult.markCriteriaChanged();
        }

        // The direct child plan item instance have been checked.
        // However, the event which triggered this evaluation could also impact cross border dependencies.
        evaluateDependentPlanItems();

        // After the loop, the newly created plan item instances can be added
        if (evaluationResult.hasNewChildPlanItemInstances()) {
            for (PlanItemInstanceEntity newChildPlanItemInstance : evaluationResult.getNewChildPlanItemInstances()) {
                planItemInstanceContainer.getChildPlanItemInstances().add(newChildPlanItemInstance);
            }
        }

        return evaluationResult.criteriaChangedOrNewActiveChildren();
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
                                PlanItemInstanceEntity parentPlanItemInstance = planItemInstanceEntityManager
                                    .createPlanItemInstanceEntityBuilder()
                                    .planItem(parentPlanItem)
                                    .caseDefinitionId(caseInstanceEntity.getCaseDefinitionId())
                                    .caseInstanceId(caseInstanceEntity.getId())
                                    .stagePlanItemInstance(previousParentPlanItemInstance)
                                    .tenantId(caseInstanceEntity.getTenantId())
                                    .addToParent(true)
                                    .create();

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
                        PlanItemInstanceEntity entryDependentPlanItemInstance = planItemInstanceEntityManager
                            .createPlanItemInstanceEntityBuilder()
                            .planItem(entryDependentPlanItem)
                            .caseDefinitionId(caseInstanceEntity.getCaseDefinitionId())
                            .caseInstanceId(caseInstanceEntity.getId())
                            .stagePlanItemInstance(previousParentPlanItemInstance)
                            // previous is closest parent stage plan item instance
                            .tenantId(caseInstanceEntity.getTenantId())
                            .addToParent(true)
                            .create();

                        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(entryDependentPlanItemInstance);

                        // Special care needed in case the plan item instance is repeating
                        if (!entryDependentPlanItem.getEntryCriteria().isEmpty() && ExpressionUtil.hasRepetitionRule(entryDependentPlanItemInstance)) {
                            if (ExpressionUtil.evaluateRepetitionRule(commandContext, entryDependentPlanItemInstance, (PlanItemInstanceContainer) null)) {
                                createPlanItemInstanceDuplicateForRepetition(entryDependentPlanItemInstance);
                            }
                        }

                        // All plan item instances are created. Now activate them.
                        CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(entryDependentPlanItemInstance, satisfiedCriterion.getId());
                        for (int i = parentPlanItemInstancesToActivate.size() - 1; i >= 0; i--) {
                            PlanItemInstanceEntity parentPlanItemInstance = parentPlanItemInstancesToActivate.get(i);
                            if (parentPlanItemInstance.getState() == null) { // newly created one
                                CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceOperation(parentPlanItemInstance);
                            }
                            CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(parentPlanItemInstance, null); // null -> no sentry satisfied, activation is because of child activation
                        }
                    }

                }
            }
        }
    }

    protected boolean isStageCompletable(PlanItemInstanceEntity stagePlanItemInstanceEntity, Stage stage) {
        boolean autoComplete = ExpressionUtil.evaluateAutoComplete(commandContext, stagePlanItemInstanceEntity, stage);
        if (!autoComplete || evaluateStagesAndCaseInstanceCompletion) { // auto completion should only be evaluated when children are stable
            CompletionEvaluationResult completionEvaluationResult = PlanItemInstanceContainerUtil
                .shouldPlanItemContainerComplete(commandContext, stagePlanItemInstanceEntity, autoComplete);

            if (completionEvaluationResult.isCompletable()) {
                stagePlanItemInstanceEntity.setCompletable(true);
            }

            return completionEvaluationResult.shouldBeCompleted();
        }
        return false;
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

            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            if (sentry.getOnParts().size() == 1 && sentry.getSentryIfPart() == null) { // Only one on part and no if part: no need to fetch the previously satisfied onparts
                if (planItemLifeCycleEvent != null) {
                    SentryOnPart sentryOnPart = sentry.getOnParts().get(0);
                    if (sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("{}: single onPart matches life cycle event: [{}]", criterion, planItemLifeCycleEvent);
                        }

                        if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                            CmmnLoggingSessionUtil.addEvaluateSentryLoggingData(sentry.getOnParts(), entityWithSentryPartInstances, cmmnEngineConfiguration.getObjectMapper());
                        }

                        return criterion;
                    }
                }

            } else if (sentry.getOnParts().isEmpty() && sentry.getSentryIfPart() != null) { // Only an if part: simply evaluate the if part
                if (evaluateSentryIfPart(entityWithSentryPartInstances, sentry, entityWithSentryPartInstances)) {

                    if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                        CmmnLoggingSessionUtil.addEvaluateSentryLoggingData(sentry.getSentryIfPart(), entityWithSentryPartInstances, cmmnEngineConfiguration.getObjectMapper());
                    }

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

                // Verify if the onParts which are not yet satisfied, become satisfied due to the new event
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

                boolean allOnPartsSatisfied = allOnPartsSatisfied(satisfiedSentryOnPartIds, sentry.getOnParts());

                if (allOnPartsSatisfied && LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{}: all onParts have been satisfied", criterion);
                }

                // Evaluate the if part of the sentry:
                // In the onEvent triggerMode all onParts need to be satisfied before the if is evaluated
                if (sentry.getSentryIfPart() != null && !sentryIfPartSatisfied
                    && (isDefaultTriggerMode || (sentry.isOnEventTriggerMode() && allOnPartsSatisfied) )) {

                    if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                        CmmnLoggingSessionUtil.addEvaluateSentryLoggingData(sentry.getOnParts(), sentry.getSentryIfPart(), 
                                entityWithSentryPartInstances, cmmnEngineConfiguration.getObjectMapper());
                    }

                    if (evaluateSentryIfPart(entityWithSentryPartInstances, sentry, entityWithSentryPartInstances)) {

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

    /**
     * Evaluate, if the sentries on-parts are all satisfied.
     *
     * @param satisfiedSentryOnPartIds the set of satisfied sentry on parts, which might also contain on-parts from other sentries on the same plan item.
     * @param sentryOnParts the list of on-parts of the currently evaluated sentry
     * @return true, if all on parts of the sentry are satisfied, false otherwise
     */
    protected boolean allOnPartsSatisfied(Set<String> satisfiedSentryOnPartIds, List<SentryOnPart> sentryOnParts) {
        if (satisfiedSentryOnPartIds.size() == 0 && sentryOnParts.size() > 0) {
            return false;
        }

        for (SentryOnPart sentryOnPart : sentryOnParts) {
            if (!satisfiedSentryOnPartIds.contains(sentryOnPart.getId())) {
                return false;
            }
        }
        return true;
    }

    public boolean sentryOnPartMatchesCurrentLifeCycleEvent(SentryOnPart sentryOnPart) {
        return planItemLifeCycleEvent.getPlanItem().getId().equals(sentryOnPart.getSourceRef())
            && planItemLifeCycleEvent.getTransition().equals(sentryOnPart.getStandardEvent());
    }

    protected List<PlanItemInstanceEntity> findChangedEventListenerInstances(PlanItemInstanceContainer planItemInstanceContainer, String state, boolean conditionValueToChange) {
        return planItemInstanceContainer.getChildPlanItemInstances().stream()
            .filter(planItemInstance -> state.equals(planItemInstance.getState())
                && isEventListenerWithAvailableCondition(planItemInstance.getPlanItem())
                && conditionValueToChange == evaluateAvailableCondition(commandContext, planItemInstance))
            .collect(Collectors.toList());
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

    protected boolean evaluateSentryIfPart(EntityWithSentryPartInstances entityWithSentryPartInstances, Sentry sentry, VariableContainer variableContainer) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        try { 
            Expression conditionExpression = cmmnEngineConfiguration.getExpressionManager().createExpression(sentry.getSentryIfPart().getCondition());
            Object result = conditionExpression.getValue(variableContainer);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Evaluation of sentry if condition {} for {} results in '{}'", sentry.getSentryIfPart().getCondition(), entityWithSentryPartInstances, result);
            }

            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        } catch (RuntimeException e) {
            if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
                CmmnLoggingSessionUtil.addEvaluateSentryFailedLoggingData(sentry.getSentryIfPart(), e, 
                        entityWithSentryPartInstances, cmmnEngineConfiguration.getObjectMapper());
            }

            throw e;
        }
        return false;
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

    protected boolean planItemsShareDirectParentStage(PlanItem planItemOne, PlanItem planItemTwo) {
        Stage parentStage = planItemOne.getParentStage();
        return parentStage.findPlanItemInPlanFragmentOrDownwards(planItemTwo.getId()) != null;
    }

    protected PlanItemInstanceEntity createPlanItemInstanceDuplicateForRepetition(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemInstanceEntity childPlanItemInstanceEntity = PlanItemInstanceUtil.copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, false, false);

        String oldState = childPlanItemInstanceEntity.getState();
        String newState = PlanItemInstanceState.WAITING_FOR_REPETITION;
        childPlanItemInstanceEntity.setState(newState);
        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
            .executeLifecycleListeners(commandContext, childPlanItemInstanceEntity, oldState, newState);

        // createPlanItemInstance operations will also sync planItemInstance history
        CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(childPlanItemInstanceEntity);
        return childPlanItemInstanceEntity;
    }

    protected PlanItemInstanceEntity createPlanItemInstanceDuplicateForCollectionRepetition(RepetitionRule repetitionRule,
        PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId, Object item, int index) {

        // check, if we need to set local variables as the item or item index
        Map<String, Object> localVariables = new HashMap<>(2);
        if (repetitionRule.hasElementVariable()) {
            localVariables.put(repetitionRule.getElementVariableName(), item);
        }
        if (repetitionRule.hasElementIndexVariable()) {
            localVariables.put(repetitionRule.getElementIndexVariableName(), index);
        }

        PlanItemInstanceEntity childPlanItemInstanceEntity = PlanItemInstanceUtil.copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, localVariables, false, false);

        // record the plan item being created based on the collection, so it gets synchronized to the history as well
        CommandContextUtil.getAgenda(commandContext).planCreateRepeatedPlanItemInstanceOperation(childPlanItemInstanceEntity);

        // The repetition counter is 1 based
        setRepetitionCounter(childPlanItemInstanceEntity, index + 1);

        String oldState = childPlanItemInstanceEntity.getState();
        String newState = PlanItemInstanceState.ACTIVE;
        childPlanItemInstanceEntity.setState(newState);
        CommandContextUtil.getCmmnEngineConfiguration(commandContext).getListenerNotificationHelper()
            .executeLifecycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

        // createPlanItemInstance operations will also sync planItemInstance history
        CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(childPlanItemInstanceEntity, entryCriterionId);
        return childPlanItemInstanceEntity;
    }

    public PlanItemLifeCycleEvent getPlanItemLifeCycleEvent() {
        return planItemLifeCycleEvent;
    }

    public void setPlanItemLifeCycleEvent(PlanItemLifeCycleEvent planItemLifeCycleEvent) {
        this.planItemLifeCycleEvent = planItemLifeCycleEvent;
    }


    public boolean isEvaluateCaseInstanceCompleted() {
        return evaluateStagesAndCaseInstanceCompletion;
    }

    public void setEvaluateStagesAndCaseInstanceCompletion(boolean evaluateStagesAndCaseInstanceCompletion) {
        this.evaluateStagesAndCaseInstanceCompletion = evaluateStagesAndCaseInstanceCompletion;
    }
}

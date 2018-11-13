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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.listener.PlanItemLifeCycleListenerUtil;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CountingPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.EntityWithSentryPartInstances;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceContainer;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Joram Barrez
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

        String satisfiedExitCriterion = evaluateExitCriteria(caseInstanceEntity, getPlanModel(caseInstanceEntity));
        if (satisfiedExitCriterion != null) {
            CommandContextUtil.getAgenda(commandContext).planTerminateCaseInstanceOperation(caseInstanceEntity.getId(), satisfiedExitCriterion);

        } else {
            boolean criteriaChangeOrActiveChildren = evaluatePlanItemsCriteria(caseInstanceEntity);
            if (evaluateCaseInstanceCompleted 
                    && !criteriaChangeOrActiveChildren
                    && !CaseInstanceState.END_STATES.contains(caseInstanceEntity.getState())
                    && isPlanModelComplete()){
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

        for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstances) {

            PlanItem planItem = planItemInstanceEntity.getPlanItem();
            String state = planItemInstanceEntity.getState();

            if (PlanItemInstanceState.EVALUATE_ENTRY_CRITERIA_STATES.contains(state)) {
                
                String satisfiedEntryCriterion = evaluateEntryCriteria(planItemInstanceEntity, planItem);
                if (planItem.getEntryCriteria().isEmpty() || satisfiedEntryCriterion != null) {
                    boolean activatePlanItemInstance = true;
                    if (!planItem.getEntryCriteria().isEmpty() && hasRepetitionRule(planItemInstanceEntity)) {
                        boolean isRepeating = evaluateRepetitionRule(planItemInstanceEntity);
                        if (isRepeating) {

                            PlanItemInstanceEntity childPlanItemInstanceEntity = copyAndInsertPlanItemInstance(commandContext, planItemInstanceEntity, false);

                            String oldState = childPlanItemInstanceEntity.getState();
                            String newState = PlanItemInstanceState.WAITING_FOR_REPETITION;
                            childPlanItemInstanceEntity.setState(newState);
                            PlanItemLifeCycleListenerUtil.callLifeCycleListeners(commandContext, planItemInstanceEntity, oldState, newState);

                            if (newChildPlanItemInstances == null) {
                                newChildPlanItemInstances = new ArrayList<>(1);
                            }
                            newChildPlanItemInstances.add(childPlanItemInstanceEntity);
                            // createPlanItemInstance operations will also sync planItemInstance history
                            CommandContextUtil.getAgenda(commandContext).planCreatePlanItemInstanceForRepetitionOperation(childPlanItemInstanceEntity);

                        } else {
                            activatePlanItemInstance = false;
                        }
                    }

                    if (planItem.getPlanItemDefinition() instanceof EventListener) {
                        activatePlanItemInstance = false; // event listeners occur, they don't become active
                    }

                    if (activatePlanItemInstance) {
                        criteriaChanged = true;
                        CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstanceOperation(planItemInstanceEntity, satisfiedEntryCriterion);
                    }

                }

            }
            
            if (!PlanItemInstanceState.END_STATES.contains(state)) {
                
                String satisfiedExitCriterion = evaluateExitCriteria(planItemInstanceEntity, planItem);
                if (satisfiedExitCriterion != null) {
                    criteriaChanged = true;
                    CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(planItemInstanceEntity, satisfiedExitCriterion);

                } else if (planItem.getPlanItemDefinition() instanceof Stage) {

                    if (PlanItemInstanceState.ACTIVE.equals(state)) {
                        boolean criteriaChangeOrActiveChildrenForStage = evaluatePlanItemsCriteria(planItemInstanceEntity);
                        if (criteriaChangeOrActiveChildrenForStage) {
                            criteriaChanged = true;
                            planItemInstanceEntity.setCompleteable(false); // an active child = stage cannot be completed anymore
                        } else {
                            Stage stage = (Stage) planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
                            if (isStageCompletable(planItemInstanceEntity, stage)) {
                                criteriaChanged = true;
                                CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
                            }
                        }
                    }
                } else if (PlanItemInstanceState.ACTIVE.equals(state)) {
                    activeChildren++;
                }
            }

            if (planItemInstanceEntity.getState() == null) {
                // plan item is still being created
                criteriaChanged = true;
            }
        }
        
        // After the loop, the newly created plan item instances can be added
        if (newChildPlanItemInstances != null) {
            for (PlanItemInstanceEntity newChildPlanItemInstance : newChildPlanItemInstances) {
                planItemInstanceContainer.getChildPlanItemInstances().add(newChildPlanItemInstance);
            }
        }

        return criteriaChanged || activeChildren > 0;
    }

    protected String evaluateEntryCriteria(PlanItemInstanceEntity planItemInstanceEntity, PlanItem planItem) {
        List<Criterion> criteria = planItem.getEntryCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            return evaluateCriteria(planItemInstanceEntity, criteria);
        }
        return null;
    }

    protected String evaluateExitCriteria(EntityWithSentryPartInstances entityWithSentryPartInstances, HasExitCriteria hasExitCriteria) { // EntityWithSentryPartInstances -> can be used for both case instance and plan item instance
        List<Criterion> criteria = hasExitCriteria.getExitCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            return evaluateCriteria(entityWithSentryPartInstances, criteria);
        }
        return null;
    }

    /**
     * @return Returns the id of the criterion that is satisfied.
     *         If none is satisfied, null is returned.
     */
    protected String evaluateCriteria(EntityWithSentryPartInstances entityWithSentryPartInstances, List<Criterion> criteria) {
        for (Criterion criterion : criteria) {

            Sentry sentry = criterion.getSentry();

            // There can be zero or more on parts and zero or one if part.
            // All defined parts need to be satisfied for the sentry to trigger.

            if (sentry.getOnParts().size() == 1 && sentry.getSentryIfPart() == null) { // Only one one part and no if part: no need to fetch the previously satisfied onparts
                if (planItemLifeCycleEvent != null) {
                    SentryOnPart sentryOnPart = sentry.getOnParts().get(0);
                    if (sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {
                        return criterion.getId();
                    }
                }

            } else if (sentry.getOnParts().isEmpty() && sentry.getSentryIfPart() != null) { // Only an if part: simply evaluate the if part
                if (evaluateSentryIfPart(sentry, entityWithSentryPartInstances)) {
                    return criterion.getId();
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
                            createSentryPartInstanceEntity(entityWithSentryPartInstances, sentry, sentryOnPart, null);
                            satisfiedSentryOnPartIds.add(sentryOnPart.getId());
                        }
                    }
                }

                boolean allOnPartsSatisfied = (satisfiedSentryOnPartIds.size() == sentry.getOnParts().size());

                // Evaluate the if part of the sentry:
                // In the onEvent triggerMode all onParts need to be satisfied before the if is evaluated
                if (sentry.getSentryIfPart() != null && !sentryIfPartSatisfied
                        && (isDefaultTriggerMode || (sentry.isOnEventTriggerMode() && allOnPartsSatisfied) )) {

                    if (evaluateSentryIfPart(sentry, entityWithSentryPartInstances)) {
                        createSentryPartInstanceEntity(entityWithSentryPartInstances, sentry, null, sentry.getSentryIfPart());
                        sentryIfPartSatisfied = true;
                    }

                }

                if (allOnPartsSatisfied && (sentryIfPartSatisfied || sentry.getSentryIfPart() == null)) {
                    return criterion.getId();
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
    
    protected boolean evaluateSentryIfPart(Sentry sentry, EntityWithSentryPartInstances entityWithSentryPartInstances) {
        Expression conditionExpression = CommandContextUtil.getExpressionManager(commandContext).createExpression(sentry.getSentryIfPart().getCondition());
        Object result = conditionExpression.getValue(entityWithSentryPartInstances);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return false;
    }

    protected boolean isEndStateReachedForAllRequiredChildPlanItems(PlanItemInstanceContainer planItemInstanceContainer) {
        if (planItemInstanceContainer.getChildPlanItemInstances() != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : planItemInstanceContainer.getChildPlanItemInstances()) {
                if (PlanItemInstanceState.END_STATES.contains(childPlanItemInstance.getState())) {
                    continue;
                }
                if (isRequiredPlanItemInstance(childPlanItemInstance)) {
                    return false;
                }
                return isEndStateReachedForAllChildPlanItems(childPlanItemInstance);
            }
        }
        return true;
    }

    protected boolean isRequiredPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemControl planItemControl = planItemInstanceEntity.getPlanItem().getItemControl();
        if (planItemControl != null && planItemControl.getRequiredRule() != null) {

            boolean isRequired = true; // Having a required rule means required by default, unless the condition says otherwise
            String requiredCondition = planItemControl.getRequiredRule().getCondition();
            if (StringUtils.isNotEmpty(requiredCondition)) {
                isRequired = evaluateBooleanExpression(commandContext, planItemInstanceEntity, requiredCondition);
            }
            return isRequired;
        }
        return false;
    }


    protected boolean isEndStateReachedForAllChildPlanItems(PlanItemInstanceContainer planItemInstanceContainer) {
        if (planItemInstanceContainer.getChildPlanItemInstances() != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : planItemInstanceContainer.getChildPlanItemInstances()) {
                if (!PlanItemInstanceState.END_STATES.contains(childPlanItemInstance.getState())) {
                    return false;
                }
                boolean allChildChildsEndStateReached = isEndStateReachedForAllChildPlanItems(childPlanItemInstance);
                if (!allChildChildsEndStateReached) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean isAvailableChildPlanCompletionNeutralOrNotActive(PlanItemInstanceContainer planItemInstanceContainer) {
        if (planItemInstanceContainer.getChildPlanItemInstances() != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : planItemInstanceContainer.getChildPlanItemInstances()) {
                if (PlanItemInstanceState.END_STATES.contains(childPlanItemInstance.getState())) {
                    continue;
                }
                if (PlanItemInstanceState.AVAILABLE.contains(childPlanItemInstance.getState()) && isCompletionNeutralPlanItemInstance(childPlanItemInstance)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    protected boolean isCompletionNeutralPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        PlanItemControl planItemControl = planItemInstanceEntity.getPlanItem().getItemControl();
        if (planItemControl != null && planItemControl.getCompletionNeutralRule() != null) {

            boolean isCompletionNeutral = true; // Having a required rule means required by default, unless the condition says otherwise
            String condition = planItemControl.getCompletionNeutralRule().getCondition();
            if (StringUtils.isNotEmpty(condition)) {
                isCompletionNeutral = evaluateBooleanExpression(commandContext, planItemInstanceEntity, condition);
            }
            return isCompletionNeutral;
        }
        return false;
    }


    protected boolean isStageCompletable(PlanItemInstanceEntity stagePlanItemInstanceEntity, Stage stage) {
        boolean allRequiredChildrenInEndState = isEndStateReachedForAllRequiredChildPlanItems(stagePlanItemInstanceEntity);
        if (allRequiredChildrenInEndState) {
            stagePlanItemInstanceEntity.setCompleteable(true);
        }

        if (stagePlanItemInstanceEntity.isCompleteable()) {
            if (stage.isAutoComplete()) {
                return true;
            } else {
                return isAvailableChildPlanCompletionNeutralOrNotActive(stagePlanItemInstanceEntity);
            }
        } else {
            return false;
        }
    }
    
    protected boolean isPlanModelComplete() {
        boolean allRequiredChildrenInEndState = isEndStateReachedForAllRequiredChildPlanItems(caseInstanceEntity);
        if (allRequiredChildrenInEndState) {
            caseInstanceEntity.setCompleteable(true);
        }
        
        boolean isAutoComplete = CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel().isAutoComplete();

        if (caseInstanceEntity.isCompleteable()) {
            if (isAutoComplete) {
                return true;
            } else {
                return isAvailableChildPlanCompletionNeutralOrNotActive(caseInstanceEntity);
            }
        } else {
            return false;
        }
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
                stringBuilder.append(" for plan item ").append(planItemLifeCycleEvent.getPlanItem().getId());
                if (planItemLifeCycleEvent.getPlanItem().getName() != null) {
                    stringBuilder.append(" (").append(planItemLifeCycleEvent.getPlanItem().getName()).append(")");
                }
            }
        }

        return stringBuilder.toString();
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
    
}

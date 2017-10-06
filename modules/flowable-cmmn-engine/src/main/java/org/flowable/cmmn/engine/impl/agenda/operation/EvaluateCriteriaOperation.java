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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.EntityWithSentryPartInstances;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.engine.common.api.delegate.Expression;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class EvaluateCriteriaOperation extends AbstractCaseInstanceOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateCriteriaOperation.class);

    protected PlanItemLifeCycleEvent planItemLifeCycleEvent;

    private enum CriteriaEvaluationResult {SENTRY_SATISFIED, PART_TRIGGERED, NONE}

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

        CriteriaEvaluationResult planModelExitCriteriaEvaluationResult = evaluateExitCriteria(caseInstanceEntity, getPlanModel(caseInstanceEntity));
        if (CriteriaEvaluationResult.SENTRY_SATISFIED.equals(planModelExitCriteriaEvaluationResult)) {
            CommandContextUtil.getAgenda(commandContext).planTerminateCaseInstance(caseInstanceEntity.getId(), false);

        } else {
            boolean criteriaChangeOrActiveChildren = evaluatePlanItemsCriteria(caseInstanceEntity.getChildPlanItemInstances());
            if (!criteriaChangeOrActiveChildren) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("No active plan items found for plan model, completing case instance");
                }
                CommandContextUtil.getAgenda(commandContext).planCompleteCaseInstance(caseInstanceEntity);
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
    protected boolean evaluatePlanItemsCriteria(List<PlanItemInstanceEntity> planItemInstances) {
        int activeChildren = 0;
        boolean criteriaChanged = false;
        for (PlanItemInstanceEntity planItemInstanceEntity : planItemInstances) {

            PlanItem planItem = planItemInstanceEntity.getPlanItem();
            CriteriaEvaluationResult evaluationResult = null;
            if (PlanItemInstanceState.AVAILABLE.equals(planItemInstanceEntity.getState())) {
                evaluationResult = evaluateEntryCriteria(planItemInstanceEntity, planItem);
                if (evaluationResult.equals(CriteriaEvaluationResult.SENTRY_SATISFIED)) {
                    CommandContextUtil.getAgenda(commandContext).planActivatePlanItemInstance(planItemInstanceEntity);
                }

            } else if (PlanItemInstanceState.ACTIVE.equals(planItemInstanceEntity.getState())) {
                evaluationResult = evaluateExitCriteria(planItemInstanceEntity, planItem);
                if (evaluationResult.equals(CriteriaEvaluationResult.SENTRY_SATISFIED)) {
                    CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstance(planItemInstanceEntity);

                } else if (planItem.getPlanItemDefinition() instanceof Stage) {
                    boolean criteriaChangeOrActiveChildrenForStage = evaluateStagePlanItemInstance(planItemInstanceEntity);
                    if (criteriaChangeOrActiveChildrenForStage) {
                        activeChildren++;
                    } else {
                        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstance(planItemInstanceEntity);
                    }

                } else {
                    activeChildren++;

                }

            }

            if (evaluationResult != null && !evaluationResult.equals(CriteriaEvaluationResult.NONE)) {
                criteriaChanged = true; // some part of a sentry has changed
            }

        }

        return criteriaChanged || activeChildren > 0;
    }

    protected boolean evaluateStagePlanItemInstance(PlanItemInstanceEntity stagePlanItemInstanceEntity) {

        // Can already be completed by another evaluation. No need to verify it again.
        if (!PlanItemInstanceState.ACTIVE.equals(stagePlanItemInstanceEntity.getState())) {
            return false;
        }

        // Check children of stage
        if (stagePlanItemInstanceEntity.getChildren() != null) {
            return evaluatePlanItemsCriteria(stagePlanItemInstanceEntity.getChildren());
        }

        return false;
    }

    protected CriteriaEvaluationResult evaluateEntryCriteria(PlanItemInstanceEntity planItemInstanceEntity, PlanItem planItem) {
        List<Criterion> criteria = planItem.getEntryCriteria();
        if (criteria == null || criteria.isEmpty()) {
            return CriteriaEvaluationResult.SENTRY_SATISFIED;
        } else {
            return evaluateCriteria(planItemInstanceEntity, criteria);
        }
    }

    protected CriteriaEvaluationResult evaluateExitCriteria(EntityWithSentryPartInstances entityWithSentryPartInstances, HasExitCriteria hasExitCriteria) {
        List<Criterion> criteria = hasExitCriteria.getExitCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            return evaluateCriteria(entityWithSentryPartInstances, criteria);
        }
        return CriteriaEvaluationResult.NONE;
    }

    protected CriteriaEvaluationResult evaluateCriteria(EntityWithSentryPartInstances entityWithSentryPartInstances, List<Criterion> criteria) {
        boolean partTriggered = false;
        for (Criterion entryCriterion : criteria) {
            Sentry sentry = entryCriterion.getSentry();

            if (sentry.getOnParts().size() == 1 && sentry.getSentryIfPart() == null) { // No need to look into the satisfied onparts
                if (planItemLifeCycleEvent != null) {
                    SentryOnPart sentryOnPart = sentry.getOnParts().get(0);
                    if (sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {
                        return CriteriaEvaluationResult.SENTRY_SATISFIED;
                    }
                }

            } else if (sentry.getOnParts().isEmpty() && sentry.getSentryIfPart() != null) {
                if (evaluateSentryIfPart(sentry, entityWithSentryPartInstances)) {
                    return CriteriaEvaluationResult.SENTRY_SATISFIED;
                }
                
            } else {

                boolean sentryIfPartSatisfied = false;
                Set<String> satisfiedSentryOnPartIds = new HashSet<>(1); // can maxmimum be one for a given sentry
                for (SentryPartInstanceEntity sentryPartInstanceEntity : entityWithSentryPartInstances.getSatisfiedSentryPartInstances()) {
                    if (sentryPartInstanceEntity.getOnPartId() != null) {
                        satisfiedSentryOnPartIds.add(sentryPartInstanceEntity.getOnPartId());
                    } else if (sentryPartInstanceEntity.getIfPartId() != null 
                            && sentryPartInstanceEntity.getIfPartId().equals(sentry.getSentryIfPart().getId())) {
                        sentryIfPartSatisfied = true;
                    }
                }

                boolean criteriaSatisfied = false;
                
                // On parts
                for (SentryOnPart sentryOnPart : sentry.getOnParts()) {
                    if (!satisfiedSentryOnPartIds.contains(sentryOnPart.getId())) {
                        if (planItemLifeCycleEvent != null && sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {
                            createSentryPartInstanceEntity(entityWithSentryPartInstances, sentryOnPart, null);
                            satisfiedSentryOnPartIds.add(sentryOnPart.getId());
                            criteriaSatisfied = true;
                        }
                    }
                }
                
                // If parts
                if (sentry.getSentryIfPart() != null && !sentryIfPartSatisfied) {
                    if (evaluateSentryIfPart(sentry, entityWithSentryPartInstances)) {
                        createSentryPartInstanceEntity(entityWithSentryPartInstances, null, sentry.getSentryIfPart());
                        sentryIfPartSatisfied = true;
                        criteriaSatisfied = true;
                    }
                }

                if (entityWithSentryPartInstances.getSatisfiedSentryPartInstances().size() == (sentry.getOnParts().size() + (sentry.getSentryIfPart() != null ? 1 : 0))) {
                    return CriteriaEvaluationResult.SENTRY_SATISFIED;
                } else if (criteriaSatisfied) {
                    partTriggered = true;
                }

            }

        }
        
        return partTriggered ? CriteriaEvaluationResult.PART_TRIGGERED : CriteriaEvaluationResult.NONE;
    }

    public boolean sentryOnPartMatchesCurrentLifeCycleEvent(SentryOnPart sentryOnPart) {
        return planItemLifeCycleEvent.getPlanItem().getId().equals(sentryOnPart.getSourceRef())
                && planItemLifeCycleEvent.getTransition().equals(sentryOnPart.getStandardEvent());
    }

    protected SentryPartInstanceEntity createSentryPartInstanceEntity(EntityWithSentryPartInstances entityWithSentryPartInstances, 
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
            sentryPartInstanceEntity.setCaseInstanceId(((PlanItemInstanceEntity) entityWithSentryPartInstances).getCaseInstanceId());
            sentryPartInstanceEntity.setCaseDefinitionId(((PlanItemInstanceEntity) entityWithSentryPartInstances).getCaseDefinitionId());
            sentryPartInstanceEntity.setPlanItemInstanceId(((PlanItemInstanceEntity) entityWithSentryPartInstances).getId());
        }

        sentryPartInstanceEntityManager.insert(sentryPartInstanceEntity);
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
            stringBuilder.append(" with transition ").append(planItemLifeCycleEvent.getTransition()).append(" having fired");
            if (planItemLifeCycleEvent.getPlanItem() != null) {
                stringBuilder.append(" for plan item ").append(planItemLifeCycleEvent.getPlanItem().getId());
                if (planItemLifeCycleEvent.getPlanItem().getName() != null) {
                    stringBuilder.append(" (").append(planItemLifeCycleEvent.getPlanItem().getName()).append(")");
                }
            }
        }

        return stringBuilder.toString();
    }

}

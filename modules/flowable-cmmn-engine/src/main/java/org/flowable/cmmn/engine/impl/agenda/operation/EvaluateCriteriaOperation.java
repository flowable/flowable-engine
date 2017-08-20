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
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryOnPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryOnPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class EvaluateCriteriaOperation extends AbstractCaseInstanceOperation {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateCriteriaOperation.class);
    
    protected PlanItemLifeCycleEvent planItemLifeCycleEvent;
    
    private enum EvaluationResult { ALL_FIRED, SOME_FIRED , NONE_FIRED };
    
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
        evaluateStagePlanItemInstance(caseInstanceEntity.getPlanModelInstance());
    }

    protected void evaluateStagePlanItemInstance(PlanItemInstanceEntity stagePlanItemInstanceEntity) {
        
        // Can already be completed by another evaluation. No need to verify it again.
        if (!PlanItemInstanceState.ACTIVE.equals(stagePlanItemInstanceEntity.getState())) {
            return;
        }
        
        int activeChildren = 0;
        boolean criteriaFired = false;
        if (stagePlanItemInstanceEntity.getChildren() != null) {
            for (PlanItemInstanceEntity planItemInstanceEntity : stagePlanItemInstanceEntity.getChildren()) {
                
                PlanItem planItem = planItemInstanceEntity.getPlanItem();
                EvaluationResult evaluationResult = null;
                if (PlanItemInstanceState.AVAILABLE.equals(planItemInstanceEntity.getState())) {
                    evaluationResult = evaluateEntryCriteria(planItemInstanceEntity, planItem);
                    if (evaluationResult.equals(EvaluationResult.ALL_FIRED)) {
                        CommandContextUtil.getAgenda(commandContext).planActivatePlanItem(planItemInstanceEntity);  
                    }
                    
                } else if (PlanItemInstanceState.ACTIVE.equals(planItemInstanceEntity.getState()) ) {
                    activeChildren++;
                    evaluationResult = evaluateExitCriteria(planItemInstanceEntity, planItem);
                    if (evaluationResult.equals(EvaluationResult.ALL_FIRED)) {
                        CommandContextUtil.getAgenda(commandContext).planExitPlanItem(planItemInstanceEntity);
                    }
                    
                }
                
                if (planItem.getPlanItemDefinition() instanceof PlanFragment) {
                    evaluateStagePlanItemInstance(planItemInstanceEntity);
                }
                
                if (evaluationResult != null && !evaluationResult.equals(EvaluationResult.NONE_FIRED)) {
                    criteriaFired = true;
                }
                
            }
        }
        
        if (evaluateStageExitCriteria(stagePlanItemInstanceEntity)) {
            criteriaFired = true;
        }
        
        if (!criteriaFired 
                && PlanItemInstanceState.ACTIVE.equals(stagePlanItemInstanceEntity.getState()) 
                && activeChildren == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No active plan items found for stage, planning stage completion");
            }
            CommandContextUtil.getAgenda(commandContext).planCompletePlanItem(stagePlanItemInstanceEntity);
        }
    }

    protected EvaluationResult evaluateEntryCriteria(PlanItemInstanceEntity planItemInstanceEntity, PlanItem planItem) {
        List<Criterion> criteria = planItem.getEntryCriteria();
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstanceEntity.getState())) {
            if (criteria == null || criteria.isEmpty()) {
                return EvaluationResult.ALL_FIRED;
            } else {
                return evaluateCriteria(planItemInstanceEntity, criteria);
            }
        }
        return EvaluationResult.NONE_FIRED;
    }
    
    protected EvaluationResult evaluateExitCriteria(PlanItemInstanceEntity planItemInstanceEntity, HasExitCriteria hasExitCriteria) {
        List<Criterion> criteria = hasExitCriteria.getExitCriteria();
        if (criteria != null && !criteria.isEmpty()) {
            return evaluateCriteria(planItemInstanceEntity, criteria);
        }
        return EvaluationResult.NONE_FIRED;
    }
    
    protected boolean evaluateStageExitCriteria(PlanItemInstanceEntity stagePlanItemInstanceEntity) {
        Stage stage  = getStage(stagePlanItemInstanceEntity);
        if (stage.getExitCriteria() != null && !stage.getExitCriteria().isEmpty()) {
            EvaluationResult evaluationResult = evaluateExitCriteria(stagePlanItemInstanceEntity, stage);
            if (evaluationResult.equals(EvaluationResult.ALL_FIRED)) {
                CommandContextUtil.getAgenda(commandContext).planExitPlanItem(stagePlanItemInstanceEntity);
                return true;
                
            } else if (evaluationResult != null && !evaluationResult.equals(EvaluationResult.NONE_FIRED)) {
                return true;
                
            }
        }
        return false;
    }
    
    protected EvaluationResult evaluateCriteria(PlanItemInstanceEntity planItemInstanceEntity, List<Criterion> criteria) {
        for (Criterion entryCriterion : criteria) {
            Sentry sentry = entryCriterion.getSentry();
            
            if (sentry.getOnParts().size() == 1) { // No need to look into the satisfied onparts
                if (planItemLifeCycleEvent != null) {
                    SentryOnPart sentryOnPart = sentry.getOnParts().get(0);
                    if (sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {
                        return EvaluationResult.ALL_FIRED;
                    }
                }
                
            } else {
                
                Set<String> satisfiedSentryOnPartIds = new HashSet<>();
                for (SentryOnPartInstanceEntity sentryOnPartInstanceEntity : planItemInstanceEntity.getSatisfiedSentryOnPartInstances()) {
                    satisfiedSentryOnPartIds.add(sentryOnPartInstanceEntity.getOnPartId());
                }
                
                boolean criteriaSatisfied = false;
                for (SentryOnPart sentryOnPart : sentry.getOnParts()) {
                    if (!satisfiedSentryOnPartIds.contains(sentryOnPart.getId())) {
                        if (planItemLifeCycleEvent != null && sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {
                            SentryOnPartInstanceEntity sentryOnPartInstanceEntity = createSentryOnPartInstanceEntity(planItemInstanceEntity, sentryOnPart);
                            planItemInstanceEntity.getSatisfiedSentryOnPartInstances().add(sentryOnPartInstanceEntity);
                            satisfiedSentryOnPartIds.add(sentryOnPart.getId());
                            criteriaSatisfied = true;
                        } 
                    }
                }
                
                if (sentry.getOnParts().size() == planItemInstanceEntity.getSatisfiedSentryOnPartInstances().size()) {
                    return EvaluationResult.ALL_FIRED;
                } else if (criteriaSatisfied){
                    return EvaluationResult.SOME_FIRED;
                }
                
            }
            
        }
        return EvaluationResult.NONE_FIRED;
    }
    
    public boolean sentryOnPartMatchesCurrentLifeCycleEvent(SentryOnPart sentryOnPart) {
        return planItemLifeCycleEvent.getPlanItem().getId().equals(sentryOnPart.getSourceRef()) 
                    && planItemLifeCycleEvent.getTransition().equals(sentryOnPart.getStandardEvent());
    }
    
    protected SentryOnPartInstanceEntity createSentryOnPartInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity, SentryOnPart sentryOnPart) {
        SentryOnPartInstanceEntityManager sentryOnPartInstanceEntityManager = CommandContextUtil.getSentryOnPartInstanceEntityManager(commandContext);
        SentryOnPartInstanceEntity sentryOnPartInstanceEntity = sentryOnPartInstanceEntityManager.create();
        sentryOnPartInstanceEntity.setCaseInstanceId(planItemInstanceEntity.getCaseInstanceId());
        sentryOnPartInstanceEntity.setCaseDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
        sentryOnPartInstanceEntity.setPlanItemInstanceId(planItemInstanceEntity.getId());
        sentryOnPartInstanceEntity.setOnPartId(sentryOnPart.getId());
        sentryOnPartInstanceEntity.setTimeStamp(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        sentryOnPartInstanceEntityManager.insert(sentryOnPartInstanceEntity);
        return sentryOnPartInstanceEntity;
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
            stringBuilder.append(" with transition " + planItemLifeCycleEvent.getTransition() + " having fired");
            if (planItemLifeCycleEvent.getPlanItem() != null) {
                stringBuilder.append(" for plan item " + planItemLifeCycleEvent.getPlanItem().getId());
                if (planItemLifeCycleEvent.getPlanItem().getName() != null) {
                    stringBuilder.append(" (" + planItemLifeCycleEvent.getPlanItem().getName() + ")");
                }
            }
        }
        
        return stringBuilder.toString();
    }
    
}

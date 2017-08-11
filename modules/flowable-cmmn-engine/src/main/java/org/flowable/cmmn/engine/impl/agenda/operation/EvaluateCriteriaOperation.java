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

import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class EvaluateCriteriaOperation extends AbstractCaseInstanceOperation {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateCriteriaOperation.class);
    
    protected PlanItemLifeCycleEvent planItemLifeCycleEvent;
    
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
        
        PlanItemInstanceEntity stagePlanItemInstanceEntity = caseInstanceEntity.getPlanModelInstance();
        int activeChildren = 0;
        boolean criteriaFired = false;
        if (stagePlanItemInstanceEntity.getChildren() != null) {
            for (PlanItemInstanceEntity planItemInstanceEntity : stagePlanItemInstanceEntity.getChildren()) {
                
                PlanItem planItem = planItemInstanceEntity.getPlanItem();
                if (PlanItemInstanceState.AVAILABLE.equals(planItemInstanceEntity.getState())) {
                    
                    if (evaluatePlanItemInstanceEntryCriteria(planItemInstanceEntity, planItem)) {
                        criteriaFired = true;
                    }
                    
                } else if (PlanItemInstanceState.ACTIVE.equals(planItemInstanceEntity.getState()) ) {
                    activeChildren++;
                    
                }
                
            }
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
    
    protected boolean evaluatePlanItemInstanceEntryCriteria(PlanItemInstanceEntity planItemInstanceEntity, PlanItem planItem) {
        List<Criterion> criteria = planItem.getEntryCriteria();
        if (!PlanItemInstanceState.ACTIVE.equals(planItemInstanceEntity.getState()) 
                && (criteria == null || criteria.isEmpty() || evaluateCriteria(planItemInstanceEntity, criteria)) ) {
            CommandContextUtil.getAgenda(commandContext).planActivatePlanItem(planItemInstanceEntity);
            return true;
        }
        return false;
    }
    
    protected boolean evaluateCriteria(PlanItemInstanceEntity planItemInstanceEntity, List<Criterion> criteria) {
        for (Criterion entryCriterion : criteria) {
            Sentry sentry = entryCriterion.getSentry();
            
            if (sentry.getOnParts().size() == 1) {
                if (planItemLifeCycleEvent != null) {
                    SentryOnPart sentryOnPart = sentry.getOnParts().get(0);
                    if (sentryOnPartMatchesCurrentLifeCycleEvent(sentryOnPart)) {
                        return true;
                    }
                }
                
            } else {
                
                // TODO: store fired sentries?
                // TODO: if expressions
                
            }
            
        }
        return false;
    }
    
    public boolean sentryOnPartMatchesCurrentLifeCycleEvent(SentryOnPart sentryOnPart) {
        return planItemLifeCycleEvent.getPlanItem().getId().equals(sentryOnPart.getSourceRef()) 
                    && planItemLifeCycleEvent.getTransition().equals(sentryOnPart.getStandardEvent());
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

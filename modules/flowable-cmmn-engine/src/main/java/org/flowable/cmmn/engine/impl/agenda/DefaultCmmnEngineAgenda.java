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
package org.flowable.cmmn.engine.impl.agenda;

import java.util.Iterator;

import org.flowable.cmmn.engine.impl.agenda.operation.ActivateAsyncPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ActivatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CmmnOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompleteCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompletePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CreatePlanItemInstanceForRepetitionOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CreatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.DisablePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EnablePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EvaluateCriteriaOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ExitPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitPlanModelInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitStageInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.OccurPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.StartPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminateCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TriggerPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.agenda.AbstractAgenda;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class DefaultCmmnEngineAgenda extends AbstractAgenda implements CmmnEngineAgenda {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCmmnEngineAgenda.class);

    public DefaultCmmnEngineAgenda(CommandContext commandContext) {
        super(commandContext);
    }

    public void addOperation(CmmnOperation operation, String caseInstanceId) {
        
        int operationIndex = getOperationIndex(operation);
        if (operationIndex >= 0) {
            operations.add(operationIndex, operation);
        } else {
            operations.addLast(operation);
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Planned {}", operation);
        }
        
        if (caseInstanceId != null) {
            CommandContextUtil.addInvolvedCaseInstanceId(commandContext, caseInstanceId);
        }
    }
    
    /**
     * Returns the index in the list of operations where the {@link CmmnOperation} should be inserted.
     * Returns a negative value if the element should be added to the end of the list. 
     */
    protected int getOperationIndex(CmmnOperation operation) {
        
        // The operation to evaluate the criteria is the most expensive operation.
        // As such, when it's planned it is always 
        // - moved to the end of the operations list
        // - checked for duplicates to avoid duplicate evaluations (see the add method for it)
        // - other operations are always planned before, as these can trigger new evaluation operations
        
        if (!operations.isEmpty() && !(operation instanceof EvaluateCriteriaOperation)) {
            for (int i=0; i<operations.size(); i++) {
                if (operations.get(i) instanceof EvaluateCriteriaOperation) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void planInitPlanModelOperation(CaseInstanceEntity caseInstanceEntity) {
        addOperation(new InitPlanModelInstanceOperation(commandContext, caseInstanceEntity), caseInstanceEntity.getId());
    }

    @Override
    public void planInitStageOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new InitStageInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }
    
    @Override
    public void planEvaluateCriteriaOperation(String caseInstanceEntityId) {
        internalPlanEvaluateCriteria(caseInstanceEntityId, null, false);
    }
    
    @Override
    public void planEvaluateCriteriaOperation(String caseInstanceEntityId, boolean evaluateCaseInstanceCompleted) {
        internalPlanEvaluateCriteria(caseInstanceEntityId, null, evaluateCaseInstanceCompleted);
    }

    @Override
    public void planEvaluateCriteriaOperation(String caseInstanceEntityId, PlanItemLifeCycleEvent lifeCycleEvent) {
        internalPlanEvaluateCriteria(caseInstanceEntityId, lifeCycleEvent, false);
    }
    
    protected void internalPlanEvaluateCriteria(String caseInstanceEntityId, PlanItemLifeCycleEvent planItemLifeCycleEvent, boolean evaluateCaseInstanceCompleted) {
        
        // To avoid too many evaluations of the 'same situation', the currently planned operations are looked at
        // and when one is found that matches the pattern of one that is now to be planned, it is removed as the new one will
        // do the same thing at a later point in the execution.
        
        Iterator<Runnable> plannedOperations = operations.iterator();
        boolean found = false;
        while (!found && plannedOperations.hasNext()) {
            Runnable operation = plannedOperations.next();
            if (operation instanceof EvaluateCriteriaOperation) {
                EvaluateCriteriaOperation evaluateCriteriaOperation = (EvaluateCriteriaOperation) operation;
                if (evaluateCriteriaOperation.getCaseInstanceEntityId() != null
                        && evaluateCriteriaOperation.getPlanItemLifeCycleEvent() != null
                        && evaluateCriteriaOperation.getPlanItemLifeCycleEvent().getTransition() != null
                        && evaluateCriteriaOperation.getPlanItemLifeCycleEvent().getPlanItem() != null
                        && planItemLifeCycleEvent != null
                        && evaluateCriteriaOperation.getCaseInstanceEntityId().equals(caseInstanceEntityId)
                        && evaluateCriteriaOperation.getPlanItemLifeCycleEvent().getTransition().equals(planItemLifeCycleEvent.getTransition())
                        && evaluateCriteriaOperation.getPlanItemLifeCycleEvent().getPlanItem().getId().equals(planItemLifeCycleEvent.getPlanItem().getId())
                        && evaluateCriteriaOperation.isEvaluateCaseInstanceCompleted() == evaluateCaseInstanceCompleted) {
                    LOGGER.info("Deferred criteria evaluation for {} to later in the execution", caseInstanceEntityId);
                    plannedOperations.remove();
                    found = true;
                }
            }
        }
        
        EvaluateCriteriaOperation evaluateCriteriaOperation = new EvaluateCriteriaOperation(commandContext, caseInstanceEntityId, planItemLifeCycleEvent);
        evaluateCriteriaOperation.setEvaluateCaseInstanceCompleted(evaluateCaseInstanceCompleted);
        addOperation(evaluateCriteriaOperation, caseInstanceEntityId);
    }
    
    @Override
    public void planCreatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CreatePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planCreatePlanItemInstanceForRepetitionOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CreatePlanItemInstanceForRepetitionOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planActivatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ActivatePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }
    
    @Override
    public void planStartPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new StartPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }
    
    @Override
    public void planEnablePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new EnablePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    public void planActivateAsyncPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ActivateAsyncPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }
    
    @Override
    public void planDisablePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new DisablePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());        
    }

    @Override
    public void planCompletePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CompletePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planOccurPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new OccurPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planExitPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ExitPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planTerminatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new TerminatePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planTriggerPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new TriggerPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planCompleteCaseInstanceOperation(CaseInstanceEntity caseInstanceEntity) {
        addOperation(new CompleteCaseInstanceOperation(commandContext, caseInstanceEntity), caseInstanceEntity.getId());
    }

    @Override
    public void planTerminateCaseInstanceOperation(String caseInstanceEntityId, boolean manualTermination) {
        addOperation(new TerminateCaseInstanceOperation(commandContext, caseInstanceEntityId, manualTermination), caseInstanceEntityId);
    }
    
}

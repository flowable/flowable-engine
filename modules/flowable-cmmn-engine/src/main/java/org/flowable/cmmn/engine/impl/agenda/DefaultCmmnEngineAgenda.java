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

import java.util.List;

import org.flowable.cmmn.engine.impl.agenda.operation.ActivateAsyncPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ActivatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ChangePlanItemInstanceToAvailableOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CmmnOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompleteCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompletePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CreatePlanItemInstanceForRepetitionOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CreatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CreatePlanItemInstanceWithoutEvaluationOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CreateRepeatedPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.DisablePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.DismissPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EnablePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EvaluateCriteriaOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EvaluateToActivatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EvaluateVariableEventListenersOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ExitPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitPlanModelInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitStageInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitiatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.OccurPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ReactivateCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ReactivatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ReactivatePlanModelInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.StartPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminateCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TriggerPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.behavior.impl.ChildTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.interceptor.MigrationContext;
import org.flowable.cmmn.model.PlanItem;
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

    public void addOperation(CmmnOperation operation) {
        
        int operationIndex = getOperationIndex(operation);
        if (operationIndex >= 0) {
            operations.add(operationIndex, operation);
        } else {
            operations.addLast(operation);
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Planned {}", operation);
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
        addOperation(new InitPlanModelInstanceOperation(commandContext, caseInstanceEntity));
    }

    @Override
    public void planReactivateCaseInstanceOperation(CaseInstanceEntity caseInstanceEntity) {
        addOperation(new ReactivateCaseInstanceOperation(commandContext, caseInstanceEntity));
    }

    @Override
    public void planReactivatePlanModelOperation(CaseInstanceEntity caseInstanceEntity, List<PlanItem> directlyReactivatedPlanItems) {
        addOperation(new ReactivatePlanModelInstanceOperation(commandContext, caseInstanceEntity, directlyReactivatedPlanItems));
    }

    @Override
    public void planInitStageOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new InitStageInstanceOperation(commandContext, planItemInstanceEntity));
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
        EvaluateCriteriaOperation evaluateCriteriaOperation = new EvaluateCriteriaOperation(commandContext, caseInstanceEntityId, planItemLifeCycleEvent);
        evaluateCriteriaOperation.setEvaluateStagesAndCaseInstanceCompletion(evaluateCaseInstanceCompleted);
        addOperation(evaluateCriteriaOperation);
    }
    
    @Override
    public void planCreatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CreatePlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planCreatePlanItemInstanceWithoutEvaluationOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CreatePlanItemInstanceWithoutEvaluationOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planCreateRepeatedPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CreateRepeatedPlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planReactivatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ReactivatePlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planCreatePlanItemInstanceForRepetitionOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CreatePlanItemInstanceForRepetitionOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planInitiatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new InitiatePlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planDismissPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new DismissPlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planActivatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        addOperation(new ActivatePlanItemInstanceOperation(commandContext, planItemInstanceEntity, entryCriterionId));
    }

    @Override
    public void planEvaluateToActivatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new EvaluateToActivatePlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planStartPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        addOperation(new StartPlanItemInstanceOperation(commandContext, planItemInstanceEntity, entryCriterionId));
    }
    
    @Override
    public void planStartPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId,
            ChildTaskActivityBehavior.VariableInfo childTaskVariableInfo) {
        addOperation(new StartPlanItemInstanceOperation(commandContext, planItemInstanceEntity, entryCriterionId, childTaskVariableInfo));
    }
    
    @Override
    public void planStartPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId, MigrationContext migrationContext) {
        addOperation(new StartPlanItemInstanceOperation(commandContext, planItemInstanceEntity, entryCriterionId, migrationContext));
    }

    @Override
    public void planEnablePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        addOperation(new EnablePlanItemInstanceOperation(commandContext, planItemInstanceEntity, entryCriterionId));
    }

    @Override
    public void planActivateAsyncPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        addOperation(new ActivateAsyncPlanItemInstanceOperation(commandContext, planItemInstanceEntity, entryCriterionId));
    }
    
    @Override
    public void planDisablePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new DisablePlanItemInstanceOperation(commandContext, planItemInstanceEntity));        
    }

    @Override
    public void planCompletePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CompletePlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planOccurPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new OccurPlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planExitPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String exitCriterionId, String exitType, String exitEventType) {
        addOperation(new ExitPlanItemInstanceOperation(commandContext, planItemInstanceEntity, exitCriterionId, exitType, exitEventType));
    }

    @Override
    public void planTerminatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String exitType, String exitEventType) {
        addOperation(new TerminatePlanItemInstanceOperation(commandContext, planItemInstanceEntity, exitType, exitEventType));
    }
    
    @Override
    public void planChangePlanItemInstanceToAvailableOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ChangePlanItemInstanceToAvailableOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planTriggerPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new TriggerPlanItemInstanceOperation(commandContext, planItemInstanceEntity));
    }

    @Override
    public void planCompleteCaseInstanceOperation(CaseInstanceEntity caseInstanceEntity) {
        addOperation(new CompleteCaseInstanceOperation(commandContext, caseInstanceEntity));
    }

    @Override
    public void planManualTerminateCaseInstanceOperation(String caseInstanceEntityId) {
        addOperation(new TerminateCaseInstanceOperation(commandContext, caseInstanceEntityId, true, null, null, null));
    }

    @Override
    public void planTerminateCaseInstanceOperation(String caseInstanceEntityId, String exitCriterionId, String exitType, String exitEventType) {
        addOperation(new TerminateCaseInstanceOperation(commandContext, caseInstanceEntityId, false, exitCriterionId, exitType, exitEventType));
    }

    @Override
    public void planEvaluateVariableEventListenersOperation(String caseInstanceEntityId) {
        addOperation(new EvaluateVariableEventListenersOperation(commandContext, caseInstanceEntityId));
    }
}

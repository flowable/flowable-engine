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

import org.flowable.cmmn.engine.impl.agenda.operation.ActivatePlanItemOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CmmnOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompleteCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompletePlanItemOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EvaluateCriteriaOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ExitPlanItemOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitPlanModelOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitStageOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.OccurPlanItemOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminateCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminatePlanItemOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TriggerPlanItemOperation;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.engine.common.impl.agenda.AbstractAgenda;
import org.flowable.engine.common.impl.interceptor.CommandContext;
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
        operations.add(operation);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Planned " + operation);
        }
    }
    
    @Override
    public void planInitPlanModelOperation(CaseInstanceEntity caseInstanceEntity) {
        addOperation(new InitPlanModelOperation(commandContext, caseInstanceEntity));
    }
    
    @Override
    public void planInitStageOperation(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new InitStageOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planEvaluateCriteria(String caseInstanceEntityId) {
        addOperation(new EvaluateCriteriaOperation(commandContext, caseInstanceEntityId));
    }
    
    @Override
    public void planEvaluateCriteria(String caseInstanceEntityId, PlanItemLifeCycleEvent lifeCycleEvent) {
        addOperation(new EvaluateCriteriaOperation(commandContext, caseInstanceEntityId, lifeCycleEvent));
    }
    
    @Override
    public void planActivatePlanItem(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ActivatePlanItemOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planCompletePlanItem(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CompletePlanItemOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planOccurPlanItem(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new OccurPlanItemOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planExitPlanItem(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ExitPlanItemOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planTerminatePlanItem(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new TerminatePlanItemOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planTriggerPlanItem(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new TriggerPlanItemOperation(commandContext, planItemInstanceEntity));
    }
    
    @Override
    public void planCompleteCase(CaseInstanceEntity caseInstanceEntity) {
        addOperation(new CompleteCaseInstanceOperation(commandContext, caseInstanceEntity));
    }
    
    @Override
    public void planTerminateCase(String caseInstanceEntityId, boolean manualTermination) {
        addOperation(new TerminateCaseInstanceOperation(commandContext, caseInstanceEntityId, manualTermination));
    }
    
    @Override
    public void planTerminateCase(CaseInstanceEntity caseInstanceEntity, boolean manualTermination) {
        addOperation(new TerminateCaseInstanceOperation(commandContext, caseInstanceEntity, manualTermination));
    }

}

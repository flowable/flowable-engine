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

import org.flowable.cmmn.engine.impl.agenda.operation.ActivatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CmmnOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompleteCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.CompletePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.EvaluateCriteriaOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.ExitPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitPlanModelInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.InitStageInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.OccurPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminateCaseInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TerminatePlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.agenda.operation.TriggerPlanItemInstanceOperation;
import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
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

    public void addOperation(CmmnOperation operation, String caseInstanceId) {
        operations.add(operation);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Planned {}", operation);
        }
        
        if (caseInstanceId != null) {
            CommandContextUtil.addInvolvedCaseInstanceId(commandContext, caseInstanceId);
        }
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
    public void planEvaluateCriteria(String caseInstanceEntityId) {
        addOperation(new EvaluateCriteriaOperation(commandContext, caseInstanceEntityId), caseInstanceEntityId);
    }

    @Override
    public void planEvaluateCriteria(String caseInstanceEntityId, PlanItemLifeCycleEvent lifeCycleEvent) {
        addOperation(new EvaluateCriteriaOperation(commandContext, caseInstanceEntityId, lifeCycleEvent), caseInstanceEntityId);
    }

    @Override
    public void planActivatePlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ActivatePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planCompletePlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new CompletePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planOccurPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new OccurPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planExitPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new ExitPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planTerminatePlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new TerminatePlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planTriggerPlanItemInstance(PlanItemInstanceEntity planItemInstanceEntity) {
        addOperation(new TriggerPlanItemInstanceOperation(commandContext, planItemInstanceEntity), planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public void planCompleteCaseInstance(CaseInstanceEntity caseInstanceEntity) {
        addOperation(new CompleteCaseInstanceOperation(commandContext, caseInstanceEntity), caseInstanceEntity.getId());
    }

    @Override
    public void planTerminateCaseInstance(String caseInstanceEntityId, boolean manualTermination) {
        addOperation(new TerminateCaseInstanceOperation(commandContext, caseInstanceEntityId, manualTermination), caseInstanceEntityId);
    }
    
    

}

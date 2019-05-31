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

import java.util.Map;

import org.flowable.cmmn.engine.impl.criteria.PlanItemLifeCycleEvent;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.impl.agenda.Agenda;

/**
 * @author Joram Barrez
 */
public interface CmmnEngineAgenda extends Agenda {

    void planInitPlanModelOperation(CaseInstanceEntity caseInstanceEntity);

    void planInitStageOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planCreatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planCreatePlanItemInstanceForRepetitionOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planInitiatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planDismissPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planActivatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId);
    
    void planStartPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId);
    
    void planStartPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId, Map<String, Object> variables);
    
    void planEnablePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId);

    void planActivateAsyncPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId);

    void planDisablePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planCompletePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planOccurPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planExitPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity, String exitCriterionId);

    void planTerminatePlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planTriggerPlanItemInstanceOperation(PlanItemInstanceEntity planItemInstanceEntity);
    
    void planChangePlanItemInstanceToAvailableOperation(PlanItemInstanceEntity planItemInstanceEntity);

    void planCompleteCaseInstanceOperation(CaseInstanceEntity caseInstanceEntity);

    void planManualTerminateCaseInstanceOperation(String caseInstanceEntityId);

    void planTerminateCaseInstanceOperation(String caseInstanceEntityId, String exitCriterionId);

    void planEvaluateCriteriaOperation(String caseInstanceEntityId);
    
    void planEvaluateCriteriaOperation(String caseInstanceEntityId, boolean evaluateCaseInstanceComplete);

    void planEvaluateCriteriaOperation(String caseInstanceEntityId, PlanItemLifeCycleEvent lifeCycleEvent);

}

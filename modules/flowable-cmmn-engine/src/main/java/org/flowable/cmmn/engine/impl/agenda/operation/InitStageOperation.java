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
import java.util.List;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.runtime.PlanItemInstanceState;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class InitStageOperation extends AbstractPlanItemInstanceOperation {
    
    public InitStageOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        super(commandContext, planItemInstanceEntity);
    }
    
    @Override
    public void run() {
        Stage stage = getStage(planItemInstanceEntity);
        planItemInstanceEntity.setState(PlanItemInstanceState.ACTIVE);
        
        PlanItemInstanceEntityManager planItemInstanceEntityManager = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext);
        List<PlanItemInstanceEntity> planItemInstances = new ArrayList<>();
        for (PlanItem planItem : stage.getPlanItems()) {
            PlanItemInstanceEntity newPlanItemInstanceEntity = planItemInstanceEntityManager.create();
            newPlanItemInstanceEntity.setCaseDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
            newPlanItemInstanceEntity.setCaseInstanceId(planItemInstanceEntity.getCaseInstanceId());
            newPlanItemInstanceEntity.setName(planItem.getName());
            newPlanItemInstanceEntity.setStartTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
            newPlanItemInstanceEntity.setTenantId(planItemInstanceEntity.getTenantId());
            newPlanItemInstanceEntity.setElementId(planItem.getId());
            newPlanItemInstanceEntity.setStageInstanceId(planItemInstanceEntity.getId());
            newPlanItemInstanceEntity.setState(PlanItemInstanceState.AVAILABLE);
            planItemInstanceEntityManager.insert(newPlanItemInstanceEntity);
            
            planItemInstances.add(newPlanItemInstanceEntity);
        }
        planItemInstanceEntity.setChildren(planItemInstances);
        
        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteria(planItemInstanceEntity.getCaseInstanceId());
    }

    @Override
    public String toString() {
        return "[Operation] Initialise Stage using plan item " + planItemInstanceEntity.getName() + " (" + planItemInstanceEntity.getId() + ")";
    }

}

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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Stage;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class InitPlanModelOperation extends AbstractCaseInstanceOperation {
    
    protected CaseInstanceEntity caseInstanceEntity;
    
    public InitPlanModelOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, null, caseInstanceEntity);
        this.caseInstanceEntity = caseInstanceEntity;
    }
    
    @Override
    public void run() {
        super.run();
        
        Stage stage = CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel();
        List<PlanItemInstanceEntity> planItemInstances = createPlanItemInstances(commandContext, 
                stage.getPlanItems(), 
                caseInstanceEntity.getCaseDefinitionId(), 
                caseInstanceEntity.getId(), 
                null, 
                caseInstanceEntity.getTenantId());
        caseInstanceEntity.setChildPlanItemInstances(planItemInstances);
        
        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteria(caseInstanceEntity.getId());
    }
    
    @Override
    public String toString() {
        return "[Init Plan Model] initializing plan model for case instance " + caseInstanceEntity.getId();
    }

}

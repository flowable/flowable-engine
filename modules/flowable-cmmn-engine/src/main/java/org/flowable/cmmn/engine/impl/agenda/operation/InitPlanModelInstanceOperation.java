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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.repository.CaseDefinitionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EventInstanceCmmnUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.Stage;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.impl.constant.EventConstants;

/**
 * @author Joram Barrez
 */
public class InitPlanModelInstanceOperation extends AbstractCaseInstanceOperation {
    
    protected CaseInstanceEntity caseInstanceEntity;
    
    public InitPlanModelInstanceOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, null, caseInstanceEntity);
        this.caseInstanceEntity = caseInstanceEntity;
    }
    
    @Override
    public void run() {
        super.run();
        
        Case caseModel = CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId());
        Object eventInstance = caseInstanceEntity.getTransientVariable(EventConstants.EVENT_INSTANCE);
        if (eventInstance instanceof EventInstance) {
            EventInstanceCmmnUtil.handleEventInstanceOutParameters(caseInstanceEntity, caseModel, (EventInstance) eventInstance);
        }
        Stage stage = CaseDefinitionUtil.getCase(caseInstanceEntity.getCaseDefinitionId()).getPlanModel();
        createPlanItemInstancesForNewStage(commandContext,
                stage.getPlanItems(), 
                caseInstanceEntity.getCaseDefinitionId(), 
                caseInstanceEntity,
                null, 
                caseInstanceEntity.getTenantId());
        
        CommandContextUtil.getAgenda(commandContext).planEvaluateCriteriaOperation(caseInstanceEntity.getId());
    }
    
    @Override
    public String toString() {
        return "[Init Plan Model] initializing plan model for case instance " + caseInstanceEntity.getId();
    }

}

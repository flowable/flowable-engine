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

import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;

/**
 * @author Joram Barrez
 */
public abstract class AbstractDeleteCaseInstanceOperation extends AbstractChangeCaseInstanceStateOperation {

    public AbstractDeleteCaseInstanceOperation(CommandContext commandContext, String caseInstanceId) {
        super(commandContext, caseInstanceId);
    }

    public AbstractDeleteCaseInstanceOperation(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        super(commandContext, caseInstanceEntity);
    }

    @Override
    public void run() {
        super.run();
        deleteCaseInstance();
    }
    
    protected void deleteCaseInstance() {
        updateChildPlanItemInstancesState();
        CommandContextUtil.getCaseInstanceEntityManager(commandContext).delete(caseInstanceEntity.getId(), false, getDeleteReason());
        
        if (CommandContextUtil.getCmmnEngineConfiguration(commandContext).isLoggingSessionEnabled()) {
            String loggingType = null;
            if (CaseInstanceState.TERMINATED.equals(getNewState())) {
                loggingType = CmmnLoggingSessionConstants.TYPE_CASE_TERMINATED;
            } else {
                loggingType = CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED;
            }
            CmmnLoggingSessionUtil.addLoggingData(loggingType, "Completed case instance with id " + caseInstanceEntity.getId(), caseInstanceEntity);
        }
        
        String newState = getNewState();
        CommandContextUtil.getCaseInstanceHelper(commandContext).callCaseInstanceStateChangeCallbacks(commandContext, 
                caseInstanceEntity, caseInstanceEntity.getState(), newState);
        CommandContextUtil.getCmmnHistoryManager(commandContext)
            .recordCaseInstanceEnd(caseInstanceEntity, newState, commandContext.getCurrentEngineConfiguration().getClock().getCurrentTime());
    }

    protected void updateChildPlanItemInstancesState() {
        List<PlanItemInstanceEntity> childPlanItemInstances = caseInstanceEntity.getChildPlanItemInstances();
        if (childPlanItemInstances != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : childPlanItemInstances) {
                if (PlanItemInstanceState.ACTIVE.equals(childPlanItemInstance.getState())
                        || PlanItemInstanceState.AVAILABLE.equals(childPlanItemInstance.getState())) {
                    changeStateForChildPlanItemInstance(childPlanItemInstance);
                }
            }
        }
    }
    
    protected abstract String getDeleteReason();
    
}

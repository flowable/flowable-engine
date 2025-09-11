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
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.callback.CallbackData;
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
    public void internalExecute() {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (cmmnEngineConfiguration.getEndCaseInstanceInterceptor() != null) {
            cmmnEngineConfiguration.getEndCaseInstanceInterceptor().beforeEndCaseInstance(caseInstanceEntity, getNewState());
        }
        deleteCaseInstance();
        if (cmmnEngineConfiguration.getEndCaseInstanceInterceptor() != null) {
            cmmnEngineConfiguration.getEndCaseInstanceInterceptor().afterEndCaseInstance(caseInstanceEntity.getId(), getNewState());
        }
    }

    protected void deleteCaseInstance() {
        updateChildPlanItemInstancesState();
        
        String newState = getNewState();
        CallbackData callBackData = new CallbackData(caseInstanceEntity.getCallbackId(), caseInstanceEntity.getCallbackType(),
            caseInstanceEntity.getId(), caseInstanceEntity.getState(), newState);
        addAdditionalCallbackData(callBackData);
        CommandContextUtil.getCaseInstanceHelper(commandContext).callCaseInstanceStateChangeCallbacks(callBackData);
        
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CommandContextUtil.getCmmnHistoryManager(commandContext)
            .recordCaseInstanceEnd(caseInstanceEntity, newState, cmmnEngineConfiguration.getClock().getCurrentTime());

        if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
            String loggingType = null;
            if (CaseInstanceState.TERMINATED.equals(getNewState())) {
                loggingType = CmmnLoggingSessionConstants.TYPE_CASE_TERMINATED;
            } else {
                loggingType = CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED;
            }
            CmmnLoggingSessionUtil.addLoggingData(loggingType, "Completed case instance with id " + caseInstanceEntity.getId(), 
                    caseInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
        }

        CommandContextUtil.getCaseInstanceEntityManager(commandContext).delete(caseInstanceEntity.getId(), false, getDeleteReason());
    }

    protected void updateChildPlanItemInstancesState() {
        List<PlanItemInstanceEntity> childPlanItemInstances = caseInstanceEntity.getChildPlanItemInstances();
        if (childPlanItemInstances != null) {
            for (PlanItemInstanceEntity childPlanItemInstance : childPlanItemInstances) {
                // if the child plan item is not yet in a terminal state, terminate it
                if (!PlanItemInstanceState.isInTerminalState(childPlanItemInstance)) {
                    changeStateForChildPlanItemInstance(childPlanItemInstance);
                }
            }
        }
    }

    public abstract String getDeleteReason();

    public void addAdditionalCallbackData(CallbackData callbackData) {
        // meant to be overridden
    }
}

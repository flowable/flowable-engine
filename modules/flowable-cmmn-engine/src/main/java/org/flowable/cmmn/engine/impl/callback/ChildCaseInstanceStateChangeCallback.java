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
package org.flowable.cmmn.engine.impl.callback;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IOParameterUtil;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * Callback implementation for a child case instance returning it's state change to its parent.
 *
 * @author Joram Barrez
 */
public class ChildCaseInstanceStateChangeCallback implements RuntimeInstanceStateChangeCallback {

    @Override
    public void stateChanged(CallbackData callbackData) {

        /*
         * The child case instance has the plan item instance id as callback id stored.
         * When the child case instance is finished, the plan item of the parent case
         * needs to be triggered.
         */

        if (CaseInstanceState.COMPLETED.equals(callbackData.getNewState())
            || CaseInstanceState.TERMINATED.equals(callbackData.getNewState())) {

            CommandContext commandContext = CommandContextUtil.getCommandContext();
            PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext)
                .findById(callbackData.getCallbackId());

            if (planItemInstanceEntity != null) {
                if (CaseInstanceState.COMPLETED.equals(callbackData.getNewState())) {

                    // Handle out parameters here, before the child case instance gets deleted.
                    // The TriggerPlanItemInstanceOperation is only executed after the CompleteCaseInstanceOperation
                    // finishes, at which point the child case instance (and its variables) will already be deleted.
                    handleOutParameters(commandContext, planItemInstanceEntity, callbackData);

                    CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(planItemInstanceEntity);

                } else if (CaseInstanceState.TERMINATED.equals(callbackData.getNewState())) {

                    if (callbackData.getAdditionalData() != null && callbackData.getAdditionalData().containsKey(CallbackConstants.MANUAL_TERMINATION)) {

                        boolean manualTermination = (Boolean) callbackData.getAdditionalData().get(CallbackConstants.MANUAL_TERMINATION);
                        if (manualTermination) {
                            // For a manual termination, the state is simply changed and no additional logic (e.g. out parameter mapping) needs to be done.
                            CommandContextUtil.getAgenda(commandContext).planTerminatePlanItemInstanceOperation(planItemInstanceEntity,
                                (String) callbackData.getAdditionalData().get(CallbackConstants.EXIT_TYPE),
                                (String) callbackData.getAdditionalData().get(CallbackConstants.EXIT_EVENT_TYPE));
                        } else {
                            // a termination through an exit sentry needs to go beyond than just change the state
                            CommandContextUtil.getAgenda(commandContext).planExitPlanItemInstanceOperation(planItemInstanceEntity,
                                (String) callbackData.getAdditionalData().get(CallbackConstants.EXIT_CRITERION_ID),
                                (String) callbackData.getAdditionalData().get(CallbackConstants.EXIT_TYPE),
                                (String) callbackData.getAdditionalData().get(CallbackConstants.EXIT_EVENT_TYPE));
                        }

                    }

                }
            }

        }

    }

    protected void handleOutParameters(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, CallbackData callbackData) {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition instanceof CaseTask caseTask) {
            List<IOParameter> outParameters = caseTask.getOutParameters();
            if (outParameters != null && !outParameters.isEmpty()) {
                CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
                CaseInstanceEntity childCaseInstance = CommandContextUtil.getCaseInstanceEntityManager(commandContext)
                        .findById(callbackData.getInstanceId());
                if (childCaseInstance != null) {
                    IOParameterUtil.processOutParameters(outParameters, childCaseInstance, planItemInstanceEntity,
                            cmmnEngineConfiguration.getExpressionManager());
                }
            }
        }
    }

}

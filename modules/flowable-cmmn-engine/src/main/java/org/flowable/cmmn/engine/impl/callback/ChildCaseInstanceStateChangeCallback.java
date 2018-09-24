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

import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.callback.CallbackData;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
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
        
        if (CaseInstanceState.TERMINATED.equals(callbackData.getNewState())
                || CaseInstanceState.COMPLETED.equals(callbackData.getNewState())) {
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            PlanItemInstanceEntity planItemInstanceEntity = CommandContextUtil
                    .getPlanItemInstanceEntityManager(commandContext).findById(callbackData.getCallbackId());
            if (planItemInstanceEntity != null) {
                CommandContextUtil.getAgenda(commandContext).planTriggerPlanItemInstanceOperation(planItemInstanceEntity);
            }
        }
    }

}

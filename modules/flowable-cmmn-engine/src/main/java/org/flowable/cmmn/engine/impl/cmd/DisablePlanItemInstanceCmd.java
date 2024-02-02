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
package org.flowable.cmmn.engine.impl.cmd;

import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormInfo;

/**
 * @author Joram Barrez
 */
public class DisablePlanItemInstanceCmd extends AbstractNeedsPlanItemInstanceCmd {

    public DisablePlanItemInstanceCmd(String planItemInstanceId) {
        super(planItemInstanceId);
    }

    public DisablePlanItemInstanceCmd(String planItemInstanceId, Map<String, Object> variables,
            Map<String, Object> formVariables, String formOutcome, FormInfo formInfo,
            Map<String, Object> localVariables,
            Map<String, Object> transientVariables) {
        
        super(planItemInstanceId, variables, formVariables, formOutcome, formInfo, 
                localVariables, transientVariables);
    }

    @Override
    protected void internalExecute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        if (!PlanItemInstanceState.ENABLED.equals(planItemInstanceEntity.getState())) {
            throw new FlowableIllegalStateException("Can only disable a plan item instance which is in state ENABLED");
        }
        CommandContextUtil.getAgenda(commandContext).planDisablePlanItemInstanceOperation(planItemInstanceEntity);
    }
    
}

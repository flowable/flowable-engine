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

import java.io.Serializable;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.api.persistence.entity.VariableInstance;

public class GetPlanItemVariableInstanceCmd implements Command<VariableInstance>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected String planItemInstanceId;
    protected String variableName;

    public GetPlanItemVariableInstanceCmd(String planItemInstanceId, String variableName) {
        this.planItemInstanceId = planItemInstanceId;
        this.variableName = variableName;
    }

    @Override
    public VariableInstance execute(CommandContext commandContext) {
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("planItemInstanceId is null");
        }
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }

        PlanItemInstanceEntity planItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(planItemInstanceId);

        if (planItemInstance == null) {
            throw new FlowableObjectNotFoundException("plan item instance " + planItemInstanceId + " doesn't exist", PlanItemInstance.class);
        }

        return planItemInstance.getVariableInstance(variableName, false);
    }
}

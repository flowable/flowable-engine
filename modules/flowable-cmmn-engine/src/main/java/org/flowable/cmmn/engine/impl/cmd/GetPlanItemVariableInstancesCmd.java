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
import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.api.persistence.entity.VariableInstance;

public class GetPlanItemVariableInstancesCmd implements Command<Map<String, VariableInstance>>, Serializable {

    private static final long serialVersionUID = 1L;
    
    protected String planItemInstanceId;

    public GetPlanItemVariableInstancesCmd(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    @Override
    public Map<String, VariableInstance> execute(CommandContext commandContext) {

        // Verify existence of execution
        if (planItemInstanceId == null) {
            throw new FlowableIllegalArgumentException("planItemInstanceId is null");
        }

        PlanItemInstanceEntity planItemInstance = CommandContextUtil.getPlanItemInstanceEntityManager(commandContext).findById(planItemInstanceId);

        if (planItemInstance == null) {
            throw new FlowableObjectNotFoundException("plan item instance " + planItemInstanceId + " doesn't exist", PlanItemInstance.class);
        }

        return planItemInstance.getVariableInstancesLocal();
    }
}

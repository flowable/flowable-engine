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

package org.flowable.engine.impl.cmd;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class ChangeActivityStateCmd implements Command<Void> {

    protected final String processInstanceId;
    protected final String executionId;
    protected final String cancelActivityId;
    protected final String startActivityId;

    public ChangeActivityStateCmd(ChangeActivityStateBuilderImpl changeActivityStateBuilder) {
        this.processInstanceId = changeActivityStateBuilder.getProcessInstanceId();
        this.executionId = changeActivityStateBuilder.getExecutionId();
        this.cancelActivityId = changeActivityStateBuilder.getCancelActivityId();
        this.startActivityId = changeActivityStateBuilder.getStartActivityId();
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (processInstanceId == null && executionId == null) {
            throw new FlowableIllegalArgumentException("Process instance id or execution id is required");
        }
        
        DynamicStateManager dynamicStateManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicStateManager();

        String fromActivityId = null;
        ExecutionEntity execution = null;
        if (executionId != null) {
            execution = dynamicStateManager.resolveActiveExecution(executionId, commandContext);
            fromActivityId = execution.getCurrentFlowElement().getId();
            
        } else {
            execution = dynamicStateManager.resolveActiveExecution(processInstanceId, cancelActivityId, commandContext);
            fromActivityId = cancelActivityId;
        }

        dynamicStateManager.moveExecutionState(execution, fromActivityId, startActivityId, commandContext);

        return null;
    }

}

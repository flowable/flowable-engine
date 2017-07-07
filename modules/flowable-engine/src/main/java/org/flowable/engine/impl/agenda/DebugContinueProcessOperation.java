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
package org.flowable.engine.impl.agenda;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessDebugger;

/**
 * This class extends {@link ContinueProcessOperation} with the possibility to check whether execution is trying to
 * execute a breakpoint
 *
 * @author martin.grofcik
 */
public class DebugContinueProcessOperation extends ContinueProcessOperation {

    public static final String HANDLER_TYPE_BREAK_POINT = "breakpoint";
    protected ProcessDebugger debugger;

    public DebugContinueProcessOperation(ProcessDebugger debugger, CommandContext commandContext,
                                         ExecutionEntity execution, boolean forceSynchronousOperation,
                                         boolean inCompensation) {
        super(commandContext, execution, forceSynchronousOperation, inCompensation);
        this.debugger = debugger;
    }

    public DebugContinueProcessOperation(ProcessDebugger debugger, CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
        this.debugger = debugger;
    }

    protected void continueThroughFlowNode(FlowNode flowNode) {
        if (debugger.isBreakpoint(execution)) {
            breakExecution(flowNode);
        } else {
            super.continueThroughFlowNode(flowNode);
        }
    }

    protected void breakExecution(FlowNode flowNode) {
        DeadLetterJobEntity brokenJob = getDeadLetterJobEntityManager().create();
        brokenJob.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        brokenJob.setRevision(1);
        brokenJob.setRetries(0);
        brokenJob.setExecutionId(execution.getId());
        brokenJob.setProcessInstanceId(execution.getProcessInstanceId());
        brokenJob.setProcessDefinitionId(execution.getProcessDefinitionId());
        brokenJob.setExclusive(false);
        brokenJob.setJobHandlerType(HANDLER_TYPE_BREAK_POINT);

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            brokenJob.setTenantId(execution.getTenantId());
        }

        getDeadLetterJobEntityManager().insert(brokenJob);
    }

    protected DeadLetterJobEntityManager getDeadLetterJobEntityManager() {
        return CommandContextUtil.getDeadLetterJobEntityManager(commandContext);
    }

}

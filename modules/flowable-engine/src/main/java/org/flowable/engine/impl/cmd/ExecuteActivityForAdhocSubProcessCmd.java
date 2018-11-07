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

import java.io.Serializable;

import org.flowable.bpmn.model.AdhocSubProcess;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;

/**
 * @author Tijs Rademakers
 */
public class ExecuteActivityForAdhocSubProcessCmd implements Command<Execution>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String executionId;
    protected String activityId;

    public ExecuteActivityForAdhocSubProcessCmd(String executionId, String activityId) {
        this.executionId = executionId;
        this.activityId = activityId;
    }

    @Override
    public Execution execute(CommandContext commandContext) {
        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);
        if (execution == null) {
            throw new FlowableObjectNotFoundException("No execution found for id '" + executionId + "'", ExecutionEntity.class);
        }

        if (!(execution.getCurrentFlowElement() instanceof AdhocSubProcess)) {
            throw new FlowableException("The current flow element of the requested execution is not an ad-hoc sub process");
        }

        FlowNode foundNode = null;
        AdhocSubProcess adhocSubProcess = (AdhocSubProcess) execution.getCurrentFlowElement();

        // if sequential ordering, only one child execution can be active
        if (adhocSubProcess.hasSequentialOrdering()) {
            if (execution.getExecutions().size() > 0) {
                throw new FlowableException("Sequential ad-hoc sub process already has an active execution");
            }
        }

        for (FlowElement flowElement : adhocSubProcess.getFlowElements()) {
            if (activityId.equals(flowElement.getId()) && flowElement instanceof FlowNode) {
                FlowNode flowNode = (FlowNode) flowElement;
                if (flowNode.getIncomingFlows().size() == 0) {
                    foundNode = flowNode;
                }
            }
        }

        if (foundNode == null) {
            throw new FlowableException("The requested activity with id " + activityId + " can not be enabled");
        }

        ExecutionEntity activityExecution = CommandContextUtil.getExecutionEntityManager().createChildExecution(execution);
        activityExecution.setCurrentFlowElement(foundNode);
        CommandContextUtil.getAgenda().planContinueProcessOperation(activityExecution);

        return activityExecution;
    }

}

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
package org.flowable.engine.impl.jobexecutor;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 *
 * @author Tijs Rademakers
 */
public class AsyncSendEventJobHandler implements JobHandler {

    public static final String TYPE = "async-send-event";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ExecutionEntity executionEntity = (ExecutionEntity) variableScope;
        FlowElement flowElement = executionEntity.getCurrentFlowElement();

        if (!(flowElement instanceof SendEventServiceTask)) {
            throw new FlowableException("Unexpected activity type found for " + job + " at " + executionEntity);
        }

        Object behavior = ((SendEventServiceTask) flowElement).getBehavior();
        if (!(behavior instanceof ActivityBehavior activityBehavior)) {
            throw new FlowableException(
                    "Unexpected activity behavior (" + behavior.getClass() + ") found for " + job + " at " + executionEntity);
        }

        try {
            commandContext.addAttribute(TYPE, true); // Will be read in the SendEventTaskActivityBehavior
            activityBehavior.execute(executionEntity);
        } finally {
            commandContext.removeAttribute(TYPE);
        }
    }

}

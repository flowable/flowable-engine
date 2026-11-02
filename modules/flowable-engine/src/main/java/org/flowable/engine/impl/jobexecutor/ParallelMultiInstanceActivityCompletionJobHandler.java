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

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Filip Hrisafov
 */
public class ParallelMultiInstanceActivityCompletionJobHandler  implements JobHandler {

    public static final String TYPE = "parallel-multi-instance-complete";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        ExecutionEntity completingExecution = processEngineConfiguration.getExecutionEntityManager().findById(job.getExecutionId());
        if (completingExecution != null) {
            // It is possible that the execution completed (through another thread). In that case we ignore it.
            FlowElement currentFlowElement = completingExecution.getCurrentFlowElement();
            if (currentFlowElement instanceof Activity) {
                Object behavior = ((Activity) currentFlowElement).getBehavior();
                if (behavior instanceof ParallelMultiInstanceBehavior parallelMultiInstanceBehavior) {
                    parallelMultiInstanceBehavior.leaveAsync(completingExecution);
                }
            }
        }
    }
}

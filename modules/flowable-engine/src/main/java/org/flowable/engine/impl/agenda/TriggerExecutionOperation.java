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

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;
import org.flowable.engine.impl.jobexecutor.AsyncTriggerJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.JobUtil;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * Operation that triggers a wait state and continues the process, leaving that activity.
 * 
 * The {@link ExecutionEntity} for this operations should be in a wait state (receive task for example) and have a {@link FlowElement} that has a behaviour that implements the
 * {@link TriggerableActivityBehavior}.
 * 
 * @author Joram Barrez
 */
public class TriggerExecutionOperation extends AbstractOperation {
    
    protected boolean triggerAsync;

    public TriggerExecutionOperation(CommandContext commandContext, ExecutionEntity execution) {
        super(commandContext, execution);
    }

    public TriggerExecutionOperation(CommandContext commandContext, ExecutionEntity execution, boolean triggerAsync) {
        super(commandContext, execution);
        this.triggerAsync = triggerAsync;
    }

    @Override
    public void run() {
        FlowElement currentFlowElement = getCurrentFlowElement(execution);
        if (currentFlowElement instanceof FlowNode) {

            ActivityBehavior activityBehavior = (ActivityBehavior) ((FlowNode) currentFlowElement).getBehavior();
            if (activityBehavior instanceof TriggerableActivityBehavior) {

                if (!triggerAsync) {
                    ((TriggerableActivityBehavior) activityBehavior).trigger(execution, null, null);
                    
                } else {
                    ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                    JobService jobService = processEngineConfiguration.getJobServiceConfiguration().getJobService();
                    JobEntity job = JobUtil.createJob(execution, currentFlowElement, AsyncTriggerJobHandler.TYPE, processEngineConfiguration);

                    jobService.createAsyncJob(job, true);
                    jobService.scheduleAsyncJob(job);
                }


            } else {
                throw new FlowableException("Cannot trigger " + execution
                    + " : the activityBehavior " + activityBehavior.getClass() + " does not implement the "
                    + TriggerableActivityBehavior.class.getName() + " interface");

            }

        } else if (currentFlowElement == null) {
            throw new FlowableException("Cannot trigger " + execution
                    + " : no current flow element found. Check the execution id that is being passed "
                    + "(it should not be a process instance execution, but a child execution currently referencing a flow element).");

        } else {
            throw new FlowableException("Programmatic error: cannot trigger " + execution + ", invalid flow element type found: "
                    + currentFlowElement.getClass().getName() + ".");

        }
    }

}

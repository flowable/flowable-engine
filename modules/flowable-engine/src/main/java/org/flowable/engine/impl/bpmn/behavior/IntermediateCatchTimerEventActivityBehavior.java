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
package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;

import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.asyncexecutor.JobManager;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.persistence.entity.JobEntityManager;
import org.flowable.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

public class IntermediateCatchTimerEventActivityBehavior extends IntermediateCatchEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected TimerEventDefinition timerEventDefinition;

    public IntermediateCatchTimerEventActivityBehavior(TimerEventDefinition timerEventDefinition) {
        this.timerEventDefinition = timerEventDefinition;
    }

    @Override
    public void execute(DelegateExecution execution) {
        JobManager jobManager = CommandContextUtil.getJobManager();

        // end date should be ignored for intermediate timer events.
        TimerJobEntity timerJob = jobManager.createTimerJob(timerEventDefinition, false, (ExecutionEntity) execution, TriggerTimerEventJobHandler.TYPE,
                TimerEventHandler.createConfiguration(execution.getCurrentActivityId(), null, timerEventDefinition.getCalendarName()));

        if (timerJob != null) {
            jobManager.scheduleTimerJob(timerJob);
        }
    }

    @Override
    public void eventCancelledByEventGateway(DelegateExecution execution) {
        JobEntityManager jobEntityManager = CommandContextUtil.getJobEntityManager();
        List<JobEntity> jobEntities = jobEntityManager.findJobsByExecutionId(execution.getId());

        for (JobEntity jobEntity : jobEntities) { // Should be only one
            jobEntityManager.delete(jobEntity);
        }

        CommandContextUtil.getExecutionEntityManager().deleteExecutionAndRelatedData((ExecutionEntity) execution,
                DeleteReason.EVENT_BASED_GATEWAY_CANCEL);
    }

}

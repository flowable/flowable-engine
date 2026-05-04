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

import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.TimerUtil;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * Process-level timer start event behavior. Owns the deploy-time timer job scheduling for this
 * start event.
 */
public class TimerStartEventActivityBehavior extends FlowNodeActivityBehavior implements ProcessLevelStartEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected TimerEventDefinition timerEventDefinition;

    public TimerStartEventActivityBehavior(TimerEventDefinition timerEventDefinition) {
        this.timerEventDefinition = timerEventDefinition;
    }

    @Override
    public void deploy(ProcessLevelStartEventDeployContext context) {
        ProcessDefinitionEntity processDefinition = context.getProcessDefinition();
        TimerJobEntity timerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, context.getStartEvent(),
                false, processDefinition, TimerStartEventJobHandler.TYPE,
                TimerEventHandler.createConfiguration(context.getStartEvent().getId(),
                        timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));

        context.getProcessEngineConfiguration().getJobServiceConfiguration().getTimerJobService().scheduleTimerJob(timerJob);
    }

    @Override
    public void undeploy(ProcessLevelStartEventUndeployContext context) {
        context.registerObsoleteTimerJobHandlerType(TimerStartEventJobHandler.TYPE);
    }

    public TimerEventDefinition getTimerEventDefinition() {
        return timerEventDefinition;
    }
}

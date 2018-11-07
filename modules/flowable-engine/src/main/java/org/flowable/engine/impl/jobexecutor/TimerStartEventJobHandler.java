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
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cmd.StartProcessInstanceCmd;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerStartEventJobHandler extends TimerEventHandler implements JobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerStartEventJobHandler.class);

    public static final String TYPE = "timer-start-event";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {

        ProcessDefinitionEntity processDefinitionEntity = ProcessDefinitionUtil
                .getProcessDefinitionFromDatabase(job.getProcessDefinitionId()); // From DB -> need to get latest suspended state
        if (processDefinitionEntity == null) {
            throw new FlowableException("Could not find process definition needed for timer start event");
        }

        try {
            if (!processDefinitionEntity.isSuspended()) {

                if (CommandContextUtil.getEventDispatcher().isEnabled()) {
                    CommandContextUtil.getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.TIMER_FIRED, job));
                }

                // Find initial flow element matching the signal start event
                org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(job.getProcessDefinitionId());
                String activityId = TimerEventHandler.getActivityIdFromConfiguration(configuration);
                if (activityId != null) {
                    FlowElement flowElement = process.getFlowElement(activityId, true);
                    if (flowElement == null) {
                        throw new FlowableException("Could not find matching FlowElement for activityId " + activityId);
                    }
                    ProcessInstanceHelper processInstanceHelper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getProcessInstanceHelper();
                    processInstanceHelper.createAndStartProcessInstanceWithInitialFlowElement(processDefinitionEntity, null, null, flowElement, process, null, null, true);
                } else {
                    new StartProcessInstanceCmd(processDefinitionEntity.getKey(), null, null, null, job.getTenantId()).execute(commandContext);
                }

            } else {
                LOGGER.debug("ignoring timer of suspended process definition {}", processDefinitionEntity.getName());
            }
        } catch (RuntimeException e) {
            LOGGER.error("exception during timer execution", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("exception during timer execution", e);
            throw new FlowableException("exception during timer execution: " + e.getMessage(), e);
        }
    }
}

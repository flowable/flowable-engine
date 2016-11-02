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
package org.activiti5.engine.impl.jobexecutor;

import org.activiti.engine.delegate.event.ActivitiEngineEventType;
import org.activiti.engine.runtime.Job;
import org.activiti5.engine.ActivitiException;
import org.activiti5.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti5.engine.impl.interceptor.CommandContext;
import org.activiti5.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti5.engine.impl.pvm.process.ActivityImpl;
import org.activiti5.engine.logging.LogMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TimerCatchIntermediateEventJobHandler extends TimerEventHandler implements JobHandler {

  private static Logger log = LoggerFactory.getLogger(TimerCatchIntermediateEventJobHandler.class);

  public static final String TYPE = "timer-intermediate-transition";

  public String getType() {
    return TYPE;
  }

  public void execute(Job job, String configuration, ExecutionEntity execution, CommandContext commandContext) {

    String nestedActivityId = TimerEventHandler.getActivityIdFromConfiguration(configuration);

    ActivityImpl intermediateEventActivity = execution.getProcessDefinition().findActivity(nestedActivityId);

    if (intermediateEventActivity == null) {
      throw new ActivitiException("Error while firing timer: intermediate event activity " + nestedActivityId + " not found");
    }

    try {
      if (commandContext.getEventDispatcher().isEnabled()) {
        commandContext.getEventDispatcher().dispatchEvent(
          ActivitiEventBuilder.createEntityEvent(ActivitiEngineEventType.TIMER_FIRED, job));
      }

      if(!execution.getActivity().getId().equals(intermediateEventActivity.getId())) {
        execution.setActivity(intermediateEventActivity);
      }
      execution.signal(null, null);
    } catch (RuntimeException e) {
      LogMDC.putMDCExecution(execution);
      log.error("exception during timer execution", e);
      LogMDC.clear();
      throw e;
    } catch (Exception e) {
      LogMDC.putMDCExecution(execution);
      log.error("exception during timer execution", e);
      LogMDC.clear();
      throw new ActivitiException("exception during timer execution: " + e.getMessage(), e);
    }
  }

}

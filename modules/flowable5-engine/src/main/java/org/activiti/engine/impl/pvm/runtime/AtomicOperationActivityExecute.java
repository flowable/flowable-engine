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
package org.activiti.engine.impl.pvm.runtime;

import org.activiti.engine.ActivitiActivityExecutionException;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.pvm.PvmException;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.logging.LogMDC;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class AtomicOperationActivityExecute implements AtomicOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicOperationActivityExecute.class);

    @Override
    public boolean isAsync(InterpretableExecution execution) {
        return false;
    }

    @Override
    public void execute(InterpretableExecution execution) {
        ActivityImpl activity = (ActivityImpl) execution.getActivity();

        ActivityBehavior activityBehavior = activity.getActivityBehavior();
        if (activityBehavior == null) {
            throw new PvmException("no behavior specified in " + activity);
        }

        LOGGER.debug("{} executes {}: {}", execution, activity, activityBehavior.getClass().getName());

        try {
            if (Context.getProcessEngineConfiguration() != null && Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
                Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                        ActivitiEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_STARTED,
                                execution.getActivity().getId(),
                                (String) execution.getActivity().getProperty("name"),
                                execution.getId(),
                                execution.getProcessInstanceId(),
                                execution.getProcessDefinitionId(),
                                (String) activity.getProperties().get("type"),
                                activity.getActivityBehavior().getClass().getCanonicalName()));
            }

            activityBehavior.execute(execution);

        } catch (ActivitiException e) {
            throw e;
        } catch (Throwable t) {
            LogMDC.putMDCExecution(execution);
            throw new ActivitiActivityExecutionException("couldn't execute activity <" + activity.getProperty("type") + " id=\"" + activity.getId() + "\" ...>: " + t.getMessage(), t);
        }
    }
}

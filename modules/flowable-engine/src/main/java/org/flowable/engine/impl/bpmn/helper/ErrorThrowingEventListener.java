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
package org.flowable.engine.impl.bpmn.helper;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;

/**
 * An {@link FlowableEventListener} that throws a error event when an event is dispatched to it.
 * 
 * @author Frederik Heremans
 * 
 */
public class ErrorThrowingEventListener extends BaseDelegateEventListener {

    protected String errorCode;

    @Override
    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event) && event instanceof FlowableEngineEvent) {

            FlowableEngineEvent engineEvent = (FlowableEngineEvent) event;
            CommandContext commandContext = Context.getCommandContext();

            if (engineEvent.getProcessDefinitionId() != null &&
                    Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, engineEvent.getProcessDefinitionId())) {

                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                compatibilityHandler.throwErrorEvent(event);
                return;
            }

            ExecutionEntity execution = null;

            if (engineEvent.getExecutionId() != null) {
                // Get the execution based on the event's execution ID instead
                execution = CommandContextUtil.getExecutionEntityManager().findById(engineEvent.getExecutionId());
            }

            if (execution == null) {
                throw new FlowableException("No execution context active and event (" + event + ") is not related to an execution. No compensation event can be thrown.");
            }

            try {
                ErrorPropagation.propagateError(errorCode, execution);
            } catch (Exception e) {
                throw new FlowableException("Error while propagating error-event for " + execution, e);
            }
        }
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }
}

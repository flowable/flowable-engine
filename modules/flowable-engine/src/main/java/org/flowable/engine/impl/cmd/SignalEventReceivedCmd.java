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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.Execution;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class SignalEventReceivedCmd implements Command<Void> {

    protected final String eventName;
    protected final String executionId;
    protected final Map<String, Object> payload;
    protected final boolean async;
    protected String tenantId;

    public SignalEventReceivedCmd(String eventName, String executionId, Map<String, Object> processVariables, String tenantId) {
        this.eventName = eventName;
        this.executionId = executionId;
        if (processVariables != null) {
            this.payload = new HashMap<>(processVariables);

        } else {
            this.payload = null;
        }
        this.async = false;
        this.tenantId = tenantId;
    }

    public SignalEventReceivedCmd(String eventName, String executionId, boolean async, String tenantId) {
        this.eventName = eventName;
        this.executionId = executionId;
        this.async = async;
        this.payload = null;
        this.tenantId = tenantId;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        List<SignalEventSubscriptionEntity> signalEvents = null;

        EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);
        if (executionId == null) {
            signalEvents = eventSubscriptionEntityManager.findSignalEventSubscriptionsByEventName(eventName, tenantId);
        } else {

            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);

            if (execution == null) {
                throw new FlowableObjectNotFoundException("Cannot find execution with id '" + executionId + "'", Execution.class);
            }

            if (execution.isSuspended()) {
                throw new FlowableException("Cannot throw signal event '" + eventName + "' because execution '" + executionId + "' is suspended");
            }

            if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                compatibilityHandler.signalEventReceived(eventName, executionId, payload, async, tenantId);
                return null;
            }

            signalEvents = eventSubscriptionEntityManager.findSignalEventSubscriptionsByNameAndExecution(eventName, executionId);

            if (signalEvents.isEmpty()) {
                throw new FlowableException("Execution '" + executionId + "' has not subscribed to a signal event with name '" + eventName + "'.");
            }
        }

        for (SignalEventSubscriptionEntity signalEventSubscriptionEntity : signalEvents) {
            // We only throw the event to globally scoped signals.
            // Process instance scoped signals must be thrown within the process itself
            if (signalEventSubscriptionEntity.isGlobalScoped()) {

                if (executionId == null && Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, signalEventSubscriptionEntity.getProcessDefinitionId())) {
                    Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                    compatibilityHandler.signalEventReceived(signalEventSubscriptionEntity, payload, async);

                } else {
                    CommandContextUtil.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                            FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNALED, signalEventSubscriptionEntity.getActivityId(), eventName,
                                    payload, signalEventSubscriptionEntity.getExecutionId(), signalEventSubscriptionEntity.getProcessInstanceId(),
                                    signalEventSubscriptionEntity.getProcessDefinitionId()));

                    eventSubscriptionEntityManager.eventReceived(signalEventSubscriptionEntity, payload, async);
                }
            }
        }

        return null;
    }

}

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
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.event.MessageEventHandler;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class MessageEventReceivedCmd extends NeedsActiveExecutionCmd<Void> {

    private static final long serialVersionUID = 1L;

    protected final Map<String, Object> payload;
    protected final String messageName;
    protected final boolean async;

    public MessageEventReceivedCmd(String messageName, String executionId, Map<String, Object> processVariables) {
        super(executionId);
        this.messageName = messageName;

        if (processVariables != null) {
            this.payload = new HashMap<>(processVariables);

        } else {
            this.payload = null;
        }
        this.async = false;
    }

    public MessageEventReceivedCmd(String messageName, String executionId, boolean async) {
        super(executionId);
        this.messageName = messageName;
        this.payload = null;
        this.async = async;
    }

    @Override
    protected Void execute(CommandContext commandContext, ExecutionEntity execution) {
        if (messageName == null) {
            throw new FlowableIllegalArgumentException("messageName cannot be null");
        }

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            compatibilityHandler.messageEventReceived(messageName, executionId, payload, async);
            return null;
        }

        EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);
        List<EventSubscriptionEntity> eventSubscriptions = eventSubscriptionEntityManager.findEventSubscriptionsByNameAndExecution(MessageEventHandler.EVENT_HANDLER_TYPE, messageName, executionId);

        if (eventSubscriptions.isEmpty()) {
            throw new FlowableException("Execution with id '" + executionId + "' does not have a subscription to a message event with name '" + messageName + "'");
        }

        // there can be only one:
        EventSubscriptionEntity eventSubscriptionEntity = eventSubscriptions.get(0);
        eventSubscriptionEntityManager.eventReceived(eventSubscriptionEntity, payload, async);

        return null;
    }

}

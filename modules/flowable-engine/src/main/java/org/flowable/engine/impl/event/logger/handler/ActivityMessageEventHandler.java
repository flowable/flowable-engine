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
package org.flowable.engine.impl.event.logger.handler;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.event.FlowableMessageEvent;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;

/**
 * @author Joram Barrez
 */
public class ActivityMessageEventHandler extends AbstractDatabaseEventLoggerEventHandler {

    @Override
    public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
        FlowableMessageEvent messageEvent = (FlowableMessageEvent) event;

        Map<String, Object> data = new HashMap<>();
        putInMapIfNotNull(data, Fields.ACTIVITY_ID, messageEvent.getActivityId());
        putInMapIfNotNull(data, Fields.ACTIVITY_NAME, messageEvent.getActivityName());
        putInMapIfNotNull(data, Fields.PROCESS_DEFINITION_ID, messageEvent.getProcessDefinitionId());
        putInMapIfNotNull(data, Fields.PROCESS_INSTANCE_ID, messageEvent.getProcessInstanceId());
        putInMapIfNotNull(data, Fields.EXECUTION_ID, messageEvent.getExecutionId());
        putInMapIfNotNull(data, Fields.ACTIVITY_TYPE, messageEvent.getActivityType());

        putInMapIfNotNull(data, Fields.MESSAGE_NAME, messageEvent.getMessageName());
        putInMapIfNotNull(data, Fields.MESSAGE_DATA, messageEvent.getMessageData());

        return createEventLogEntry(messageEvent.getProcessDefinitionId(), messageEvent.getProcessInstanceId(), messageEvent.getExecutionId(), null, data);
    }

}

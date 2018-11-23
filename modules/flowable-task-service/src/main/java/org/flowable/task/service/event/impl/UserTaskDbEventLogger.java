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

package org.flowable.task.service.event.impl;

import java.util.Collections;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.runtime.ClockReader;
import org.flowable.task.service.impl.persistence.entity.TaskLogEntryEntity;
import org.flowable.task.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author martin.grofcik
 */
public class UserTaskDbEventLogger extends AbstractFlowableEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTaskDbEventLogger.class);

    protected ClockReader clock;
    protected ObjectMapper objectMapper;

    public UserTaskDbEventLogger(ClockReader clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableUserTaskEvent) {
            FlowableUserTaskEvent userTaskEvent = (FlowableUserTaskEvent) event;
            TaskLogEntryEntity taskLogEntry = createInitialTaskLogEntry(userTaskEvent);
            if (event instanceof FlowableUserTaskCreatedEvent) {
                FlowableUserTaskCreatedEvent userTaskCreatedEvent = (FlowableUserTaskCreatedEvent) event;
                taskLogEntry.setTimeStamp(userTaskCreatedEvent.getTask().getCreateTime());
                taskLogEntry.setData(serializeLogEntryData(userTaskCreatedEvent.getTask()));
            } else if (event instanceof FlowableUserTaskAssigneeChangedEvent) {
                taskLogEntry.setData(
                    serializeLogEntryData(
                        Collections.singletonMap("newAssigneeId", userTaskEvent.getTask().getAssignee())
                    )
                );
            }
            CommandContextUtil.getTaskLogEntryEntityManager().insert(taskLogEntry);
        }
    }

    protected byte[] serializeLogEntryData(Object dataToSerialize) {
        try {
            return objectMapper.writeValueAsBytes(dataToSerialize);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Could not serialize user task event data. Data will not be written to the database", e);
        }
        return null;
    }

    protected TaskLogEntryEntity createInitialTaskLogEntry(FlowableUserTaskEvent userTaskCreatedEvent) {
        TaskLogEntryEntity taskLogEntity = CommandContextUtil.getTaskLogEntryEntityManager().create();
        taskLogEntity.setTaskId(userTaskCreatedEvent.getTask().getId());
        taskLogEntity.setTimeStamp(clock.getCurrentTime());
        taskLogEntity.setType(userTaskCreatedEvent.getType().name());
        taskLogEntity.setUserId(Authentication.getAuthenticatedUserId());
        return taskLogEntity;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}

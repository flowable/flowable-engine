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

package org.flowable.task.service.impl.event;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.task.service.event.impl.FlowableUserTaskCreatedEvent;
import org.flowable.task.service.impl.persistence.entity.TaskLogEntryEntity;
import org.flowable.task.service.impl.util.CommandContextUtil;

/**
 * @author martin.grofcik
 */
public class UserTaskDbEventLogger extends AbstractFlowableEventListener {

    @Override
    public void onEvent(FlowableEvent event) {
        if (event instanceof FlowableUserTaskCreatedEvent) {
            FlowableUserTaskCreatedEvent userTaskCreatedEvent = (FlowableUserTaskCreatedEvent) event;
            TaskLogEntryEntity taskLogEntity = CommandContextUtil.getTaskLogEntryEntityManager().create();
            taskLogEntity.setTaskId(userTaskCreatedEvent.getTask().getId());
            taskLogEntity.setTimeStamp(userTaskCreatedEvent.getTask().getCreateTime());
            taskLogEntity.setType(userTaskCreatedEvent.getType().name());
            taskLogEntity.setUserId(Authentication.getAuthenticatedUserId());
            CommandContextUtil.getTaskLogEntryEntityManager().insert(taskLogEntity);
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}

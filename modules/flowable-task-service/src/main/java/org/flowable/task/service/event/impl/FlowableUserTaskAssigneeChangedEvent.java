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

import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author martin.grofcik
 */
public class FlowableUserTaskAssigneeChangedEvent extends FlowableUserTaskEvent {

    public static final String USER_TASK_ASSIGNEE_CHANGED = "USER_TASK_ASSIGNEE_CHANGED";

    public FlowableUserTaskAssigneeChangedEvent(TaskEntity task) {
        super(task);
    }

    @Override
    public FlowableEventType getType() {
        return () -> USER_TASK_ASSIGNEE_CHANGED;
    }
}

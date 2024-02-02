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
package org.flowable.task.api.history;

import org.flowable.common.engine.api.delegate.event.FlowableEventType;

/**
 * @author martin.grofcik
 */
public enum HistoricTaskLogEntryType implements FlowableEventType {
    USER_TASK_COMPLETED,
    USER_TASK_ASSIGNEE_CHANGED,
    USER_TASK_CREATED,
    USER_TASK_OWNER_CHANGED,
    USER_TASK_PRIORITY_CHANGED,
    USER_TASK_DUEDATE_CHANGED,
    USER_TASK_NAME_CHANGED,
    USER_TASK_SUSPENSIONSTATE_CHANGED,
    USER_TASK_IDENTITY_LINK_ADDED,
    USER_TASK_IDENTITY_LINK_REMOVED
}

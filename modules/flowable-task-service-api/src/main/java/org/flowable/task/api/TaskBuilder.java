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
package org.flowable.task.api;

import java.util.Date;
import java.util.Set;

import org.flowable.identitylink.api.IdentityLinkInfo;

/**
 * Wraps {@link TaskInfo} to the builder.
 * 
 */
public interface TaskBuilder {

    /**
     * Creates task instance according values set in the builder
     * 
     * @return task instance
     */
    Task create();
    
    /**
     * DB id of the task.
     */
    TaskBuilder id(String id);
    String getId();

    /**
     * Name or title of the task.
     */
    TaskBuilder name(String name);
    String getName();

    /**
     * Free text description of the task.
     */
    TaskBuilder description(String description);
    String getDescription();

    /**
     * Indication of how important/urgent this task is
     */
    TaskBuilder priority(int priority);
    int getPriority();

    /**
     * The userId of the person that is responsible for this task.
     */
    TaskBuilder owner(String ownerId);
    String getOwner();

    /**
     * The userId of the person to which this task is delegated.
     */
    TaskBuilder assignee(String assigneId);
    String getAssignee();
    /**
     * Change due date of the task.
     */
    TaskBuilder dueDate(Date dueDate);
    Date getDueDate();

    /**
     * Change the category of the task. This is an optional field and allows to 'tag' tasks as belonging to a certain category.
     */
    TaskBuilder category(String category);
    String getCategory();

    /**
     * the parent task for which this task is a subtask
     */
    TaskBuilder parentTaskId(String parentTaskId);
    String getParentTaskId();

    /**
     * Change the tenantId of the task
     */
    TaskBuilder tenantId(String tenantId);
    String getTenantId();

    /**
     * Change the form key of the task
     */
    TaskBuilder formKey(String formKey);
    String getFormKey();

    /**
     * task definition id to create task from
     */
    TaskBuilder taskDefinitionId(String taskDefinitionId);
    String getTaskDefinitionId();

    /**
     * task definition key to create task from
     */
    TaskBuilder taskDefinitionKey(String taskDefinitionKey);
    String getTaskDefinitionKey();

    /**
     * add identity links to the task
     */
    TaskBuilder identityLinks(Set<? extends IdentityLinkInfo> identityLinks);
    Set<? extends IdentityLinkInfo> getIdentityLinks();

    /**
     * add task scopeId
     */
    TaskBuilder scopeId(String scopeId);
    String getScopeId();

    /**
     * Add scope type
     */
    TaskBuilder scopeType(String scopeType);
    String getScopeType();
}

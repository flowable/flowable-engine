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
import java.util.List;
import java.util.Map;

import org.flowable.identitylink.api.IdentityLinkInfo;

/**
 * @author Joram Barrez
 */
public interface TaskInfo {

    /** DB id of the task. */
    String getId();

    /**
     * Name or title of the task.
     */
    String getName();

    /**
     * Free text description of the task.
     */
    String getDescription();

    /**
     * Indication of how important/urgent this task is
     */
    int getPriority();

    /**
     * The user id of the person that is responsible for this task.
     */
    String getOwner();

    /**
     * The user id of the person to which this task is delegated.
     */
    String getAssignee();

    /**
     * Reference to the process instance or null if it is not related to a process instance.
     */
    String getProcessInstanceId();

    /**
     * Reference to the path of execution or null if it is not related to a process instance.
     */
    String getExecutionId();

    /**
     * Reference to the task definition or null if it is not related to any task definition.
     */
    String getTaskDefinitionId();

    /**
     * Reference to the process definition or null if it is not related to a process.
     */
    String getProcessDefinitionId();
    
    /**
     * Reference to a scope identifier or null if none is set (e.g. for bpmn process task it is null)
     */
    String getScopeId();
    
    /**
     * Reference to a sub scope identifier or null if none is set (e.g. for bpmn process task it is null)
     */
    String getSubScopeId();
    
    /**
     * Reference to a scope type or null if none is set (e.g. for bpmn process task it is null)
     */
    String getScopeType();
    
    /**
     * Reference to a scope definition identifier or null if none is set (e.g. for bpmn process task it is null)
     */
    String getScopeDefinitionId();

    /**
     * If this task runs in the context of a case and stage, this method returns it's closest parent stage instance id (the stage plan item instance id to be
     * precise). Even if the direct parent of the task is a process which itself might have been created out of a process task of a case, its stage instance
     * is reflected in the task.
     *
     * @return the stage instance id this task belongs to or null, if this task is not part of a case at all or is not a child element of a stage
     */
    String getPropagatedStageInstanceId();

    /** The date/time when this task was created */
    Date getCreateTime();

    /**
     * The id of the activity in the process defining this task or null if this is not related to a process
     */
    String getTaskDefinitionKey();

    /**
     * Due date of the task.
     */
    Date getDueDate();

    /**
     * The category of the task. This is an optional field and allows to 'tag' tasks as belonging to a certain category.
     */
    String getCategory();

    /**
     * The parent task for which this task is a subtask
     */
    String getParentTaskId();

    /**
     * The tenant identifier of this task
     */
    String getTenantId();

    /**
     * The form key for the user task
     */
    String getFormKey();

    /**
     * Returns the local task variables if requested in the task query
     */
    Map<String, Object> getTaskLocalVariables();

    /**
     * Returns the process variables if requested in the task query
     */
    Map<String, Object> getProcessVariables();

    /**
     * Returns the case variables if requested in the task query
     */
    Map<String, Object> getCaseVariables();

    /**
     * Returns the identity links.
     */
    List<? extends IdentityLinkInfo> getIdentityLinks();

    /**
     * The claim time of this task
     */
    Date getClaimTime();
}

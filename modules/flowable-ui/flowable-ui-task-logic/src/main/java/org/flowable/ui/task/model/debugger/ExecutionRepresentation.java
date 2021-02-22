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
package org.flowable.ui.task.model.debugger;

/**
 * @author martin.grofcik
 */
public class ExecutionRepresentation {
    
    protected String id;
    protected String parentId;
    protected String processInstanceId;
    protected String superExecutionId;
    protected String activityId;
    protected Boolean suspended;
    protected String tenantId;

    public ExecutionRepresentation(String id, String parentId, String processInstanceId, String superExecutionId, 
                                   String activityId, boolean suspended, String tenantId) {
        this.id = id;
        this.parentId = parentId;
        this.processInstanceId = processInstanceId;
        this.superExecutionId = superExecutionId;
        this.activityId = activityId;
        this.suspended = suspended;
        this.tenantId = tenantId;
    }

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getSuperExecutionId() {
        return superExecutionId;
    }

    public String getActivityId() {
        return activityId;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String getTenantId() {
        return tenantId;
    }
}

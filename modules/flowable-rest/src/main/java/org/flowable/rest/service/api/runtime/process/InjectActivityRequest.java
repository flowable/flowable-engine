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

package org.flowable.rest.service.api.runtime.process;

/**
 * @author Tijs Rademakers
 */
public class InjectActivityRequest {

    protected String injectionType;
    protected String id;
    protected String name;
    protected String assignee;
    protected String taskId;
    protected String processDefinitionId;
    protected boolean joinParallelActivitiesOnComplete = true;
    
    public String getInjectionType() {
        return injectionType;
    }
    public void setInjectionType(String injectionType) {
        this.injectionType = injectionType;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAssignee() {
        return assignee;
    }
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    public String getTaskId() {
        return taskId;
    }
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }
    public boolean isJoinParallelActivitiesOnComplete() {
        return joinParallelActivitiesOnComplete;
    }
    public void setJoinParallelActivitiesOnComplete(boolean joinParallelActivitiesOnComplete) {
        this.joinParallelActivitiesOnComplete = joinParallelActivitiesOnComplete;
    }
}

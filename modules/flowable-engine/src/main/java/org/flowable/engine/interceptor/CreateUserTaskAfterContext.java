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
package org.flowable.engine.interceptor;

import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class CreateUserTaskAfterContext {

    protected UserTask userTask;
    protected TaskEntity taskEntity;
    protected DelegateExecution execution;
    
    public CreateUserTaskAfterContext() {
        
    }
    
    public CreateUserTaskAfterContext(UserTask userTask, TaskEntity taskEntity, DelegateExecution execution) {
        this.userTask = userTask;
        this.taskEntity = taskEntity;
        this.execution = execution;
    }

    public UserTask getUserTask() {
        return userTask;
    }

    public void setUserTask(UserTask userTask) {
        this.userTask = userTask;
    }

    public TaskEntity getTaskEntity() {
        return taskEntity;
    }

    public void setTaskEntity(TaskEntity taskEntity) {
        this.taskEntity = taskEntity;
    }

    public DelegateExecution getExecution() {
        return execution;
    }

    public void setExecution(DelegateExecution execution) {
        this.execution = execution;
    }
}

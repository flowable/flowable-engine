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
package org.flowable.cmmn.engine.interceptor;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class CreateHumanTaskAfterContext {

    protected HumanTask humanTask;
    protected TaskEntity taskEntity;
    protected PlanItemInstanceEntity planItemInstanceEntity;
    
    public CreateHumanTaskAfterContext() {
        
    }
    
    public CreateHumanTaskAfterContext(HumanTask humanTask, TaskEntity taskEntity, PlanItemInstanceEntity planItemInstanceEntity) {
        this.humanTask = humanTask;
        this.taskEntity = taskEntity;
        this.planItemInstanceEntity = planItemInstanceEntity;
    }

    public HumanTask getHumanTask() {
        return humanTask;
    }

    public void setHumanTask(HumanTask humanTask) {
        this.humanTask = humanTask;
    }

    public TaskEntity getTaskEntity() {
        return taskEntity;
    }

    public void setTaskEntity(TaskEntity taskEntity) {
        this.taskEntity = taskEntity;
    }

    public PlanItemInstanceEntity getPlanItemInstanceEntity() {
        return planItemInstanceEntity;
    }

    public void setPlanItemInstanceEntity(PlanItemInstanceEntity planItemInstanceEntity) {
        this.planItemInstanceEntity = planItemInstanceEntity;
    }
}

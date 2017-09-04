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

package org.flowable.engine.impl.form;

import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Tom Baeyens
 */
public class DefaultTaskFormHandler extends DefaultFormHandler implements TaskFormHandler {

    @Override
    public TaskFormData createTaskForm(TaskEntity task) {
        TaskFormDataImpl taskFormData = new TaskFormDataImpl();
        
        ExecutionEntity executionEntity = null;
        if (task.getExecutionId() != null) {
            executionEntity = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
        }
        
        if (formKey != null) {
            Object formValue = formKey.getValue(executionEntity);
            if (formValue != null) {
                taskFormData.setFormKey(formValue.toString());
            }
        }
        taskFormData.setDeploymentId(deploymentId);
        taskFormData.setTask(task);
        initializeFormProperties(taskFormData, executionEntity);
        return taskFormData;
    }
}

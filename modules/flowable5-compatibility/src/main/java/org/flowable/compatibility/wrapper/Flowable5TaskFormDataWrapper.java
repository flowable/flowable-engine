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

package org.flowable.compatibility.wrapper;

import java.util.List;

import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.TaskFormData;
import org.flowable.task.api.Task;

/**
 * Wraps an v5 task form data to an v6 {@link TaskFormData}.
 * 
 * @author Tijs Rademakers
 */
public class Flowable5TaskFormDataWrapper implements TaskFormData {

    private org.activiti.engine.form.TaskFormData activiti5TaskFormData;

    public Flowable5TaskFormDataWrapper(org.activiti.engine.form.TaskFormData activiti5TaskFormData) {
        this.activiti5TaskFormData = activiti5TaskFormData;
    }

    @Override
    public String getFormKey() {
        return activiti5TaskFormData.getFormKey();
    }

    @Override
    public String getDeploymentId() {
        return activiti5TaskFormData.getDeploymentId();
    }

    @Override
    public List<FormProperty> getFormProperties() {
        return activiti5TaskFormData.getFormProperties();
    }

    @Override
    public Task getTask() {
        if (activiti5TaskFormData.getTask() != null) {
            return new Flowable5TaskWrapper(activiti5TaskFormData.getTask());
        }
        
        return null;
    }

}

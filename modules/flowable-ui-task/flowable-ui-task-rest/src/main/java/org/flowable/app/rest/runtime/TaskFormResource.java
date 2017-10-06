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
package org.flowable.app.rest.runtime;

import java.util.List;

import org.flowable.app.model.runtime.CompleteFormRepresentation;
import org.flowable.app.model.runtime.ProcessInstanceVariableRepresentation;
import org.flowable.app.model.runtime.SaveFormRepresentation;
import org.flowable.app.service.runtime.FlowableTaskFormService;
import org.flowable.form.model.FormModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Joram Barrez
 */
@RestController
@RequestMapping("/rest/task-forms")
public class TaskFormResource {

    @Autowired
    protected FlowableTaskFormService taskFormService;

    @RequestMapping(value = "/{taskId}", method = RequestMethod.GET, produces = "application/json")
    public FormModel getTaskForm(@PathVariable String taskId) {
        return taskFormService.getTaskForm(taskId);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/{taskId}", method = RequestMethod.POST, produces = "application/json")
    public void completeTaskForm(@PathVariable String taskId, @RequestBody CompleteFormRepresentation completeTaskFormRepresentation) {
        taskFormService.completeTaskForm(taskId, completeTaskFormRepresentation);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/{taskId}/save-form", method = RequestMethod.POST, produces = "application/json")
    public void saveTaskForm(@PathVariable String taskId, @RequestBody SaveFormRepresentation saveFormRepresentation) {
        taskFormService.saveTaskForm(taskId, saveFormRepresentation);
    }

    @RequestMapping(value = "/{taskId}/variables", method = RequestMethod.GET, produces = "application/json")
    public List<ProcessInstanceVariableRepresentation> getProcessInstanceVariables(@PathVariable String taskId) {
        return taskFormService.getProcessInstanceVariables(taskId);
    }
}

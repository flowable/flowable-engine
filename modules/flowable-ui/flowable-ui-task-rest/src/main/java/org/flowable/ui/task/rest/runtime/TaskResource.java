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
package org.flowable.ui.task.rest.runtime;

import org.flowable.ui.task.model.runtime.TaskRepresentation;
import org.flowable.ui.task.model.runtime.TaskUpdateRepresentation;
import org.flowable.ui.task.service.runtime.FlowableTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app")
public class TaskResource {

    @Autowired
    protected FlowableTaskService taskService;

    @GetMapping(value = "/rest/tasks/{taskId}", produces = "application/json")
    public TaskRepresentation getTask(@PathVariable String taskId   ) {
        return taskService.getTask(taskId);
    }

    @PutMapping(value = "/rest/tasks/{taskId}", produces = "application/json")
    public TaskRepresentation updateTask(@PathVariable("taskId") String taskId, @RequestBody TaskUpdateRepresentation updated) {
        return taskService.updateTask(taskId, updated);
    }

    @GetMapping(value = "/rest/tasks/{taskId}/subtasks", produces = "application/json")
    public List<TaskRepresentation> getSubTasks(@PathVariable String taskId) {
        return taskService.getSubTasks(taskId);
    }

}

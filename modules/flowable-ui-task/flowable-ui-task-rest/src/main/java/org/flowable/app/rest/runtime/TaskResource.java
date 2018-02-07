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

import org.flowable.app.model.runtime.TaskRepresentation;
import org.flowable.app.model.runtime.TaskUpdateRepresentation;
import org.flowable.app.service.runtime.FlowableTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TaskResource {

    @Autowired
    protected FlowableTaskService taskService;

    @RequestMapping(value = "/rest/tasks/{taskId}", method = RequestMethod.GET, produces = "application/json")
    public TaskRepresentation getTask(@PathVariable String taskId   ) {
        return taskService.getTask(taskId);
    }

    @RequestMapping(value = "/rest/tasks/{taskId}", method = RequestMethod.PUT, produces = "application/json")
    public TaskRepresentation updateTask(@PathVariable("taskId") String taskId, @RequestBody TaskUpdateRepresentation updated) {
        return taskService.updateTask(taskId, updated);
    }

    @RequestMapping(value = "/rest/tasks/{taskId}/subtasks", method = RequestMethod.GET, produces = "application/json")
    public List<TaskRepresentation> getSubTasks(@PathVariable String taskId) {
        return taskService.getSubTasks(taskId);
    }

}

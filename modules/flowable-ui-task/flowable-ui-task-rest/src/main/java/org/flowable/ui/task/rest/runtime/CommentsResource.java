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

import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.task.model.runtime.CommentRepresentation;
import org.flowable.ui.task.service.runtime.FlowableCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource related to comment collection on tasks and process instances.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 */
@RestController
@RequestMapping("/app")
public class CommentsResource {

    @Autowired
    protected FlowableCommentService commentService;

    @RequestMapping(value = "/rest/tasks/{taskId}/comments", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getTaskComments(@PathVariable("taskId") String taskId) {
        return commentService.getTaskComments(taskId);
    }

    @RequestMapping(value = "/rest/tasks/{taskId}/comments", method = RequestMethod.POST, produces = "application/json")
    public CommentRepresentation addTaskComment(@RequestBody CommentRepresentation commentRequest, @PathVariable("taskId") String taskId) {
        return commentService.addTaskComment(commentRequest, taskId);
    }

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}/comments", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getProcessInstanceComments(@PathVariable("processInstanceId") String processInstanceId) {
        return commentService.getProcessInstanceComments(processInstanceId);
    }

    @RequestMapping(value = "/rest/process-instances/{processInstanceId}/comments", method = RequestMethod.POST, produces = "application/json")
    public CommentRepresentation addProcessInstanceComment(@RequestBody CommentRepresentation commentRequest,
            @PathVariable("processInstanceId") String processInstanceId) {
        return commentService.addProcessInstanceComment(commentRequest, processInstanceId);
    }

}

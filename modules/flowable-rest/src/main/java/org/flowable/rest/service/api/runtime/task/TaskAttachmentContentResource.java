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

package org.flowable.rest.service.api.runtime.task;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.task.Attachment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Task Attachments" }, description = "Manage Tasks Attachments", authorizations = { @Authorization(value = "basicAuth") })
public class TaskAttachmentContentResource extends TaskBaseResource {

    @ApiOperation(value = "Get the content for an attachment", tags = { "Task Attachments" },
            notes = "The response body contains the binary content. By default, the content-type of the response is set to application/octet-stream unless the attachment type contains a valid Content-type.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task and attachment was found and the requested content is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found or the task doesn’t have an attachment with the given id or the attachment doesn’t have a binary stream available. Status message provides additional information.")
    })
    @GetMapping(value = "/runtime/tasks/{taskId}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> getAttachmentContent(@ApiParam(name = "taskId") @PathVariable("taskId") String taskId, @ApiParam(name = "attachmentId") @PathVariable("attachmentId") String attachmentId, HttpServletResponse response) {

        HistoricTaskInstance task = getHistoricTaskFromRequest(taskId);
        Attachment attachment = taskService.getAttachment(attachmentId);

        if (attachment == null || !task.getId().equals(attachment.getTaskId())) {
            throw new FlowableObjectNotFoundException("Task '" + task.getId() + "' doesn't have an attachment with id '" + attachmentId + "'.", Attachment.class);
        }

        InputStream attachmentStream = taskService.getAttachmentContent(attachmentId);
        if (attachmentStream == null) {
            throw new FlowableObjectNotFoundException("Attachment with id '" + attachmentId + "' doesn't have content associated with it.", Attachment.class);
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        MediaType mediaType = null;
        if (attachment.getType() != null) {
            try {
                mediaType = MediaType.valueOf(attachment.getType());
                responseHeaders.set("Content-Type", attachment.getType());
            } catch (Exception e) {
                // ignore if unknown media type
            }
        }

        if (mediaType == null) {
            responseHeaders.set("Content-Type", "application/octet-stream");
        }

        try {
            return new ResponseEntity<>(IOUtils.toByteArray(attachmentStream), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            throw new FlowableException("Error creating attachment data", e);
        }
    }
}

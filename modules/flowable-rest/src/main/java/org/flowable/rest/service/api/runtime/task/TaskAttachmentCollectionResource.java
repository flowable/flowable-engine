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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.task.Attachment;
import org.flowable.rest.service.api.engine.AttachmentRequest;
import org.flowable.rest.service.api.engine.AttachmentResponse;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Frederik Heremans
 */
@RestController
@Api(tags = { "Task Attachments"}, description = "Manage Tasks Attachments", authorizations = { @Authorization(value = "basicAuth") })
public class TaskAttachmentCollectionResource extends TaskBaseResource {

    @Autowired
    protected ObjectMapper objectMapper;

    @ApiOperation(value = "List attachments on a task", nickname="listTaskAttachments", tags = { "Task Attachments" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the task was found and the attachments are returned."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @GetMapping(value = "/runtime/tasks/{taskId}/attachments", produces = "application/json")
    public List<AttachmentResponse> getAttachments(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletRequest request) {
        List<AttachmentResponse> result = new ArrayList<>();
        HistoricTaskInstance task = getHistoricTaskFromRequest(taskId);

        for (Attachment attachment : taskService.getTaskAttachments(task.getId())) {
            result.add(restResponseFactory.createAttachmentResponse(attachment));
        }

        return result;
    }

    // FIXME OASv3 to solve Multiple Endpoint issue
    @ApiOperation(value = "Create a new attachment on a task, containing a link to an external resource or an attached file", tags = { "Task Attachments" },
            notes = "This endpoint can be used in 2 ways: By passing a JSON Body (AttachmentRequest) to link an external resource or by passing a multipart/form-data Object to attach a file.\n"
                    + "NB: Swagger V2 specification doesn't support this use case that's why this endpoint might be buggy/incomplete if used with other tools.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "body", type = "org.flowable.rest.service.api.engine.AttachmentRequest", value = "create an attachment containing a link to an external resource", paramType = "body", example = "{\n" + "  \"name\":\"Simple attachment\",\n" + "  \"description\":\"Simple attachment description\",\n"
                    + "  \"type\":\"simpleType\",\n" + "  \"externalUrl\":\"http://flowable.org\"\n" + "}"),
            @ApiImplicitParam(name = "file", dataType = "file", value = "Attachment file", paramType = "form"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Required name of the variable", paramType = "form", example = "Simple attachment"),
            @ApiImplicitParam(name = "description", dataType = "string", value = "Description of the attachment, optional", paramType = "form", example = "Simple attachment description"),
            @ApiImplicitParam(name = "type", dataType = "string", value = "Type of attachment, optional. Supports any arbitrary string or a valid HTTP content-type.", paramType = "form", example = "simpleType")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the attachment was created and the result is returned."),
            @ApiResponse(code = 400, message = "Indicates the attachment name is missing from the request."),
            @ApiResponse(code = 404, message = "Indicates the requested task was not found.")
    })
    @PostMapping(value = "/runtime/tasks/{taskId}/attachments", produces = "application/json", consumes = {"application/json", "multipart/form-data"})
    public AttachmentResponse createAttachment(@ApiParam(name = "taskId") @PathVariable String taskId, HttpServletRequest request, HttpServletResponse response) {

        AttachmentResponse result = null;
        Task task = getTaskFromRequest(taskId);
        if (request instanceof MultipartHttpServletRequest) {
            result = createBinaryAttachment((MultipartHttpServletRequest) request, task, response);
        } else {

            AttachmentRequest attachmentRequest = null;
            try {
                attachmentRequest = objectMapper.readValue(request.getInputStream(), AttachmentRequest.class);

            } catch (Exception e) {
                throw new FlowableIllegalArgumentException("Failed to serialize to a AttachmentRequest instance", e);
            }

            if (attachmentRequest == null) {
                throw new FlowableIllegalArgumentException("AttachmentRequest properties not found in request");
            }

            result = createSimpleAttachment(attachmentRequest, task);
        }

        response.setStatus(HttpStatus.CREATED.value());
        return result;
    }

    protected AttachmentResponse createSimpleAttachment(AttachmentRequest attachmentRequest, Task task) {

        if (attachmentRequest.getName() == null) {
            throw new FlowableIllegalArgumentException("Attachment name is required.");
        }

        Attachment createdAttachment = taskService.createAttachment(attachmentRequest.getType(), task.getId(), task.getProcessInstanceId(), attachmentRequest.getName(),
                attachmentRequest.getDescription(), attachmentRequest.getExternalUrl());

        return restResponseFactory.createAttachmentResponse(createdAttachment);
    }

    protected AttachmentResponse createBinaryAttachment(MultipartHttpServletRequest request, Task task, HttpServletResponse response) {

        String name = null;
        String description = null;
        String type = null;

        Map<String, String[]> paramMap = request.getParameterMap();
        for (String parameterName : paramMap.keySet()) {
            if (paramMap.get(parameterName).length > 0) {

                if (parameterName.equalsIgnoreCase("name")) {
                    name = paramMap.get(parameterName)[0];

                } else if (parameterName.equalsIgnoreCase("description")) {
                    description = paramMap.get(parameterName)[0];

                } else if (parameterName.equalsIgnoreCase("type")) {
                    type = paramMap.get(parameterName)[0];
                }
            }
        }

        if (name == null) {
            throw new FlowableIllegalArgumentException("Attachment name is required.");
        }

        if (request.getFileMap().size() == 0) {
            throw new FlowableIllegalArgumentException("Attachment content is required.");
        }

        MultipartFile file = request.getFileMap().values().iterator().next();

        if (file == null) {
            throw new FlowableIllegalArgumentException("Attachment content is required.");
        }

        try {
            Attachment createdAttachment = taskService.createAttachment(type, task.getId(), task.getProcessInstanceId(), name, description, file.getInputStream());

            response.setStatus(HttpStatus.CREATED.value());
            return restResponseFactory.createAttachmentResponse(createdAttachment);

        } catch (Exception e) {
            throw new FlowableException("Error creating attachment response", e);
        }
    }
}

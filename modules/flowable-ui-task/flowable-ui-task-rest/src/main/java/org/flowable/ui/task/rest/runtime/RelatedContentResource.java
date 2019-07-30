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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.task.model.runtime.ContentItemRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Frederik Heremans
 */
@RestController
@RequestMapping("/app")
public class RelatedContentResource extends AbstractRelatedContentResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelatedContentResource.class);

    protected ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/rest/tasks/{taskId}/content")
    public ResultListDataRepresentation getContentItemsForTask(@PathVariable("taskId") String taskId) {
        return super.getContentItemsForTask(taskId);
    }

    @GetMapping(value = "/rest/process-instances/{processInstanceId}/content")
    public ResultListDataRepresentation getContentItemsForProcessInstance(@PathVariable("processInstanceId") String processInstanceId) {
        return super.getContentItemsForProcessInstance(processInstanceId);
    }

    @GetMapping(value = "/rest/case-instances/{caseInstanceId}/content")
    public ResultListDataRepresentation getContentItemsForCase(@PathVariable("caseInstanceId") String caseInstanceId) {
        return super.getContentItemsForCase(caseInstanceId);
    }

    @PostMapping(value = "/rest/tasks/{taskId}/raw-content")
    public ContentItemRepresentation createContentItemOnTask(@PathVariable("taskId") String taskId, @RequestParam("file") MultipartFile file) {
        return super.createContentItemOnTask(taskId, file);
    }

    /*
     * specific endpoint for IE9 flash upload component
     */
    @PostMapping(value = "/rest/tasks/{taskId}/raw-content/text")
    public String createContentItemOnTaskText(@PathVariable("taskId") String taskId, @RequestParam("file") MultipartFile file) {
        ContentItemRepresentation contentItem = super.createContentItemOnTask(taskId, file);
        String contentItemJson = null;
        try {
            contentItemJson = objectMapper.writeValueAsString(contentItem);
        } catch (Exception e) {
            LOGGER.error("Error while processing ContentItem representation json", e);
            throw new InternalServerErrorException("ContentItem on task could not be saved");
        }

        return contentItemJson;
    }

    @PostMapping(value = "/rest/tasks/{taskId}/content")
    public ContentItemRepresentation createContentItemOnTask(@PathVariable("taskId") String taskId, @RequestBody ContentItemRepresentation contentItem) {
        return super.createContentItemOnTask(taskId, contentItem);
    }

    @PostMapping(value = "/rest/processes/{processInstanceId}/content")
    public ContentItemRepresentation createContentItemOnProcessInstance(@PathVariable("processInstanceId") String processInstanceId, @RequestBody ContentItemRepresentation contentItem) {
        return super.createContentItemOnProcessInstance(processInstanceId, contentItem);
    }

    @PostMapping(value = "/rest/process-instances/{processInstanceId}/raw-content")
    public ContentItemRepresentation createContentItemOnProcessInstance(@PathVariable("processInstanceId") String processInstanceId, @RequestParam("file") MultipartFile file) {
        return super.createContentItemOnProcessInstance(processInstanceId, file);
    }

    /*
     * specific endpoint for IE9 flash upload component
     */
    @PostMapping(value = "/rest/process-instances/{processInstanceId}/raw-content/text")
    public String createContentItemOnProcessInstanceText(@PathVariable("processInstanceId") String processInstanceId, @RequestParam("file") MultipartFile file) {
        ContentItemRepresentation contentItem = super.createContentItemOnProcessInstance(processInstanceId, file);

        String contentItemJson = null;
        try {
            contentItemJson = objectMapper.writeValueAsString(contentItem);
        } catch (Exception e) {
            LOGGER.error("Error while processing ContentItem representation json", e);
            throw new InternalServerErrorException("ContentItem on process instance could not be saved");
        }

        return contentItemJson;
    }

    @PostMapping(value = "/rest/case-instances/{caseId}/raw-content")
    public ContentItemRepresentation createContentItemOnCase(@PathVariable("caseId") String caseId, @RequestParam("file") MultipartFile file) {
        return super.createContentItemOnCase(caseId, file);
    }

    /*
     * specific endpoint for IE9 flash upload component
     */
    @PostMapping(value = "/rest/case-instances/{caseId}/raw-content/text")
    public String createContentItemOnCaseText(@PathVariable("caseId") String caseId, @RequestParam("file") MultipartFile file) {
        ContentItemRepresentation contentItem = super.createContentItemOnCase(caseId, file);

        String contentItemJson = null;
        try {
            contentItemJson = objectMapper.writeValueAsString(contentItem);
        } catch (Exception e) {
            LOGGER.error("Error while processing ContentItem representation json", e);
            throw new InternalServerErrorException("ContentItem on process instance could not be saved");
        }

        return contentItemJson;
    }

    @PostMapping(value = "/rest/content/raw")
    public ContentItemRepresentation createTemporaryRawContentItem(@RequestParam("file") MultipartFile file) {
        return super.createTemporaryRawContentItem(file);
    }

    /*
     * specific endpoint for IE9 flash upload component
     */
    @PostMapping(value = "/rest/content/raw/text")
    public String createTemporaryRawContentItemText(@RequestParam("file") MultipartFile file) {
        ContentItemRepresentation contentItem = super.createTemporaryRawContentItem(file);
        String contentItemJson = null;
        try {
            contentItemJson = objectMapper.writeValueAsString(contentItem);
        } catch (Exception e) {
            LOGGER.error("Error while processing ContentItem representation json", e);
            throw new InternalServerErrorException("ContentItem could not be saved");
        }

        return contentItemJson;
    }

    @PostMapping(value = "/rest/content")
    public ContentItemRepresentation createTemporaryRelatedContent(@RequestBody ContentItemRepresentation contentItem) {
        return addContentItem(contentItem, null, null, false);
    }

    @DeleteMapping(value = "/rest/content/{contentId}")
    public void deleteContent(@PathVariable("contentId") String contentId, HttpServletResponse response) {
        super.deleteContent(contentId, response);
    }

    @GetMapping(value = "/rest/content/{contentId}")
    public ContentItemRepresentation getContent(@PathVariable("contentId") String contentId) {
        return super.getContent(contentId);
    }

    @GetMapping(value = "/rest/content/{contentId}/raw")
    public void getRawContent(@PathVariable("contentId") String contentId, HttpServletResponse response) {
        super.getRawContent(contentId, response);
    }

}

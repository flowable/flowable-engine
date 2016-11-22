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
package org.activiti.rest.content.service.api.content;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.content.api.ContentItem;
import org.activiti.content.api.ContentService;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.rest.content.ContentRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Content item" }, description = "Manages Content item")
public class ContentItemCollectionResource {

  @Autowired
  protected ContentService contentService;

  @Autowired
  protected ContentRestResponseFactory contentRestResponseFactory;

  @Autowired
  protected ObjectMapper objectMapper;
  
  @ApiOperation(value = "Create a new content item, with content item information and an optional attached file", tags = {"Content item"},
      notes="## Create a new content item, with content item information\n\n"
              + " ```JSON\n" + "{\n" + "  \"name\":\"Simple content item\",\n" + "  \"mimeType\":\"application/pdf\",\n"
              + "  \"taskId\":\"12345\",\n" + "  \"processInstanceId\":\"1234\"\n" 
              + "  \"contentStoreId\":\"5678\",\n" + "  \"contentStoreName\":\"myFileStore\"\n" 
              + "  \"field\":\"uploadField\",\n" + "  \"createdBy\":\"johndoe\"\n" 
              + "  \"lastModifiedBy\":\"johndoe\",\n" + "  \"tenantId\":\"myTenantId\"\n" + "} ```"
              + "\n\n\n"
              + "Only the content item name is required to create a new content item.\n"
              + "\n\n\n"
              + "## Create a new content item with an attached file\n\n"
              + "The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the variable. On top of that, the following additional form-fields can be present:\n"
              + "\n"
              + "- *name*: Required name of the content item.\n\n"
              + "- *mimeType*: Mime type of the content item, optional.\n\n"
              + "- *taskId*: Task identifier for the content item, optional.\n\n"
              + "- *processInstanceId*: Process instance identifier for the content item, optional.\n\n"
              + "- *contentStoreId*: The identifier of the content item in an external content store, optional.\n\n"
              + "- *contentStoreName*: The name of an external content store, optional.\n\n"
              + "- *field*: The form field for the content item, optional.\n\n"
              + "- *createdBy*: The user identifier that created the content item, optional.\n\n"
              + "- *lastModifiedBy*: The user identifier that last modified the content item, optional.\n\n"
              + "- *tenantId*: The tenant identifier of the content item, optional."
      )
  @ApiResponses(value = {
      @ApiResponse(code = 201, message = "Indicates the content item was created and the result is returned."),
      @ApiResponse(code = 400, message = "Indicates required content item info is missing from the request.")
  })
  @RequestMapping(value = "/content-service/content-items", method = RequestMethod.POST, produces = "application/json")
  public ContentItemResponse createContentItem(HttpServletRequest request, HttpServletResponse response) {
    ContentItemResponse result = null;
    if (request instanceof MultipartHttpServletRequest) {
      result = createBinaryContentItem((MultipartHttpServletRequest) request, response);
    } else {

      ContentItemRequest contentItemRequest = null;
      try {
        contentItemRequest = objectMapper.readValue(request.getInputStream(), ContentItemRequest.class);

      } catch (Exception e) {
        throw new ActivitiIllegalArgumentException("Failed to serialize to a ContentItemRequest instance", e);
      }

      if (contentItemRequest == null) {
        throw new ActivitiIllegalArgumentException("ContentItemRequest properties not found in request");
      }

      result = createSimpleContentItem(contentItemRequest);
    }

    response.setStatus(HttpStatus.CREATED.value());
    return result;
  }

  protected ContentItemResponse createSimpleContentItem(ContentItemRequest contentItemRequest) {

    if (contentItemRequest.getName() == null) {
      throw new ActivitiIllegalArgumentException("Content item name is required.");
    }

    ContentItem contentItem = contentService.newContentItem();
    contentItem.setName(contentItemRequest.getName());
    contentItem.setMimeType(contentItemRequest.getMimeType());
    contentItem.setTaskId(contentItemRequest.getTaskId());
    contentItem.setProcessInstanceId(contentItemRequest.getProcessInstanceId());
    contentItem.setContentStoreId(contentItemRequest.getContentStoreId());
    contentItem.setContentStoreName(contentItemRequest.getContentStoreName());
    contentItem.setField(contentItemRequest.getField());
    contentItem.setCreatedBy(contentItemRequest.getCreatedBy());
    contentItem.setLastModifiedBy(contentItemRequest.getLastModifiedBy());
    contentItem.setTenantId(contentItemRequest.getTenantId());
    contentService.saveContentItem(contentItem);

    return contentRestResponseFactory.createContentItemResponse(contentItem);
  }

  protected ContentItemResponse createBinaryContentItem(MultipartHttpServletRequest request, HttpServletResponse response) {
    ContentItem contentItem = contentService.newContentItem();
    
    Map<String, String[]> paramMap = request.getParameterMap();
    for (String parameterName : paramMap.keySet()) {
      if (paramMap.get(parameterName).length > 0) {

        if (parameterName.equalsIgnoreCase("name")) {
          contentItem.setName(paramMap.get(parameterName)[0]);

        } else if (parameterName.equalsIgnoreCase("mimeType")) {
          contentItem.setMimeType(paramMap.get(parameterName)[0]);

        } else if (parameterName.equalsIgnoreCase("taskId")) {
          contentItem.setTaskId(paramMap.get(parameterName)[0]);
        
        } else if (parameterName.equalsIgnoreCase("processInstanceId")) {
          contentItem.setProcessInstanceId(paramMap.get(parameterName)[0]);
        
        } else if (parameterName.equalsIgnoreCase("contentStoreId")) {
          contentItem.setContentStoreId(paramMap.get(parameterName)[0]);
        
        } else if (parameterName.equalsIgnoreCase("contentStoreName")) {
          contentItem.setContentStoreName(paramMap.get(parameterName)[0]);
        
        } else if (parameterName.equalsIgnoreCase("field")) {
          contentItem.setField(paramMap.get(parameterName)[0]);
        
        } else if (parameterName.equalsIgnoreCase("createdBy")) {
          contentItem.setCreatedBy(paramMap.get(parameterName)[0]);
        
        } else if (parameterName.equalsIgnoreCase("lastModifiedBy")) {
          contentItem.setLastModifiedBy(paramMap.get(parameterName)[0]);
        
        } else if (parameterName.equalsIgnoreCase("tenantId")) {
          contentItem.setTenantId(paramMap.get(parameterName)[0]);
        }
      }
    }

    if (contentItem.getName() == null) {
      throw new ActivitiIllegalArgumentException("Content item name is required.");
    }

    if (request.getFileMap().size() == 0) {
      throw new ActivitiIllegalArgumentException("Content item content is required.");
    }

    MultipartFile file = request.getFileMap().values().iterator().next();

    if (file == null) {
      throw new ActivitiIllegalArgumentException("Content item file is required.");
    }

    try {
      contentService.saveContentItem(contentItem, file.getInputStream());

      response.setStatus(HttpStatus.CREATED.value());
      return contentRestResponseFactory.createContentItemResponse(contentItem);

    } catch (Exception e) {
      throw new ActivitiException("Error creating content item response", e);
    }
  }
}

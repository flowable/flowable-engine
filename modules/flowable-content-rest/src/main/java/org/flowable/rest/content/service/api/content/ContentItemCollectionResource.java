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
package org.flowable.rest.content.service.api.content;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Authorization;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.rest.api.DataResponse;
import org.flowable.rest.api.RequestUtil;
import org.flowable.rest.content.ContentRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Content item" }, description = "Manages Content item", authorizations = { @Authorization(value = "basicAuth") })
public class ContentItemCollectionResource extends ContentItemBaseResource {

    @Autowired
    protected ContentService contentService;

    @Autowired
    protected ContentRestResponseFactory contentRestResponseFactory;

    @Autowired
    protected ObjectMapper objectMapper;

    @ApiOperation(value = "Get all content items", tags = { "Content item" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", value = "Only return content items with the given id.", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", value = "Only return content items with the given name.", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", value = "Only return content items with a name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "mimeType", dataType = "string", value = "Only return content items with the given mime type.", paramType = "query"),
            @ApiImplicitParam(name = "mimeTypeLike", dataType = "string", value = "Only return content items with a mime type like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "taskId", dataType = "string", value = "Only return content items with the given task id.", paramType = "query"),
            @ApiImplicitParam(name = "taskIdLike", dataType = "string", value = "Only return content items with a task like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceId", dataType = "string", value = "Only return content items with the given process instance id.", paramType = "query"),
            @ApiImplicitParam(name = "processInstanceIdLike", dataType = "string", value = "Only return content items with a process instance like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "contentStoreId", dataType = "string", value = "Only return content items with the given content store id.", paramType = "query"),
            @ApiImplicitParam(name = "contentStoreIdLike", dataType = "string", value = "Only return content items with a content store id like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "contentStoreName", dataType = "string", value = "Only return content items with the given content store name.", paramType = "query"),
            @ApiImplicitParam(name = "contentStoreNameLike", dataType = "string", value = "Only return content items with a content store name like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "contentAvailable", dataType = "boolean", value = "Only return content items with or without content available.", paramType = "query"),
            @ApiImplicitParam(name = "contentSize", dataType = "long", value = "Only return content items with the given content size.", paramType = "query"),
            @ApiImplicitParam(name = "minimumContentSize", dataType = "long", value = "Only return content items with the a minimum content size of the given value.", paramType = "query"),
            @ApiImplicitParam(name = "maximumContentSize", dataType = "long", value = "Only return content items with the a maximum content size of the given value.", paramType = "query"),
            @ApiImplicitParam(name = "field", dataType = "string", value = "Only return content items with the given field.", paramType = "query"),
            @ApiImplicitParam(name = "fieldLike", dataType = "string", value = "Only return content items with a field like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "createdOn", dataType = "date", value = "Only return content items with the given create date.", paramType = "query"),
            @ApiImplicitParam(name = "createdBefore", dataType = "date", value = "Only return content items before given create date.", paramType = "query"),
            @ApiImplicitParam(name = "createdAfter", dataType = "date", value = "Only return content items after given create date.", paramType = "query"),
            @ApiImplicitParam(name = "createdBy", dataType = "string", value = "Only return content items with the given created by.", paramType = "query"),
            @ApiImplicitParam(name = "createdByLike", dataType = "string", value = "Only return content items with a created by like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "lastModifiedOn", dataType = "date", value = "Only return content items with the given last modified date.", paramType = "query"),
            @ApiImplicitParam(name = "lastModifiedBefore", dataType = "date", value = "Only return content items before given last modified date.", paramType = "query"),
            @ApiImplicitParam(name = "lastModifiedAfter", dataType = "date", value = "Only return content items after given last modified date.", paramType = "query"),
            @ApiImplicitParam(name = "lastModifiedBy", dataType = "string", value = "Only return content items with the given last modified by.", paramType = "query"),
            @ApiImplicitParam(name = "lastModifiedByLike", dataType = "string", value = "Only return content items with a last modified by like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", value = "Only return content items with the given tenantId.", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", value = "Only return content items with a tenantId like the given value.", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", value = "If true, only returns content items without a tenantId set. If false, the withoutTenantId parameter is ignored.", paramType = "query"),
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "The content items are returned.")
    })
    @RequestMapping(value = "/content-service/content-items", method = RequestMethod.GET, produces = "application/json")
    public DataResponse getContentItems(@ApiParam(hidden = true) @RequestParam Map<String, String> requestParams, HttpServletRequest httpRequest) {
        // Create a Content item query request
        ContentItemQueryRequest request = new ContentItemQueryRequest();

        // Populate filter-parameters
        if (requestParams.containsKey("id")) {
            request.setId(requestParams.get("id"));
        }

        if (requestParams.containsKey("name")) {
            request.setName(requestParams.get("name"));
        }

        if (requestParams.containsKey("nameLike")) {
            request.setNameLike(requestParams.get("nameLike"));
        }

        if (requestParams.containsKey("mimeType")) {
            request.setMimeType(requestParams.get("mimeType"));
        }

        if (requestParams.containsKey("mimeTypeLike")) {
            request.setMimeTypeLike(requestParams.get("mimeTypeLike"));
        }

        if (requestParams.containsKey("taskId")) {
            request.setTaskId(requestParams.get("taskId"));
        }

        if (requestParams.containsKey("taskIdLike")) {
            request.setTaskIdLike(requestParams.get("taskIdLike"));
        }

        if (requestParams.containsKey("processInstanceId")) {
            request.setProcessInstanceId(requestParams.get("processInstanceId"));
        }

        if (requestParams.containsKey("processInstanceIdLike")) {
            request.setProcessInstanceIdLike(requestParams.get("processInstanceIdLike"));
        }

        if (requestParams.containsKey("contentStoreId")) {
            request.setContentStoreId(requestParams.get("contentStoreId"));
        }

        if (requestParams.containsKey("contentStoreIdLike")) {
            request.setContentStoreIdLike(requestParams.get("contentStoreIdLike"));
        }

        if (requestParams.containsKey("contentStoreName")) {
            request.setContentStoreName(requestParams.get("contentStoreName"));
        }

        if (requestParams.containsKey("contentStoreNameLike")) {
            request.setContentStoreNameLike(requestParams.get("contentStoreNameLike"));
        }

        if (requestParams.containsKey("contentSize")) {
            request.setContentSize(Long.valueOf(requestParams.get("contentSize")));
        }

        if (requestParams.containsKey("minimumContentSize")) {
            request.setMinimumContentSize(Long.valueOf(requestParams.get("minimumContentSize")));
        }

        if (requestParams.containsKey("maximumContentSize")) {
            request.setMaximumContentSize(Long.valueOf(requestParams.get("maximumContentSize")));
        }

        if (requestParams.containsKey("contentAvailable")) {
            request.setContentAvailable(Boolean.valueOf(requestParams.get("contentAvailable")));
        }

        if (requestParams.containsKey("field")) {
            request.setField(requestParams.get("field"));
        }

        if (requestParams.containsKey("fieldLike")) {
            request.setFieldLike(requestParams.get("fieldLike"));
        }

        if (requestParams.containsKey("createdOn")) {
            request.setCreatedOn(RequestUtil.getDate(requestParams, "createdOn"));
        }

        if (requestParams.containsKey("createdBefore")) {
            request.setCreatedBefore(RequestUtil.getDate(requestParams, "createdBefore"));
        }

        if (requestParams.containsKey("createdAfter")) {
            request.setCreatedAfter(RequestUtil.getDate(requestParams, "createdAfter"));
        }

        if (requestParams.containsKey("createdBy")) {
            request.setCreatedBy(requestParams.get("createdBy"));
        }

        if (requestParams.containsKey("createdByLike")) {
            request.setCreatedByLike(requestParams.get("createdByLike"));
        }

        if (requestParams.containsKey("lastModifiedOn")) {
            request.setLastModifiedOn(RequestUtil.getDate(requestParams, "lastModifiedOn"));
        }

        if (requestParams.containsKey("lastModifiedBefore")) {
            request.setLastModifiedBefore(RequestUtil.getDate(requestParams, "lastModifiedBefore"));
        }

        if (requestParams.containsKey("lastModifiedAfter")) {
            request.setLastModifiedAfter(RequestUtil.getDate(requestParams, "lastModifiedAfter"));
        }

        if (requestParams.containsKey("lastModifiedBy")) {
            request.setLastModifiedBy(requestParams.get("lastModifiedBy"));
        }

        if (requestParams.containsKey("lastModifiedByLike")) {
            request.setLastModifiedByLike(requestParams.get("lastModifiedByLike"));
        }

        if (requestParams.containsKey("tenantId")) {
            request.setTenantId(requestParams.get("tenantId"));
        }

        if (requestParams.containsKey("tenantIdLike")) {
            request.setTenantIdLike(requestParams.get("tenantIdLike"));
        }

        if (requestParams.containsKey("withoutTenantId") && Boolean.valueOf(requestParams.get("withoutTenantId"))) {
            request.setWithoutTenantId(Boolean.TRUE);
        }

        return getContentItemsFromQueryRequest(request, requestParams);
    }

    @ApiOperation(value = "Create a new content item, with content item information and an optional attached file", tags = { "Content item" }, notes = "## Create a new content item, with content item information\n\n"
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
            + "- *tenantId*: The tenant identifier of the content item, optional.")
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
                throw new FlowableIllegalArgumentException("Failed to serialize to a ContentItemRequest instance", e);
            }

            if (contentItemRequest == null) {
                throw new FlowableIllegalArgumentException("ContentItemRequest properties not found in request");
            }

            result = createSimpleContentItem(contentItemRequest);
        }

        response.setStatus(HttpStatus.CREATED.value());
        return result;
    }

    protected ContentItemResponse createSimpleContentItem(ContentItemRequest contentItemRequest) {

        if (contentItemRequest.getName() == null) {
            throw new FlowableIllegalArgumentException("Content item name is required.");
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
            throw new FlowableIllegalArgumentException("Content item name is required.");
        }

        if (request.getFileMap().size() == 0) {
            throw new FlowableIllegalArgumentException("Content item content is required.");
        }

        MultipartFile file = request.getFileMap().values().iterator().next();

        if (file == null) {
            throw new FlowableIllegalArgumentException("Content item file is required.");
        }

        try {
            contentService.saveContentItem(contentItem, file.getInputStream());

            response.setStatus(HttpStatus.CREATED.value());
            return contentRestResponseFactory.createContentItemResponse(contentItem);

        } catch (Exception e) {
            throw new FlowableException("Error creating content item response", e);
        }
    }
}

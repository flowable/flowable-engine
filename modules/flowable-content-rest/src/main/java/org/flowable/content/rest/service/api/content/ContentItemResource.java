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
package org.flowable.content.rest.service.api.content;

import javax.servlet.http.HttpServletResponse;

import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.content.rest.ContentRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Content item" }, description = "Manages Content item", authorizations = { @Authorization(value = "basicAuth") })
public class ContentItemResource extends ContentItemBaseResource {

    @Autowired
    protected ContentService contentService;

    @Autowired
    protected ContentRestResponseFactory contentRestResponseFactory;

    @ApiOperation(value = "Get a content item", tags = { "Content item" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the content item was found and returned."),
            @ApiResponse(code = 404, message = "Indicates the requested content item was not found.")
    })
    @GetMapping(value = "/content-service/content-items/{contentItemId}", produces = "application/json")
    public ContentItemResponse getContentItem(@ApiParam(name = "contentItemId") @PathVariable String contentItemId) {
        return contentRestResponseFactory.createContentItemResponse(getContentItemFromRequest(contentItemId));
    }

    @ApiOperation(value = "Updates a content item, with the provided content item information", tags = { "Content item" },
            notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the content item was updated and the result is returned."),
            @ApiResponse(code = 404, message = "Indicates content item could not be found.")
    })
    @PutMapping(value = "/content-service/content-items/{contentItemId}", produces = "application/json")
    public ContentItemResponse updateContentItem(@RequestBody ContentItemRequest request, @ApiParam(name = "contentItemId") @PathVariable String contentItemId) {
        ContentItem contentItem = getContentItemFromRequest(contentItemId);
        if (request.getName() != null) {
            contentItem.setName(request.getName());
        }

        if (request.getMimeType() != null) {
            contentItem.setMimeType(request.getMimeType());
        }

        if (request.getTaskId() != null) {
            contentItem.setTaskId(request.getTaskId());
        }

        if (request.getProcessInstanceId() != null) {
            contentItem.setProcessInstanceId(request.getProcessInstanceId());
        }

        if (request.getContentStoreId() != null) {
            contentItem.setContentStoreId(request.getContentStoreId());
        }

        if (request.getContentStoreName() != null) {
            contentItem.setContentStoreName(request.getContentStoreName());
        }

        if (request.getField() != null) {
            contentItem.setField(request.getField());
        }

        if (request.getTenantId() != null) {
            contentItem.setTenantId(request.getTenantId());
        }

        if (request.getCreatedBy() != null) {
            contentItem.setCreatedBy(request.getCreatedBy());
        }

        if (request.getLastModifiedBy() != null) {
            contentItem.setLastModifiedBy(request.getLastModifiedBy());
        }

        contentService.saveContentItem(contentItem);

        return restResponseFactory.createContentItemResponse(contentItem);
    }

    @ApiOperation(value = "Delete a content item", tags = { "Content item" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the content item was deleted."),
            @ApiResponse(code = 404, message = "Indicates the content item was not found.")
    })
    @DeleteMapping(value = "/content-service/content-items/{contentItemId}")
    public void deleteContentItem(@ApiParam(name = "contentItemId") @PathVariable String contentItemId, HttpServletResponse response) {
        ContentItem contentItem = getContentItemFromRequest(contentItemId);
        
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteContentItem(contentItem);
        }
        
        contentService.deleteContentItem(contentItemId);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}

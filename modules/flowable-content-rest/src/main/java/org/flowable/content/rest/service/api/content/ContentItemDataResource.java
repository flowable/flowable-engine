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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.apache.commons.io.IOUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.content.api.ContentItem;
import org.flowable.content.rest.ContentRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Content item" }, description = "Manage content item data", authorizations = { @Authorization(value = "basicAuth") })
public class ContentItemDataResource extends ContentItemBaseResource {

    @Autowired
    protected ContentRestResponseFactory contentRestResponseFactory;

    @ApiOperation(value = "Get the data of a content item", tags = {"Content item" },
            notes = "The response body contains the binary content. By default, the content-type of the response is set to application/octet-stream unless the content item type contains a valid mime type.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the content item was found and the requested content is returned."),
            @ApiResponse(code = 404, message = "Indicates the content item was not found or the content item doesn’t have a binary stream available. Status message provides additional information.")
    })
    @GetMapping(value = "/content-service/content-items/{contentItemId}/data")
    public ResponseEntity<byte[]> getContentItemData(@ApiParam(name = "contentItemId") @PathVariable("contentItemId") String contentItemId, HttpServletResponse response) {

        ContentItem contentItem = getContentItemFromRequest(contentItemId);
        if (!contentItem.isContentAvailable()) {
            throw new FlowableException("No data available for content item " + contentItemId);
        }

        InputStream dataStream = contentService.getContentItemData(contentItemId);
        if (dataStream == null) {
            throw new FlowableObjectNotFoundException("Content item with id '" + contentItemId + "' doesn't have content associated with it.");
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        MediaType mediaType = null;
        if (contentItem.getMimeType() != null) {
            try {
                mediaType = MediaType.valueOf(contentItem.getMimeType());
                responseHeaders.set("Content-Type", contentItem.getMimeType());
            } catch (Exception e) {
                // ignore if unknown media type
            }
        }

        if (mediaType == null) {
            responseHeaders.set("Content-Type", "application/octet-stream");
        }

        try {
            return new ResponseEntity<>(IOUtils.toByteArray(dataStream), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            throw new FlowableException("Error getting content item data " + contentItemId, e);
        }
    }

    @ApiOperation(value = "Save the content item data", tags = { "Content item" }, notes = "Save the content item data with an attached file"
            + "The request should be of type multipart/form-data. There should be a single file-part included with the binary value of the content item.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", dataType = "file", paramType = "form", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Indicates the content item data was saved and the result is returned."),
            @ApiResponse(code = 400, message = "Indicates required content item data is missing from the request.")
    })
    @PostMapping(value = "/content-service/content-items/{contentItemId}/data", produces = "application/json", consumes = "multipart/form-data")
    public ContentItemResponse saveContentItemData(@ApiParam(name = "contentItemId") @PathVariable("contentItemId") String contentItemId,
            HttpServletRequest request, HttpServletResponse response) {

        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new FlowableException("Multipart request required to save content item data");
        }

        ContentItem contentItem = getContentItemFromRequest(contentItemId);

        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        MultipartFile file = multipartRequest.getFileMap().values().iterator().next();

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

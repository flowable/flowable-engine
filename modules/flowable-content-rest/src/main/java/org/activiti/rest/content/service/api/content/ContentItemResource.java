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

import org.activiti.content.api.ContentItem;
import org.activiti.content.api.ContentService;
import org.activiti.engine.common.api.ActivitiObjectNotFoundException;
import org.activiti.rest.content.ContentRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Content item" }, description = "Manages Content item")
public class ContentItemResource {

  @Autowired
  protected ContentService contentService;

  @Autowired
  protected ContentRestResponseFactory contentRestResponseFactory;

  @ApiOperation(value = "Get a content item", tags = {"Content item"})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message =  "Indicates the content item was found and returned."),
          @ApiResponse(code = 404, message = "Indicates the requested content item was not found.")
  })
  @RequestMapping(value = "/content-service/content-items/{contentItemId}", method = RequestMethod.GET, produces = "application/json")
  public ContentItemResponse getContentItem(@ApiParam(name = "contentItemId") @PathVariable String contentItemId) {
    return contentRestResponseFactory.createContentItemResponse(getContentItemFromRequest(contentItemId));
  }
  
  protected ContentItem getContentItemFromRequest(String contentItemId) {
    ContentItem contentItem = contentService.createContentItemQuery().id(contentItemId).singleResult();
    if (contentItem == null) {
      throw new ActivitiObjectNotFoundException("Could not find a content item with id '" + contentItemId + "'.", ContentItem.class);
    }
    return contentItem;
  }
}

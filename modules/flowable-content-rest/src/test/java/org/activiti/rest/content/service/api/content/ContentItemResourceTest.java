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
import org.activiti.rest.content.ContentRestUrls;
import org.activiti.rest.content.service.api.BaseSpringContentRestTestCase;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Tijs Rademakers
 */
public class ContentItemResourceTest extends BaseSpringContentRestTestCase {

  public void testGetContentItem() throws Exception {
    String contentItemId = createContentItem("test.pdf", "application/pdf", null, "12345", null, "test", "test2");
    
    ContentItem contentItem = contentService.createContentItemQuery().singleResult();

    try {
      HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
          ContentRestUrls.URL_CONTENT_ITEM, contentItemId));
      CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
      JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
      closeResponse(response);
      
      assertEquals(contentItem.getId(), responseNode.get("id").asText());
      assertEquals(contentItem.getName(), responseNode.get("name").asText());
      assertEquals(contentItem.getMimeType(), responseNode.get("mimeType").asText());
      assertTrue(responseNode.get("taskId").isNull());
      assertEquals(contentItem.getProcessInstanceId(), responseNode.get("processInstanceId").asText());
      assertEquals("", responseNode.get("tenantId").asText());
      assertEquals(contentItem.getCreatedBy(), responseNode.get("createdBy").asText());
      assertEquals(contentItem.getLastModifiedBy(), responseNode.get("lastModifiedBy").asText());
      assertEquals(contentItem.getCreated(), getDateFromISOString(responseNode.get("created").asText()));
      assertEquals(contentItem.getLastModified(), getDateFromISOString(responseNode.get("lastModified").asText()));
  
      // Check URL's
      assertEquals(httpGet.getURI().toString(), responseNode.get("url").asText());
      
    } finally {
      contentService.deleteContentItem(contentItemId);
    }
  }

  public void testGetUnexistingContentItem() throws Exception {
    HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
        ContentRestUrls.URL_CONTENT_ITEM, "unexisting"));
    CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
    closeResponse(response);
  }

}

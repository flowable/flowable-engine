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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.content.api.ContentItem;
import org.activiti.rest.content.ContentRestUrls;
import org.activiti.rest.content.service.api.BaseSpringContentRestTestCase;
import org.activiti.rest.content.service.api.HttpMultipartHelper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class ContentItemCollectionResourceTest extends BaseSpringContentRestTestCase {

  public void testCreateContentItem() throws Exception {
    ContentItem urlContentItem = null;
    try {
      ObjectNode requestNode = objectMapper.createObjectNode();
      requestNode.put("name", "Simple content item");
      requestNode.put("mimeType", "application/pdf");
      requestNode.put("taskId", "12345");
      requestNode.put("processInstanceId", "123456");
      requestNode.put("contentStoreId", "id");
      requestNode.put("contentStoreName", "testStore");
      requestNode.put("createdBy", "testa");
      requestNode.put("lastModifiedBy", "testb");
  
      HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
          ContentRestUrls.URL_CONTENT_ITEM_COLLECTION));
      httpPost.setEntity(new StringEntity(requestNode.toString()));
      CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
  
      // Check if content item is created
      List<ContentItem> contentItems = contentService.createContentItemQuery().list();
      assertEquals(1, contentItems.size());
  
      urlContentItem = contentItems.get(0);
  
      JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
      closeResponse(response);
      assertEquals(urlContentItem.getId(), responseNode.get("id").asText());
      assertEquals("Simple content item", responseNode.get("name").asText());
      assertEquals("application/pdf", responseNode.get("mimeType").asText());
      assertEquals("12345", responseNode.get("taskId").asText());
      assertEquals("123456", responseNode.get("processInstanceId").asText());
      assertEquals("id", responseNode.get("contentStoreId").asText());
      assertEquals("testStore", responseNode.get("contentStoreName").asText());
      assertEquals(false, responseNode.get("contentAvailable").asBoolean());
      assertEquals("testa", responseNode.get("createdBy").asText());
      assertEquals("testb", responseNode.get("lastModifiedBy").asText());
      assertEquals(urlContentItem.getCreated(), getDateFromISOString(responseNode.get("created").asText()));
      assertEquals(urlContentItem.getLastModified(), getDateFromISOString(responseNode.get("lastModified").asText()));
      assertTrue(responseNode.get("url").textValue().endsWith(ContentRestUrls.createRelativeResourceUrl(
          ContentRestUrls.URL_CONTENT_ITEM, urlContentItem.getId())));
      
    } finally {
      if (urlContentItem != null) {
        contentService.deleteContentItem(urlContentItem.getId());
      }
    }
  }

  public void testCreateContentItemWithContent() throws Exception {
    ContentItem urlContentItem = null;
    try {
      InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());
  
      // Add name, type and scope
      Map<String, String> additionalFields = new HashMap<String, String>();
      additionalFields.put("name", "Simple content item");
      additionalFields.put("mimeType", "application/pdf");
      additionalFields.put("taskId", "12345");
      additionalFields.put("processInstanceId", "123456");
      additionalFields.put("createdBy", "testa");
      additionalFields.put("lastModifiedBy", "testb");
  
      HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
          ContentRestUrls.URL_CONTENT_ITEM_COLLECTION));
      httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, additionalFields));
      CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
  
      // Check if content item is created
      List<ContentItem> contentItems = contentService.createContentItemQuery().list();
      assertEquals(1, contentItems.size());
  
      urlContentItem = contentItems.get(0);
  
      assertEquals("This is binary content", IOUtils.toString(contentService.getContentItemData(urlContentItem.getId())));
  
      JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
      closeResponse(response);
      assertEquals(urlContentItem.getId(), responseNode.get("id").asText());
      assertEquals("Simple content item", responseNode.get("name").asText());
      assertEquals("application/pdf", responseNode.get("mimeType").asText());
      assertEquals("12345", responseNode.get("taskId").asText());
      assertEquals("123456", responseNode.get("processInstanceId").asText());
      assertEquals(urlContentItem.getContentStoreId(), responseNode.get("contentStoreId").asText());
      assertEquals("file", responseNode.get("contentStoreName").asText());
      assertEquals(true, responseNode.get("contentAvailable").asBoolean());
      assertEquals("testa", responseNode.get("createdBy").asText());
      assertEquals("testb", responseNode.get("lastModifiedBy").asText());
      assertEquals(urlContentItem.getCreated(), getDateFromISOString(responseNode.get("created").asText()));
      assertEquals(urlContentItem.getLastModified(), getDateFromISOString(responseNode.get("lastModified").asText()));
      assertTrue(responseNode.get("url").textValue().endsWith(ContentRestUrls.createRelativeResourceUrl(
          ContentRestUrls.URL_CONTENT_ITEM, urlContentItem.getId())));
    
    } finally {
      if (urlContentItem != null) {
        contentService.deleteContentItem(urlContentItem.getId());
      }
    }
  }

  public void testCreateContentItemNoName() throws Exception {
    ObjectNode requestNode = objectMapper.createObjectNode();
    requestNode.put("mimeType", "application/pdf");

    // Post JSON without name
    HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
        ContentRestUrls.URL_CONTENT_ITEM_COLLECTION));
    httpPost.setEntity(new StringEntity(requestNode.toString()));
    closeResponse(executeBinaryRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
  }
}

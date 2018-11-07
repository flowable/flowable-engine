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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.content.api.ContentItem;
import org.flowable.content.rest.ContentRestUrls;
import org.flowable.content.rest.service.api.BaseSpringContentRestTestCase;
import org.flowable.content.rest.service.api.HttpMultipartHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class ContentItemResourceTest extends BaseSpringContentRestTestCase {

    public void testGetContentItem() throws Exception {
        String contentItemId = createContentItem("test.pdf", "application/pdf", null, "12345",
                null, null, "test", "test2");

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

    public void testGetContentItemData() throws Exception {
        InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes());
        String contentItemId = createContentItem("test.pdf", "application/pdf", null,
                "12345", null, null, "test", "test2", binaryContent);

        try {
            // Get content item data
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
                    ContentRestUrls.URL_CONTENT_ITEM_DATA, contentItemId)), HttpStatus.SC_OK);

            // Check response headers
            assertEquals("application/pdf", response.getEntity().getContentType().getValue());
            assertEquals("This is binary content", IOUtils.toString(response.getEntity().getContent()));
            closeResponse(response);

        } finally {
            contentService.deleteContentItem(contentItemId);
        }
    }

    public void testUpdateContentItem() throws Exception {
        String contentItemId = createContentItem("test.pdf", "application/pdf", null,
                "12345", null, null, "test", "test2");

        ContentItem contentItem = contentService.createContentItemQuery().singleResult();

        try {
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("name", "test2.txt");
            requestNode.put("mimeType", "application/txt");
            requestNode.put("createdBy", "testb");
            requestNode.put("lastModifiedBy", "testc");

            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
                    ContentRestUrls.URL_CONTENT_ITEM, contentItemId));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);

            assertEquals(contentItem.getId(), responseNode.get("id").asText());
            assertEquals("test2.txt", responseNode.get("name").asText());
            assertEquals("application/txt", responseNode.get("mimeType").asText());
            assertTrue(responseNode.get("taskId").isNull());
            assertEquals(contentItem.getProcessInstanceId(), responseNode.get("processInstanceId").asText());
            assertEquals("", responseNode.get("tenantId").asText());
            assertEquals("testb", responseNode.get("createdBy").asText());
            assertEquals("testc", responseNode.get("lastModifiedBy").asText());
            assertEquals(contentItem.getCreated(), getDateFromISOString(responseNode.get("created").asText()));
            assertEquals(contentItem.getLastModified(), getDateFromISOString(responseNode.get("lastModified").asText()));

        } finally {
            contentService.deleteContentItem(contentItemId);
        }
    }

    public void testSaveContentItemData() throws Exception {
        String contentItemId = createContentItem("test.pdf", "application/pdf", null,
                "12345", null, null, "test", "test2");

        executePostAndAssert(contentItemId);
    }

    public void testSaveContentItemDataForCase() throws Exception {
        String contentItemId = createContentItem("test.pdf", "application/pdf", null,
                null, "12345", null, "test", "test2");

        executePostAndAssert(contentItemId);
    }

    protected void executePostAndAssert(String contentItemId) throws IOException {
        ContentItem origContentItem = contentService.createContentItemQuery().id(contentItemId).singleResult();
        assertNotNull(origContentItem);

        InputStream binaryContent = null;
        try {
            binaryContent = new ByteArrayInputStream("This is binary content".getBytes());

            // Get content item data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
                    ContentRestUrls.URL_CONTENT_ITEM_DATA, contentItemId));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, null));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
            closeResponse(response);

            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
                    ContentRestUrls.URL_CONTENT_ITEM_DATA, contentItemId)), HttpStatus.SC_OK);

            // Check response headers
            assertEquals("application/pdf", response.getEntity().getContentType().getValue());
            assertEquals("This is binary content", IOUtils.toString(response.getEntity().getContent()));
            closeResponse(response);

            ContentItem changedContentItem = contentService.createContentItemQuery().id(contentItemId).singleResult();
            assertTrue(origContentItem.getLastModified().getTime() < changedContentItem.getLastModified().getTime());

        } finally {
            contentService.deleteContentItem(contentItemId);
            if (binaryContent != null) {
                binaryContent.close();
            }
        }
    }

}

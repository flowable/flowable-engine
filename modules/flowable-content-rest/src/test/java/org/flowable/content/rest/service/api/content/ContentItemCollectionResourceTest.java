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
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
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
            assertFalse(responseNode.get("contentAvailable").asBoolean());
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
            Map<String, String> additionalFields = new HashMap<>();
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
            assertTrue(responseNode.get("contentAvailable").asBoolean());
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
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }

    public void testGetContentItems() throws Exception {
        try {
            Calendar contentItemCreateCal = Calendar.getInstance();
            contentItemCreateCal.set(Calendar.MILLISECOND, 0);

            Calendar contentItem2CreateCal = Calendar.getInstance();
            contentItem2CreateCal.add(Calendar.HOUR, 2);
            contentItem2CreateCal.set(Calendar.MILLISECOND, 0);

            Calendar inBetweenCreateCal = Calendar.getInstance();
            inBetweenCreateCal.add(Calendar.HOUR, 1);

            contentEngineConfiguration.getClock().setCurrentTime(contentItemCreateCal.getTime());
            ContentItem contentItem = contentService.newContentItem();
            contentItem.setName("one.pdf");
            contentItem.setMimeType("application/pdf");
            contentItem.setTaskId("task1");
            contentItem.setProcessInstanceId("process1");
            contentItem.setContentStoreId("value1");
            contentItem.setContentStoreName("store1");
            contentItem.setField("name");
            contentItem.setCreatedBy("test1");
            contentItem.setLastModifiedBy("test1");
            contentItem.setTenantId("tenant1");

            contentService.saveContentItem(contentItem);

            contentEngineConfiguration.getClock().setCurrentTime(contentItem2CreateCal.getTime());
            ContentItem contentItem2 = contentService.newContentItem();
            contentItem2.setName("two.pdf");
            contentItem2.setMimeType("application/text");
            contentItem2.setTaskId("task1");
            contentItem2.setField("name2");
            contentItem2.setCreatedBy("test1");
            contentItem2.setLastModifiedBy("test3");

            contentService.saveContentItem(contentItem2, this.getClass().getClassLoader().getResourceAsStream("test.txt"));
            long contentItem2Size = contentItem2.getContentSize();

            String url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION);
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // Name filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?name=one.pdf";
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Name like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?nameLike=" + encode("%.pdf");
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // Mime type filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?mimeType=" + encode("application/pdf");
            assertResultsPresentInDataResponse(url, contentItem.getId());

            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?mimeType=" + encode("nonexisting");
            assertEmptyResultsPresentInDataResponse(url);

            // Mime type like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?mimeTypeLike=" + encode("%pdf");
            assertResultsPresentInDataResponse(url, contentItem.getId());

            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?mimeTypeLike=" + encode("%nonexisting");
            assertEmptyResultsPresentInDataResponse(url);

            // Task id filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?taskId=task1";
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // Task id like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?taskIdLike=" + encode("task%");
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // Process instance id filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?processInstanceId=process1";
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Process instance id like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?processInstanceIdLike=" + encode("process%");
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Content store id filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?contentStoreId=value1";
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Content store id like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?contentStoreIdLike=" + encode("value%");
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Content store name filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?contentStoreName=file";
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // Content store name like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?contentStoreNameLike=" + encode("fi%");
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // Content size filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?contentSize=" + contentItem2Size;
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // Minimum content size filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?minimumContentSize=1";
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // Maximum content size filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?maximumContentSize=999999";
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // Field filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?field=name";
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Field like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?fieldLike=" + encode("name%");
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // CreatedOn filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?createdOn=" + getISODateString(contentItemCreateCal.getTime());
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // CreatedAfter filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?createdAfter=" + getISODateString(inBetweenCreateCal.getTime());
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // CreatedBefore filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?createdBefore=" + getISODateString(inBetweenCreateCal.getTime());
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Created by filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?createdBy=test1";
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // Created by like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?createdByLike=" + encode("test%");
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // LastModifiedOn filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?lastModifiedOn=" + getISODateString(contentItemCreateCal.getTime());
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // LastModifiedAfter filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?lastModifiedAfter=" + getISODateString(inBetweenCreateCal.getTime());
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // LastModifiedBefore filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?lastModifiedBefore=" + getISODateString(inBetweenCreateCal.getTime());
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // LastModified by filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?lastModifiedBy=test3";
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // LastModified by like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?lastModifiedByLike=" + encode("test%");
            assertResultsPresentInDataResponse(url, contentItem.getId(), contentItem2.getId());

            // Without tenantId filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?withoutTenantId=true";
            assertResultsPresentInDataResponse(url, contentItem2.getId());

            // Tenant id filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?tenantId=tenant1";
            assertResultsPresentInDataResponse(url, contentItem.getId());

            // Tenant id like filtering
            url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_CONTENT_ITEM_COLLECTION) + "?tenantIdLike=" + encode("%enant1");
            assertResultsPresentInDataResponse(url, contentItem.getId());

        } finally {
            // Clean content items even if test fails
            List<ContentItem> contentItems = contentService.createContentItemQuery().list();
            for (ContentItem contentItem : contentItems) {
                contentService.deleteContentItem(contentItem.getId());
            }
        }
    }
}

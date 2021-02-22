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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
import com.fasterxml.jackson.databind.node.TextNode;

import net.javacrumbs.jsonunit.core.Option;

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

            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "  id: '" + contentItem.getId() + "',"
                            + "  name: '" + contentItem.getName() + "',"
                            + "  mimeType: '" + contentItem.getMimeType() + "',"
                            + "  taskId: null,"
                            + "  tenantId: '',"
                            + "  processInstanceId: '" + contentItem.getProcessInstanceId() + "',"
                            + "  created: " + new TextNode(getISODateStringWithTZ(contentItem.getCreated())) + ","
                            + "  createdBy: '" + contentItem.getCreatedBy() + "',"
                            + "  lastModified: " + new TextNode(getISODateStringWithTZ(contentItem.getLastModified())) + ","
                            + "  lastModifiedBy: '" + contentItem.getLastModifiedBy() + "',"
                            + "  url: '" + httpGet.getURI().toString() + "'"
                            + "}");

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
            assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/pdf");
            try (InputStream contentStream = response.getEntity().getContent()) {
                assertThat(contentStream).hasContent("This is binary content");
            }
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

            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "  id: '" + contentItem.getId() + "',"
                            + "  name: 'test2.txt',"
                            + "  mimeType: 'application/txt',"
                            + "  taskId: null,"
                            + "  tenantId: '',"
                            + "  processInstanceId: '" + contentItem.getProcessInstanceId() + "',"
                            + "  created: " + new TextNode(getISODateStringWithTZ(contentItem.getCreated())) + ","
                            + "  createdBy: 'testb',"
                            + "  lastModified: " + new TextNode(getISODateStringWithTZ(contentItem.getLastModified())) + ","
                            + "  lastModifiedBy: 'testc'"
                            + "}");

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
        assertThat(origContentItem).isNotNull();

        try (InputStream binaryContent = new ByteArrayInputStream("This is binary content".getBytes())) {

            // Get content item data
            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
                ContentRestUrls.URL_CONTENT_ITEM_DATA, contentItemId));
            httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/octet-stream", binaryContent, null));
            CloseableHttpResponse response = executeBinaryRequest(httpPost, HttpStatus.SC_CREATED);
            closeResponse(response);

            response = executeRequest(new HttpGet(SERVER_URL_PREFIX + ContentRestUrls.createRelativeResourceUrl(
                ContentRestUrls.URL_CONTENT_ITEM_DATA, contentItemId)), HttpStatus.SC_OK);

            // Check response headers
            assertThat(response.getEntity().getContentType().getValue()).isEqualTo("application/pdf");
            try (InputStream contentStream = response.getEntity().getContent()) {
                assertThat(contentStream).hasContent("This is binary content");
            }
            closeResponse(response);

            ContentItem changedContentItem = contentService.createContentItemQuery().id(contentItemId).singleResult();
            assertThat(origContentItem.getLastModified().getTime()).isLessThan(changedContentItem.getLastModified().getTime());

        } finally {
            contentService.deleteContentItem(contentItemId);
        }
    }

}

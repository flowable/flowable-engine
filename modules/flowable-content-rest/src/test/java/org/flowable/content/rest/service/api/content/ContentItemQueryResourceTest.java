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

import java.util.Calendar;
import java.util.List;

import org.flowable.content.api.ContentItem;
import org.flowable.content.rest.ContentRestUrls;
import org.flowable.content.rest.service.api.BaseSpringContentRestTestCase;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for querying content items
 * 
 * @author Tijs Rademakers
 */
public class ContentItemQueryResourceTest extends BaseSpringContentRestTestCase {

    public void testQueryContentItems() throws Exception {
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

            // Check filter-less to fetch all content items
            String url = ContentRestUrls.createRelativeResourceUrl(ContentRestUrls.URL_QUERY_CONTENT_ITEM);
            ObjectNode requestNode = objectMapper.createObjectNode();
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // Name filtering
            requestNode.removeAll();
            requestNode.put("name", "one.pdf");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Name like filtering
            requestNode.removeAll();
            requestNode.put("nameLike", "%.pdf");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // Mime type filtering
            requestNode.removeAll();
            requestNode.put("mimeType", "application/pdf");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Mime type like filtering
            requestNode.removeAll();
            requestNode.put("mimeTypeLike", "application%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // Task id filtering
            requestNode.removeAll();
            requestNode.put("taskId", "task1");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // Task id like filtering
            requestNode.removeAll();
            requestNode.put("taskIdLike", "task%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // Process instance id filtering
            requestNode.removeAll();
            requestNode.put("processInstanceId", "process1");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Process instance id like filtering
            requestNode.removeAll();
            requestNode.put("processInstanceIdLike", "process%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Content store id filtering
            requestNode.removeAll();
            requestNode.put("contentStoreId", "value1");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Content store id like filtering
            requestNode.removeAll();
            requestNode.put("contentStoreIdLike", "value%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Content store name filtering
            requestNode.removeAll();
            requestNode.put("contentStoreName", "file");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // Content store id like filtering
            requestNode.removeAll();
            requestNode.put("contentStoreNameLike", "fi%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // Content size filtering
            requestNode.removeAll();
            requestNode.put("contentSize", contentItem2Size);
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // Minimum content size filtering
            requestNode.removeAll();
            requestNode.put("minimumContentSize", 1);
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // Maximum content size filtering
            requestNode.removeAll();
            requestNode.put("maximumContentSize", 99999);
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // Field filtering
            requestNode.removeAll();
            requestNode.put("field", "name");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Field like filtering
            requestNode.removeAll();
            requestNode.put("fieldLike", "name%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // CreatedOn filtering
            requestNode.removeAll();
            requestNode.put("createdOn", getISODateString(contentItemCreateCal.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // CreatedAfter filtering
            requestNode.removeAll();
            requestNode.put("createdAfter", getISODateString(inBetweenCreateCal.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // CreatedBefore filtering
            requestNode.removeAll();
            requestNode.put("createdBefore", getISODateString(inBetweenCreateCal.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Created by filtering
            requestNode.removeAll();
            requestNode.put("createdBy", "test1");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // Created by like filtering
            requestNode.removeAll();
            requestNode.put("createdByLike", "test%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // LastModifiedOn filtering
            requestNode.removeAll();
            requestNode.put("lastModifiedOn", getISODateString(contentItemCreateCal.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // LastModifiedAfter filtering
            requestNode.removeAll();
            requestNode.put("lastModifiedAfter", getISODateString(inBetweenCreateCal.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // LastModifiedBefore filtering
            requestNode.removeAll();
            requestNode.put("lastModifiedBefore", getISODateString(inBetweenCreateCal.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // LastModified by filtering
            requestNode.removeAll();
            requestNode.put("lastModifiedBy", "test3");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // LastModified by like filtering
            requestNode.removeAll();
            requestNode.put("lastModifiedByLike", "test%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId(), contentItem2.getId());

            // Filtering without tenant id
            requestNode.removeAll();
            requestNode.put("withoutTenantId", true);
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem2.getId());

            // Tenant id filtering
            requestNode.removeAll();
            requestNode.put("tenantId", "tenant1");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

            // Tenant id like filtering
            requestNode.removeAll();
            requestNode.put("tenantIdLike", "tenant%");
            assertResultsPresentInPostDataResponse(url, requestNode, contentItem.getId());

        } finally {
            // Clean content items even if test fails
            List<ContentItem> contentItems = contentService.createContentItemQuery().list();
            for (ContentItem contentItem : contentItems) {
                contentService.deleteContentItem(contentItem.getId());
            }
        }
    }
}

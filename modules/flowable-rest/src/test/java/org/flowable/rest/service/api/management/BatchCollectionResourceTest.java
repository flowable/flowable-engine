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
package org.flowable.rest.service.api.management;

import java.util.Calendar;

import org.flowable.batch.api.Batch;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to the Batch collection and a single batch resource.
 */
public class BatchCollectionResourceTest extends BaseSpringRestTestCase {

    @Test
    public void testGetBatches() throws Exception {
        Calendar hourAgo = Calendar.getInstance();
        hourAgo.add(Calendar.HOUR, -1);

        Calendar inAnHour = Calendar.getInstance();
        inAnHour.add(Calendar.HOUR, 1);

        ObjectNode docNode = objectMapper.createObjectNode();
        docNode.put("test", "value");
        
        Batch batch = managementService.createBatchBuilder()
            .batchType(Batch.PROCESS_MIGRATION_TYPE)
            .searchKey("test")
            .searchKey2("anotherTest")
            .batchDocumentJson(docNode.toString())
            .create();

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION);
        assertResultsPresentInDataResponse(url, batch.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?createTimeBefore=" + getISODateString(inAnHour.getTime());
        assertResultsPresentInDataResponse(url, batch.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?createTimeBefore=" + getISODateString(hourAgo.getTime());
        assertResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?createTimeAfter=" + getISODateString(hourAgo.getTime());
        assertResultsPresentInDataResponse(url, batch.getId());

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?createTimeAfter=" + getISODateString(inAnHour.getTime());
        assertResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?batchType=" + Batch.PROCESS_MIGRATION_TYPE;
        assertResultsPresentInDataResponse(url, batch.getId());
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?batchType=unknown";
        assertEmptyResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?searchKey=test";
        assertResultsPresentInDataResponse(url, batch.getId());
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?searchKey=unknown";
        assertEmptyResultsPresentInDataResponse(url);
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?searchKey2=anotherTest";
        assertResultsPresentInDataResponse(url, batch.getId());
        
        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?searchKey2=unknown";
        assertEmptyResultsPresentInDataResponse(url);

        url = RestUrls.createRelativeResourceUrl(RestUrls.URL_BATCH_COLLECTION) + "?withoutTenantId=true";
        assertResultsPresentInDataResponse(url, batch.getId());
        
        managementService.deleteBatch(batch.getId());
    }
}

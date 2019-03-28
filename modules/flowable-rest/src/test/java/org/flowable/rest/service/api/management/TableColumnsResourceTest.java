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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Test for all REST-operations related to the Table columns.
 * 
 * @author Frederik Heremans
 */
public class TableColumnsResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single table's columns. GET management/tables/{tableName}/columns
     */
    @Test
    public void testGetTableColumns() throws Exception {
        String tableName = managementService.getTableCount().keySet().iterator().next();

        TableMetaData metaData = managementService.getTableMetaData(tableName);

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_COLUMNS, tableName)), HttpStatus.SC_OK);

        // Check table
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertEquals(tableName, responseNode.get("tableName").textValue());

        ArrayNode names = (ArrayNode) responseNode.get("columnNames");
        ArrayNode types = (ArrayNode) responseNode.get("columnTypes");
        assertNotNull(names);
        assertNotNull(types);

        assertEquals(metaData.getColumnNames().size(), names.size());
        assertEquals(metaData.getColumnTypes().size(), types.size());

        for (int i = 0; i < names.size(); i++) {
            assertEquals(names.get(i).textValue(), metaData.getColumnNames().get(i));
            assertEquals(types.get(i).textValue(), metaData.getColumnTypes().get(i));
        }
    }

    @Test
    public void testGetColumnsForUnexistingTable() throws Exception {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_COLUMNS, "unexisting")), HttpStatus.SC_NOT_FOUND));
    }
}

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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to the Table collection and a single table resource.
 * 
 * @author Frederik Heremans
 */
public class TableResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting tables. GET management/tables
     */
    @Test
    public void testGetTables() throws Exception {
        Map<String, Long> tableCounts = managementService.getTableCount();

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLES_COLLECTION)),
                HttpStatus.SC_OK);

        // Check table array
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThat(responseNode.isArray()).isTrue();
        assertThat(responseNode).hasSize(tableCounts.size());

        for (int i = 0; i < responseNode.size(); i++) {
            ObjectNode table = (ObjectNode) responseNode.get(i);
            assertThat(table.get("name").textValue()).isNotNull();
            assertThat(table.get("count").longValue()).isNotNull();
            assertThat(table.get("url").textValue()).endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE, table.get("name").textValue()));
            assertThat(table.get("count").longValue()).isEqualTo(tableCounts.get(table.get("name").textValue()).longValue());
        }
    }

    /**
     * Test getting a single table. GET management/tables/{tableName}
     */
    @Test
    public void testGetTable() throws Exception {
        Map<String, Long> tableCounts = managementService.getTableCount();

        String tableNameToGet = tableCounts.keySet().iterator().next();

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE, tableNameToGet)),
                HttpStatus.SC_OK);

        // Check table
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThat(responseNode).isNotNull();
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "name: '" + tableNameToGet + "',"
                        + "count: " + tableCounts.get(tableNameToGet) + ","
                        + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE, tableNameToGet) + "'"
                        + "}");
    }

    @Test
    public void testGetUnexistingTable() throws Exception {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE, "unexisting")), HttpStatus.SC_NOT_FOUND));
    }
}

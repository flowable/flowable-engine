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

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to the Table columns.
 * 
 * @author Frederik Heremans
 */
public class TableDataResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single table's row data. GET management/tables/{tableName}/data
     */
    @Test
    public void testGetTableColumns() throws Exception {
        try {

            Task task = taskService.newTask();
            taskService.saveTask(task);
            taskService.setVariable(task.getId(), "var1", 123);
            taskService.setVariable(task.getId(), "var2", 456);
            taskService.setVariable(task.getId(), "var3", 789);

            // We use variable-table as a reference
            String tableName = managementService.getTableName(VariableInstanceEntity.class);

            CloseableHttpResponse response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, tableName)), HttpStatus.SC_OK);

            // Check paging result
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "total: 3,"
                            + "size: 3,"
                            + "start: 0,"
                            + "order: null,"
                            + "sort: null"
                            + "}");

            // Check variables are actually returned
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                    .isEqualTo("{"
                            + "data: [ {"
                            + "      },"
                            + "      {"
                            + "      },"
                            + "      {"
                            + "      }"
                            + "]"
                            + "}");

            // Check sorting, ascending
            response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, tableName) + "?orderAscendingColumn=LONG_"),
                    HttpStatus.SC_OK);
            responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "total: 3,"
                            + "size: 3,"
                            + "start: 0,"
                            + "order: 'asc',"
                            + "sort: 'LONG_'"
                            + "}");
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                    .isEqualTo("{"
                            + "data: [ {"
                            + "            NAME_: 'var1'"
                            + "      },"
                            + "      {"
                            + "            NAME_: 'var2'"
                            + "      },"
                            + "      {"
                            + "            NAME_: 'var3'"
                            + "      }"
                            + "]"
                            + "}");

            // Check sorting, descending
            response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, tableName) + "?orderDescendingColumn=LONG_"),
                    HttpStatus.SC_OK);
            responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "total: 3,"
                            + "size: 3,"
                            + "start: 0,"
                            + "order: 'desc',"
                            + "sort: 'LONG_'"
                            + "}");
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                    .isEqualTo("{"
                            + "data: [ {"
                            + "            NAME_: 'var3'"
                            + "      },"
                            + "      {"
                            + "            NAME_: 'var2'"
                            + "      },"
                            + "      {"
                            + "            NAME_: 'var1'"
                            + "      }"
                            + "]"
                            + "}");

            // Finally, check result limiting
            response = executeRequest(new HttpGet(
                            SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, tableName) + "?orderAscendingColumn=LONG_&start=1&size=1"),
                    HttpStatus.SC_OK);
            responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "total: 3,"
                            + "size: 1,"
                            + "start: 1,"
                            + "order: 'asc',"
                            + "sort: 'LONG_'"
                            + "}");
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                    .isEqualTo("{"
                            + "data: [ {"
                            + "            NAME_: 'var2'"
                            + "      } ]"
                            + "}");

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    @Test
    public void getTableColumnsException() throws Exception {
        // Only one of 'orderAscendingColumn' or 'orderDescendingColumn' can be supplied.
        String tableName = managementService.getTableName(VariableInstanceEntity.class);
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, tableName)
                        + "?orderAscendingColumn=LONG_&orderDescendingColumn=LONG_"),
                HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testGetDataForUnexistingTable() throws Exception {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, "unexisting")), HttpStatus.SC_NOT_FOUND));
    }

    @Test
    public void testGetDataSortByIllegalColumn() throws Exception {
        // We use variable-table as a reference
        String tableName = managementService.getTableName(VariableInstanceEntity.class);
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, tableName) + "?orderAscendingColumn=unexistingColumn"),
                HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }
}

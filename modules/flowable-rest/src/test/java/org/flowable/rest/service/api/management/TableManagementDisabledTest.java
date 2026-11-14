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

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/**
 * @author Filip Hrisafov
 */
public class TableManagementDisabledTest extends BaseSpringRestTestCase {

    @Test
    public void tableResourcesAreNotRegisteredByDefault() throws Exception {
        String tableName = managementService.getTableName(VariableInstanceEntity.class);

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLES_COLLECTION)),
                HttpStatus.SC_NOT_FOUND);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertThatJson(responseNode)
                .isEqualTo("{"
                        + "  message: 'Not Found',"
                        + "  exception: 'No endpoint GET /service/management/tables.'"
                        + "}");

        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE, tableName)),
                HttpStatus.SC_NOT_FOUND));
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_COLUMNS, tableName)),
                HttpStatus.SC_NOT_FOUND));
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TABLE_DATA, tableName)),
                HttpStatus.SC_NOT_FOUND));
    }
}

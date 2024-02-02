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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class EnginePropertiesResourceTest extends BaseSpringRestTestCase {

    @Test
    public void testGetAllProperties() throws Exception {
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_ENGINE_PROPERTIES)), HttpStatus.SC_OK);
        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        assertThat(responseNode.get("schema.version").asText()).isNotNull();
        assertThat(responseNode.get("common.schema.version").asText()).isNotNull();
    }

    @Test
    public void testCreateNewProperty() throws Exception {
        assertThat(managementService.getProperties().get("testProperty")).isNull();

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_ENGINE_PROPERTIES));
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "testProperty");
        requestNode.put("value", "testValue");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);

        assertThat(managementService.getProperties().get("testProperty")).isEqualTo("testValue");
    }

    @Test
    public void testCreateAlreadyExistingProperty() throws Exception {
        String originalPropertyValue = managementService.getProperties().get("schema.version");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_ENGINE_PROPERTIES));
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("name", "schema.version"); // already exists
        requestNode.put("value", "testValue");
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CONFLICT);

        assertThat(managementService.getProperties().get("schema.version")).isEqualTo(originalPropertyValue);
    }

    @Test
    public void testDeleteProperty() throws Exception {
        managementService.executeCommand(commandContext -> {
            PropertyEntityManager propertyEntityManager = CommandContextUtil.getPropertyEntityManager(commandContext);
            PropertyEntity propertyEntity = propertyEntityManager.create();
            propertyEntity.setName("testPropertyToDelete");
            propertyEntity.setValue("123");
            propertyEntityManager.insert(propertyEntity);
            return null;
        });

        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_ENGINE_PROPERTIES) + "/testPropertyToDelete");
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);

        assertThat(managementService.getProperties().get("testPropertyToDelete")).isNull();
    }

    @Test
    public void testDeleteInvalidProperty() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_ENGINE_PROPERTIES) + "/invalid");
        CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testUpdateProperty() throws Exception {
        managementService.executeCommand(commandContext -> {
            PropertyEntityManager propertyEntityManager = CommandContextUtil.getPropertyEntityManager(commandContext);
            PropertyEntity propertyEntity = propertyEntityManager.create();
            propertyEntity.setName("testPropertyToUpdate");
            propertyEntity.setValue("123");
            propertyEntityManager.insert(propertyEntity);
            return null;
        });

        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_ENGINE_PROPERTIES) + "/testPropertyToUpdate");
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("value", "456");
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

        assertThat(managementService.getProperties().get("testPropertyToUpdate")).isEqualTo("456");
    }

    @Test
    public void testUpdateInvalidProperty() throws Exception {
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_ENGINE_PROPERTIES) + "/invalid");
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("value", "456");
        httpPut.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_NOT_FOUND);
    }


}

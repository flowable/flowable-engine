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

package org.flowable.rest.service.api.repository;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Calendar;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.repository.Model;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Frederik Heremans
 */
public class ModelResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetModel() throws Exception {

        Model model = null;
        try {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.MILLISECOND, 0);
            processEngineConfiguration.getClock().setCurrentTime(now.getTime());

            model = repositoryService.newModel();
            model.setCategory("Model category");
            model.setKey("Model key");
            model.setMetaInfo("Model metainfo");
            model.setName("Model name");
            model.setVersion(2);
            model.setDeploymentId(deploymentId);
            model.setTenantId("myTenant");
            repositoryService.saveModel(model);

            repositoryService.addModelEditorSource(model.getId(), "This is the editor source".getBytes());
            repositoryService.addModelEditorSourceExtra(model.getId(), "This is the extra editor source".getBytes());

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .isEqualTo("{"
                            + "name: 'Model name',"
                            + "key: 'Model key',"
                            + "category: 'Model category',"
                            + "version: 2,"
                            + "metaInfo: 'Model metainfo',"
                            + "deploymentId: '" + deploymentId + "',"
                            + "id: '" + model.getId() + "',"
                            + "tenantId: 'myTenant',"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()) + "',"
                            + "deploymentUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, deploymentId) + "',"
                            + "sourceUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL_SOURCE, model.getId()) + "',"
                            + "sourceExtraUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL_SOURCE_EXTRA, model.getId())
                            + "',"
                            + "createTime: '" + getISODateString(now.getTime()) + "',"
                            + "lastUpdateTime: '" + getISODateString(now.getTime()) + "'"
                            + "}");

        } finally {
            try {
                repositoryService.deleteModel(model.getId());
            } catch (Throwable ignore) {
                // Ignore, model might not be created
            }
        }
    }

    @Test
    public void testGetUnexistingModel() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, "unexisting"));
        closeResponse(executeRequest(httpGet, HttpStatus.SC_NOT_FOUND));
    }

    @Test
    public void testDeleteModel() throws Exception {
        Model model = null;
        try {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.MILLISECOND, 0);
            processEngineConfiguration.getClock().setCurrentTime(now.getTime());

            model = repositoryService.newModel();
            model.setCategory("Model category");
            model.setKey("Model key");
            model.setMetaInfo("Model metainfo");
            model.setName("Model name");
            model.setVersion(2);
            repositoryService.saveModel(model);

            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT));

            // Check if the model is really gone
            assertThat(repositoryService.createModelQuery().modelId(model.getId()).singleResult()).isNull();

            model = null;
        } finally {
            if (model != null) {
                try {
                    repositoryService.deleteModel(model.getId());
                } catch (Throwable ignore) {
                    // Ignore, model might not be created
                }
            }
        }
    }

    @Test
    public void testDeleteUnexistingModel() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, "unexisting"));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND));
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testUpdateModel() throws Exception {

        Model model = null;
        try {
            Calendar createTime = Calendar.getInstance();
            createTime.set(Calendar.MILLISECOND, 0);
            processEngineConfiguration.getClock().setCurrentTime(createTime.getTime());

            model = repositoryService.newModel();
            model.setCategory("Model category");
            model.setKey("Model key");
            model.setMetaInfo("Model metainfo");
            model.setName("Model name");
            model.setVersion(2);
            repositoryService.saveModel(model);

            Calendar updateTime = Calendar.getInstance();
            updateTime.set(Calendar.MILLISECOND, 0);
            updateTime.add(Calendar.HOUR, 1);
            processEngineConfiguration.getClock().setCurrentTime(updateTime.getTime());

            // Create update request
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("name", "Updated name");
            requestNode.put("category", "Updated category");
            requestNode.put("key", "Updated key");
            requestNode.put("metaInfo", "Updated metainfo");
            requestNode.put("deploymentId", deploymentId);
            requestNode.put("version", 3);
            requestNode.put("tenantId", "myTenant");

            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);

            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "name: 'Updated name',"
                            + "key: 'Updated key',"
                            + "category: 'Updated category',"
                            + "version: 3,"
                            + "metaInfo: 'Updated metainfo',"
                            + "deploymentId: '" + deploymentId + "',"
                            + "id: '" + model.getId() + "',"
                            + "tenantId: 'myTenant',"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()) + "',"
                            + "deploymentUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, deploymentId) + "',"
                            + "createTime: '" + getISODateString(createTime.getTime()) + "',"
                            + "lastUpdateTime: '" + getISODateString(updateTime.getTime()) + "'"
                            + "}");

        } finally {
            try {
                repositoryService.deleteModel(model.getId());
            } catch (Throwable ignore) {
                // Ignore, model might not be created
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testUpdateModelOverrideWithNull() throws Exception {
        Model model = null;
        try {
            Calendar createTime = Calendar.getInstance();
            createTime.set(Calendar.MILLISECOND, 0);
            processEngineConfiguration.getClock().setCurrentTime(createTime.getTime());

            model = repositoryService.newModel();
            model.setCategory("Model category");
            model.setKey("Model key");
            model.setMetaInfo("Model metainfo");
            model.setName("Model name");
            model.setTenantId("myTenant");
            model.setVersion(2);
            repositoryService.saveModel(model);

            Calendar updateTime = Calendar.getInstance();
            updateTime.set(Calendar.MILLISECOND, 0);
            processEngineConfiguration.getClock().setCurrentTime(updateTime.getTime());

            // Create update request
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("name", (String) null);
            requestNode.put("category", (String) null);
            requestNode.put("key", (String) null);
            requestNode.put("metaInfo", (String) null);
            requestNode.put("deploymentId", (String) null);
            requestNode.put("version", (String) null);
            requestNode.put("tenantId", (String) null);

            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "name: null,"
                            + "key: null,"
                            + "category: null,"
                            + "version: null,"
                            + "metaInfo: null,"
                            + "deploymentId: null,"
                            + "id: '" + model.getId() + "',"
                            + "tenantId: null,"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()) + "',"
                            + "createTime: '" + getISODateString(createTime.getTime()) + "',"
                            + "lastUpdateTime: '" + getISODateString(updateTime.getTime()) + "'"
                            + "}");

            model = repositoryService.getModel(model.getId());
            assertThat(model.getName()).isNull();
            assertThat(model.getKey()).isNull();
            assertThat(model.getCategory()).isNull();
            assertThat(model.getMetaInfo()).isNull();
            assertThat(model.getDeploymentId()).isNull();
            assertThat(model.getTenantId()).isEmpty();

        } finally {
            try {
                repositoryService.deleteModel(model.getId());
            } catch (Throwable ignore) {
                // Ignore, model might not be created
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testUpdateModelNoFields() throws Exception {

        Model model = null;
        try {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.MILLISECOND, 0);
            processEngineConfiguration.getClock().setCurrentTime(now.getTime());

            model = repositoryService.newModel();
            model.setCategory("Model category");
            model.setKey("Model key");
            model.setMetaInfo("Model metainfo");
            model.setName("Model name");
            model.setVersion(2);
            model.setDeploymentId(deploymentId);
            repositoryService.saveModel(model);

            // Use empty request-node, nothing should be changed after update
            ObjectNode requestNode = objectMapper.createObjectNode();

            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()));
            httpPut.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "name: 'Model name',"
                            + "key: 'Model key',"
                            + "category: 'Model category',"
                            + "version: 2,"
                            + "metaInfo: 'Model metainfo',"
                            + "deploymentId: '" + deploymentId + "',"
                            + "id: '" + model.getId() + "',"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, model.getId()) + "',"
                            + "deploymentUrl: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_DEPLOYMENT, deploymentId) + "',"
                            + "createTime: '" + getISODateString(now.getTime()) + "',"
                            + "lastUpdateTime: '" + getISODateString(now.getTime()) + "'"
                            + "}");

        } finally {
            try {
                repositoryService.deleteModel(model.getId());
            } catch (Throwable ignore) {
                // Ignore, model might not be created
            }
        }
    }

    @Test
    public void testUpdateUnexistingModel() throws Exception {
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_MODEL, "unexisting"));
        httpPut.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_NOT_FOUND));
    }
}
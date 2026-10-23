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
package org.flowable.cmmn.rest.service.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.conf.ObjectVariableSerializationDisabledApplicationConfiguration;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.HttpMultipartHelper;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

/**
 * @author Tijs Rademakers
 */
@SpringJUnitWebConfig(ObjectVariableSerializationDisabledApplicationConfiguration.class)
public class SerializableVariablesDisabledTest extends BaseSpringRestTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testCreateSingleSerializableProcessVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        TestSerializableVariable serializable = new TestSerializableVariable();
        serializable.setSomeField("some value");

        // Serialize object to readable stream for representation
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        output.writeObject(serializable);
        output.close();

        InputStream binaryContent = new ByteArrayInputStream(buffer.toByteArray());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "serializableVariable");
        additionalFields.put("type", "serializable");

        // Upload a valid CMMN-file using multipart-data
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX +
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_COLLECTION, caseInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));

        // We have serializeable object disabled, we should get a 415.
        assertResponseStatus(httpPost, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testCreateSingleSerializableTaskVariable() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();

        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        TestSerializableVariable serializable = new TestSerializableVariable();
        serializable.setSomeField("some value");

        // Serialize object to readable stream for representation
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ObjectOutputStream output = new ObjectOutputStream(buffer);
        output.writeObject(serializable);
        output.close();

        InputStream binaryContent = new ByteArrayInputStream(buffer.toByteArray());

        // Add name, type and scope
        Map<String, String> additionalFields = new HashMap<>();
        additionalFields.put("name", "serializableVariable");
        additionalFields.put("type", "serializable");

        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX +
                CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));

        // We have serializable object disabled, we should get a 415.
        assertResponseStatus(httpPost, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
    public void testGetSerializableVariableData() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        TestSerializableVariable serializable = new TestSerializableVariable();
        serializable.setSomeField("some value");
        runtimeService.setVariable(caseInstance.getId(), "serializableVariable", serializable);

        Task task = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        // We have serializable objects disabled in the REST API, we should get a 415.
        assertResponseStatus(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "serializableVariable")),
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
        assertResponseStatus(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TASK_VARIABLE_DATA, task.getId(), "serializableVariable") + "?scope=global"),
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);

        String variableInstanceId = runtimeService.createVariableInstanceQuery().caseInstanceId(caseInstance.getId())
                .variableName("serializableVariable").singleResult().getId();
        assertResponseStatus(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_VARIABLE_INSTANCE_DATA, variableInstanceId)),
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);

        String historicVariableInstanceId = historyService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId())
                .variableName("serializableVariable").singleResult().getId();
        assertResponseStatus(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_VARIABLE_INSTANCE_DATA, historicVariableInstanceId)),
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
        assertResponseStatus(new HttpGet(SERVER_URL_PREFIX
                + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_HISTORIC_CASE_INSTANCE_VARIABLE_DATA, caseInstance.getId(), "serializableVariable")),
                HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    public void assertResponseStatus(HttpUriRequest request, int expectedStatusCode) {

        assertThatCode(() -> {
            CloseableHttpResponse response = null;
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("kermit", "kermit");
            provider.setCredentials(AuthScope.ANY, credentials);
            HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

            response = (CloseableHttpResponse) client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            assertThat(statusCode).isEqualTo(expectedStatusCode);

            if (client instanceof CloseableHttpClient) {
                ((CloseableHttpClient) client).close();
            }

            response.close();
        }).doesNotThrowAnyException();
    }

}

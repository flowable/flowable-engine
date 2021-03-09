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
package org.flowable.rest.service.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.rest.conf.ObjectVariableSerializationDisabledApplicationConfiguration;
import org.flowable.rest.service.HttpMultipartHelper;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.util.TestServerUtil;
import org.flowable.rest.util.TestServerUtil.TestServer;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class SerializableVariablesDiabledTest {

    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private IdentityService identityService;
    private TaskService taskService;

    private String serverUrlPrefix;

    private String testUserId;
    private String testGroupId;

    @Before
    public void setupServer() {
        if (serverUrlPrefix == null) {
            TestServer testServer = TestServerUtil.createAndStartServer(ObjectVariableSerializationDisabledApplicationConfiguration.class);
            serverUrlPrefix = testServer.getServerUrlPrefix();

            this.repositoryService = testServer.getApplicationContext().getBean(RepositoryService.class);
            this.runtimeService = testServer.getApplicationContext().getBean(RuntimeService.class);
            this.identityService = testServer.getApplicationContext().getBean(IdentityService.class);
            this.taskService = testServer.getApplicationContext().getBean(TaskService.class);

            User user = identityService.newUser("kermit");
            user.setFirstName("Kermit");
            user.setLastName("the Frog");
            user.setPassword("kermit");
            identityService.saveUser(user);

            Group group = identityService.newGroup("admin");
            group.setName("Administrators");
            identityService.saveGroup(group);

            identityService.createMembership(user.getId(), group.getId());

            this.testUserId = user.getId();
            this.testGroupId = group.getId();
        }
    }

    @After
    public void removeUsers() {
        identityService.deleteMembership(testUserId, testGroupId);
        identityService.deleteGroup(testGroupId);
        identityService.deleteUser(testUserId);

        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testCreateSingleSerializableProcessVariable() throws Exception {

        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml").deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

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

        // Upload a valid BPMN-file using multipart-data
        HttpPost httpPost = new HttpPost(serverUrlPrefix +
                RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_INSTANCE_VARIABLE_COLLECTION, processInstance.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));

        // We have serializeable object disabled, we should get a 415.
        assertResponseStatus(httpPost, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    public void testCreateSingleSerializableTaskVariable() throws Exception {
        repositoryService.createDeployment()
                .addClasspathResource("org/flowable/rest/service/api/runtime/ProcessInstanceVariablesCollectionResourceTest.testProcess.bpmn20.xml")
                .deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

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

        HttpPost httpPost = new HttpPost(serverUrlPrefix +
                RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_VARIABLES_COLLECTION, task.getId()));
        httpPost.setEntity(HttpMultipartHelper.getMultiPartEntity("value", "application/x-java-serialized-object", binaryContent, additionalFields));

        // We have serializeable object disabled, we should get a 415.
        assertResponseStatus(httpPost, HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    public void assertResponseStatus(HttpUriRequest request, int expectedStatusCode) {
        CloseableHttpResponse response = null;
        try {

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

        } catch (IOException e) {
            fail(e.getMessage());
        }

    }

}

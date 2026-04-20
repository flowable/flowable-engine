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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class CaseInstanceMigrationResourceTest extends BaseSpringRestTestCase {
    
    @AfterEach
    public void tearDown() {
        for (CmmnDeployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testDirectMigration() throws Exception {
        deployCaseDefinition("my deploy", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-start.cmmn.xml");
        Authentication.setAuthenticatedUserId("migrateCaseUser");
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .start();
        
        CaseDefinition toCaseDefinition = deployCaseDefinition("my deploy", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-add-task-listener.cmmn.xml");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toCaseDefinitionId", toCaseDefinition.getId());
        requestNode.put("enableAutomaticPlanItemInstanceCreation", true);

        HttpPost httpPost = new HttpPost(buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_VALIDATE_MIGRATION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode validationNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        
        assertThat(validationNode.path("validationMessages")).hasSize(0);
        
        httpPost = new HttpPost(buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_MIGRATE, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        closeResponse(response);
        
        CaseInstance migratedInstance = runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(migratedInstance.getCaseDefinitionId()).isEqualTo(toCaseDefinition.getId());
        
        List<Task> tasks = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(4);
        Task task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("Stage task 1");
        taskService.complete(task.getId());
        
        task = tasks.get(1);
        assertThat(task.getName()).isEqualTo("Stage task 2");
        taskService.complete(task.getId());
        
        task = tasks.get(2);
        assertThat(task.getName()).isEqualTo("Task 1");
        taskService.complete(task.getId());
        
        task = tasks.get(3);
        assertThat(task.getName()).isEqualTo("Task 2");
        taskService.complete(task.getId());
        
        assertCaseEnded(caseInstance.getId());
    }
    
    @Test
    public void testDirectMigrationWithSentry() throws Exception {
        deployCaseDefinition("my deploy", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-with-sentry-start.cmmn.xml");
        Authentication.setAuthenticatedUserId("migrateCaseUser");
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .start();
        
        CaseDefinition toCaseDefinition = deployCaseDefinition("my deploy", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-with-sentry-add-task-listener.cmmn.xml");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toCaseDefinitionId", toCaseDefinition.getId());
        requestNode.put("enableAutomaticPlanItemInstanceCreation", true);

        HttpPost httpPost = new HttpPost(buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_VALIDATE_MIGRATION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode validationNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        
        assertThat(validationNode.path("validationMessages")).hasSize(0);
        
        httpPost = new HttpPost(buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_MIGRATE, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        closeResponse(response);
        
        CaseInstance migratedInstance = runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(migratedInstance.getCaseDefinitionId()).isEqualTo(toCaseDefinition.getId());
        
        List<Task> tasks = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("Task 1");
        taskService.complete(task.getId());
        
        tasks = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        
        task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("Stage task 1");
        taskService.complete(task.getId());
        
        task = tasks.get(1);
        assertThat(task.getName()).isEqualTo("Stage task 2");
        taskService.complete(task.getId());
        
        assertCaseEnded(caseInstance.getId());
    }
    
    @Test
    public void testUpdateTaskProperties() throws Exception {
        deployCaseDefinition("my deploy", "org/flowable/cmmn/rest/service/api/runtime/task-and-stage-start.cmmn.xml");
        Authentication.setAuthenticatedUserId("migrateCaseUser");
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .start();
        
        CaseDefinition toCaseDefinition = deployCaseDefinition("my deploy", "org/flowable/cmmn/rest/service/api/runtime/two-tasks-and-stage.cmmn.xml");
        
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("toCaseDefinitionId", toCaseDefinition.getId());
        requestNode.put("enableAutomaticPlanItemInstanceCreation", true);
        ArrayNode activityMigrationMappings = requestNode.putArray("activatePlanItemDefinitions");
        ObjectNode migrationMappingNode = activityMigrationMappings.addObject();
        migrationMappingNode.put("planItemDefinitionId", "humanTask2");
        migrationMappingNode.put("newName", "Updated name");
        migrationMappingNode.put("newDueDate", "2040-03-02T15:16:34");
        migrationMappingNode.put("newPriority", "86");
        migrationMappingNode.put("newCategory", "Updated category");
        migrationMappingNode.put("newFormKey", "UpdatedFormKey");
        migrationMappingNode.put("newAssignee", "fozzie");
        ArrayNode candidateGroupArray = migrationMappingNode.putArray("newCandidateGroups");
        candidateGroupArray.add("group1");
        candidateGroupArray.add("group2");

        HttpPost httpPost = new HttpPost(buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_VALIDATE_MIGRATION, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_OK);
        JsonNode validationNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        
        assertThat(validationNode.path("validationMessages")).hasSize(0);
        
        httpPost = new HttpPost(buildUrl(CmmnRestUrls.URL_CASE_INSTANCE_MIGRATE, caseInstance.getId()));
        httpPost.setEntity(new StringEntity(requestNode.toString()));
        response = executeRequest(httpPost, HttpStatus.SC_OK);
        closeResponse(response);
        
        CaseInstance migratedInstance = runtimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(migratedInstance.getCaseDefinitionId()).isEqualTo(toCaseDefinition.getId());
        
        List<Task> tasks = taskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(3);
        
        Task task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("Stage task 1");
        taskService.complete(task.getId());
        
        task = tasks.get(1);
        assertThat(task.getName()).isEqualTo("Task 1");
        taskService.complete(task.getId());
        
        task = tasks.get(2);
        assertThat(task.getName()).isEqualTo("Updated name");
        assertThat(task.getPriority()).isEqualTo(86);
        assertThat(task.getCategory()).isEqualTo("Updated category");
        assertThat(task.getFormKey()).isEqualTo("UpdatedFormKey");
        assertThat(task.getAssignee()).isEqualTo("fozzie");
        
        Calendar dueDateCal = new GregorianCalendar();
        dueDateCal.setTime(task.getDueDate());
        assertThat(dueDateCal.get(Calendar.YEAR)).isEqualTo(2040);
        assertThat(dueDateCal.get(Calendar.MONTH)).isEqualTo(2);
        assertThat(dueDateCal.get(Calendar.DAY_OF_MONTH)).isEqualTo(2);
        assertThat(dueDateCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(15);
        
        List<String> taskGroups = new ArrayList<>();
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        for (IdentityLink identityLink : identityLinks) {
            if (IdentityLinkType.CANDIDATE.equals(identityLink.getType())) {
                taskGroups.add(identityLink.getGroupId());
            }
        }
        
        assertThat(taskGroups).contains("group1", "group2");
        
        taskService.complete(task.getId());
        
        assertCaseEnded(caseInstance.getId());
    }

    protected CaseDefinition deployCaseDefinition(String name, String path) {
        CmmnDeployment deployment = repositoryService.createDeployment()
            .name(name)
            .addClasspathResource(path)
            .deploy();
        
        CaseDefinition processDefinition = repositoryService.createCaseDefinitionQuery()
            .deploymentId(deployment.getId()).singleResult();

        return processDefinition;
    }
}

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
package org.flowable.cmmn.test.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServiceTaskLoggingTest extends CustomCmmnConfigurationFlowableTestCase {

    protected Task task;

    @Override
    protected String getEngineName() {
        return "cmmnEngineWithServiceTaskLogging";
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setLoggingListener(new FlowableLoggingListener());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/logging/oneHumanTaskCase.cmmn")
    public void testBasicLogging() {
        FlowableLoggingListener.clear();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").latestVersion().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        
        assertEquals(6, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_CASE_STARTED, loggingNode.get("type").asText());
        assertEquals("Started case instance with id " + caseInstance.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED, loggingNode.get("type").asText());
        assertEquals("Plan item instance created with type humantask, new state available", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("HumanTask", loggingNode.get("elementType").asText());
        assertEquals("available", loggingNode.get("state").asText());
        assertEquals("available", loggingNode.get("newState").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_CREATE, loggingNode.get("type").asText());
        assertEquals("Human task 'The Task' created", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(task.getId(), loggingNode.get("taskId").asText());
        assertEquals("The Task", loggingNode.get("taskName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("HumanTask", loggingNode.get("elementType").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_SET_ASSIGNEE, loggingNode.get("type").asText());
        assertEquals("Set task assignee value to johnDoe", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(task.getId(), loggingNode.get("taskId").asText());
        assertEquals("The Task", loggingNode.get("taskName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("HumanTask", loggingNode.get("elementType").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE, loggingNode.get("type").asText());
        assertEquals("Plan item instance state change with type humantask, old state available, new state active", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("HumanTask", loggingNode.get("elementType").asText());
        assertEquals("active", loggingNode.get("state").asText());
        assertEquals("available", loggingNode.get("oldState").asText());
        assertEquals("active", loggingNode.get("newState").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for cmmn engine", loggingNode.get("message").asText());
        assertEquals("cmmn", loggingNode.get("engineType").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/logging/oneServiceTaskCase.cmmn")
    public void testServiceTaskLogging() {
        FlowableLoggingListener.clear();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneServiceTaskCase").latestVersion().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneServiceTaskCase").start();
        
        assertEquals(8, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_CASE_STARTED, loggingNode.get("type").asText());
        assertEquals("Started case instance with id " + caseInstance.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED, loggingNode.get("type").asText());
        assertEquals("Plan item instance created with type servicetask, new state available", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("available", loggingNode.get("state").asText());
        assertEquals("available", loggingNode.get("newState").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ENTER, loggingNode.get("type").asText());
        assertEquals("Executing service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("active", loggingNode.get("state").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXIT, loggingNode.get("type").asText());
        assertEquals("Executed service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("active", loggingNode.get("state").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE, loggingNode.get("type").asText());
        assertEquals("Plan item instance state change with type servicetask, old state available, new state active", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("active", loggingNode.get("state").asText());
        assertEquals("available", loggingNode.get("oldState").asText());
        assertEquals("active", loggingNode.get("newState").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE, loggingNode.get("type").asText());
        assertEquals("Plan item instance state change with type servicetask, old state active, new state completed", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("completed", loggingNode.get("state").asText());
        assertEquals("active", loggingNode.get("oldState").asText());
        assertEquals("completed", loggingNode.get("newState").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED, loggingNode.get("type").asText());
        assertEquals("Completed case instance with id " + caseInstance.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for cmmn engine", loggingNode.get("message").asText());
        assertEquals("cmmn", loggingNode.get("engineType").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/logging/oneAsyncServiceTaskCase.cmmn")
    public void testAsyncServiceTaskLogging() {
        FlowableLoggingListener.clear();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneServiceTaskCase").latestVersion().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneServiceTaskCase").start();
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("theTask").singleResult();
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        
        assertEquals(5, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_CASE_STARTED, loggingNode.get("type").asText());
        assertEquals("Started case instance with id " + caseInstance.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED, loggingNode.get("type").asText());
        assertEquals("Plan item instance created with type servicetask, new state available", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("available", loggingNode.get("state").asText());
        assertEquals("available", loggingNode.get("newState").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ASYNC_JOB, loggingNode.get("type").asText());
        assertEquals("Created async job for theTask, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE, loggingNode.get("type").asText());
        assertEquals("Plan item instance state change with type servicetask, old state available, new state async-active", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("async-active", loggingNode.get("state").asText());
        assertEquals("available", loggingNode.get("oldState").asText());
        assertEquals("async-active", loggingNode.get("newState").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for cmmn engine", loggingNode.get("message").asText());
        assertEquals("cmmn", loggingNode.get("engineType").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000, 200, true);
        assertEquals(10, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB, loggingNode.get("type").asText());
        assertEquals("Locking job for theTask, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("org.flowable.cmmn.test.delegate.TestJavaDelegate", loggingNode.get("elementSubType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        Map<String, ObjectNode> loggingMap = new HashMap<>();
        int planItemNewStateCounter = 1;
        int commandContextCounter = 1;
        for (ObjectNode logObjectNode : FlowableLoggingListener.TEST_LOGGING_NODES) {
            String logType = logObjectNode.get("type").asText();
            if (CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE.equals(logType)) {
                logType = planItemNewStateCounter + logType;
                planItemNewStateCounter++;
                
            } else if (CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE.equals(logType)) {
                logType = commandContextCounter + logType;
                commandContextCounter++;
            }
            loggingMap.put(logType, logObjectNode);
        }
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB, loggingNode.get("type").asText());
        assertEquals("Executing async job for theTask, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        int beforeJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(beforeJobNumber > 0);
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ENTER, loggingNode.get("type").asText());
        assertEquals("Executing service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("active", loggingNode.get("state").asText());
        int newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXIT, loggingNode.get("type").asText());
        assertEquals("Executed service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("active", loggingNode.get("state").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB);
        assertEquals(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB, loggingNode.get("type").asText());
        assertEquals("Unlocking job for theTask, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertNotNull(loggingNode.get(LoggingSessionUtil.LOG_NUMBER));
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("1" + CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE, loggingNode.get("type").asText());
        assertEquals("Plan item instance state change with type servicetask, old state async-active, new state active", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("active", loggingNode.get("state").asText());
        assertEquals("async-active", loggingNode.get("oldState").asText());
        assertEquals("active", loggingNode.get("newState").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("2" + CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertEquals(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE, loggingNode.get("type").asText());
        assertEquals("Plan item instance state change with type servicetask, old state active, new state completed", loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(planItemInstance.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theTask", loggingNode.get("elementId").asText());
        assertEquals("The Task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("completed", loggingNode.get("state").asText());
        assertEquals("active", loggingNode.get("oldState").asText());
        assertEquals("completed", loggingNode.get("newState").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED);
        assertEquals(CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED, loggingNode.get("type").asText());
        assertEquals("Completed case instance with id " + caseInstance.getId(), loggingNode.get("message").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementName"));
        assertFalse(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).isNull());
        assertFalse(loggingNode.get(LoggingSessionUtil.TIMESTAMP).isNull());
        
        loggingNode = loggingMap.get("1" + CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertEquals(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for cmmn engine", loggingNode.get("message").asText());
        assertEquals("cmmn", loggingNode.get("engineType").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).isNull());
        assertFalse(loggingNode.get(LoggingSessionUtil.TIMESTAMP).isNull());
        
        loggingNode = loggingMap.get("2" + CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertEquals(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for cmmn engine", loggingNode.get("message").asText());
        assertEquals("cmmn", loggingNode.get("engineType").asText());
        assertEquals(caseInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.CMMN, loggingNode.get("scopeType").asText());
        assertEquals(caseDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(caseDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(caseDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).isNull());
        assertFalse(loggingNode.get(LoggingSessionUtil.TIMESTAMP).isNull());
    }
}

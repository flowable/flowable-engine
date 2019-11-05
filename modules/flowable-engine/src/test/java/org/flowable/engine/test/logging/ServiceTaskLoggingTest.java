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

package org.flowable.engine.test.logging;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionLoggerOutput;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServiceTaskLoggingTest extends ResourceFlowableTestCase {
    
    public ServiceTaskLoggingTest() {
        super("org/flowable/engine/test/logging/logging.test.flowable.cfg.xml");
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/serviceTask.bpmn20.xml")
    public void testServiceTaskException() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("startToEnd").latestVersion().singleResult();
            
        try {
            runtimeService.startProcessInstanceByKey("startToEnd");
            fail("expected exception");
            
        } catch (Exception e) {
            // expected
        }
            
        assertEquals(7, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_PROCESS_STARTED, loggingNode.get("type").asText());
        assertTrue(loggingNode.get("message").asText().contains("Started process instance with id "));
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In StartEvent, executing NoneStartEventActivityBehavior", loggingNode.get("message").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theStart", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("StartEvent", loggingNode.get("elementType").asText());
        assertEquals("NoneStartEventActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(2, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow1, theStart --> task", loggingNode.get("message").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("flow1", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("theStart", loggingNode.get("sourceRef").asText());
        assertEquals("task", loggingNode.get("targetRef").asText());
        assertEquals(3, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In ServiceTask, executing ClassDelegate", loggingNode.get("message").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals("ClassDelegate", loggingNode.get("activityBehavior").asText());
        assertEquals(4, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER, loggingNode.get("type").asText());
        assertEquals("Executing service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate", loggingNode.get("message").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals(5, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_EXCEPTION, loggingNode.get("type").asText());
        assertEquals("Service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate threw exception Test exception", loggingNode.get("message").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals("Test exception", loggingNode.get("exception").get("message").asText());
        assertNotNull(loggingNode.get("exception").get("stackTrace").asText());
        assertEquals(6, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(6);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE, loggingNode.get("type").asText());
        assertEquals("Exception at closing command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(7, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/logging/failingAsyncServiceTask.bpmn20.xml")
    public void testFailingServiceTaskException() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("failingServiceTask").latestVersion().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingServiceTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
            
        assertEquals(5, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_PROCESS_STARTED, loggingNode.get("type").asText());
        assertTrue(loggingNode.get("message").asText().contains("Started process instance with id "));
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In StartEvent, executing NoneStartEventActivityBehavior", loggingNode.get("message").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theStart", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("StartEvent", loggingNode.get("elementType").asText());
        assertEquals("NoneStartEventActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(2, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow1, theStart --> task", loggingNode.get("message").asText());
        assertNotNull(loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("flow1", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("theStart", loggingNode.get("sourceRef").asText());
        assertEquals("task", loggingNode.get("targetRef").asText());
        assertEquals(3, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_ASYNC_JOB, loggingNode.get("type").asText());
        assertEquals("Created async job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertEquals(4, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(5, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertEquals(8, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB, loggingNode.get("type").asText());
        assertEquals("Locking job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        Map<String, ObjectNode> loggingMap = new HashMap<>();
        int commandContextCounter = 1;
        for (ObjectNode logObjectNode : FlowableLoggingListener.TEST_LOGGING_NODES) {
            String logType = logObjectNode.get("type").asText();
            if (LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE.equals(logType)) {
                logType = logObjectNode.get("elementType").asText() + logType;
            } else if (LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE.equals(logType)) {
                logType = commandContextCounter + logType;
                commandContextCounter++;
            }
            loggingMap.put(logType, logObjectNode);
        }
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB, loggingNode.get("type").asText());
        assertEquals("Executing async job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        int beforeJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(beforeJobNumber > 0);
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("ServiceTask" + LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In ServiceTask, executing ServiceTaskExpressionActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("${failureExpressionValue}", loggingNode.get("elementSubType").asText());
        assertEquals("ServiceTaskExpressionActivityBehavior", loggingNode.get("activityBehavior").asText());
        int newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE, loggingNode.get("type").asText());
        assertEquals("Exception at closing command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertEquals(8, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB, loggingNode.get("type").asText());
        assertEquals("Locking job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testVariableCreateLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion().singleResult();
            
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
            
        assertEquals(17, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_PROCESS_STARTED, loggingNode.get("type").asText());
        assertEquals("Started process instance with id " + processInstance.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In StartEvent, executing NoneStartEventActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theStart", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("StartEvent", loggingNode.get("elementType").asText());
        assertEquals("NoneStartEventActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow1, theStart --> task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("flow1", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("theStart", loggingNode.get("sourceRef").asText());
        assertEquals("task", loggingNode.get("targetRef").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In ServiceTask, executing ClassDelegate", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals("ClassDelegate", loggingNode.get("activityBehavior").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER, loggingNode.get("type").asText());
        assertEquals("Executing service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_CREATE, loggingNode.get("type").asText());
        assertEquals("Variable 'newVariable' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("newVariable", loggingNode.get("variableName").asText());
        assertEquals("string", loggingNode.get("variableType").asText());
        assertEquals("test", loggingNode.get("variableValue").asText());
        assertEquals("test", loggingNode.get("variableRawValue").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT, loggingNode.get("type").asText());
        assertEquals("Executed service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow2, task --> userTask", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("flow2", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("task", loggingNode.get("sourceRef").asText());
        assertEquals("userTask", loggingNode.get("targetRef").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In UserTask, executing UserTaskActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("UserTaskActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_CREATE, loggingNode.get("type").asText());
        assertEquals("User task 'Some user task' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertFalse(loggingNode.has("taskCategory"));
        assertFalse(loggingNode.has("taskDescription"));
        assertEquals("someKey", loggingNode.get("taskFormKey").asText());
        assertEquals(50, loggingNode.get("taskPriority").asInt());
        assertFalse(loggingNode.has("taskDueDate"));
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_ASSIGNEE, loggingNode.get("type").asText());
        assertEquals("Set task assignee value to johndoe", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals("johndoe", loggingNode.get("taskAssignee").asText());
        assertFalse(loggingNode.has("taskCategory"));
        assertFalse(loggingNode.has("taskDescription"));
        assertFalse(loggingNode.has("taskFormKey"));
        assertFalse(loggingNode.has("taskPriority"));
        assertFalse(loggingNode.has("taskDueDate"));
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_OWNER, loggingNode.get("type").asText());
        assertEquals("Set task owner value to janedoe", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals("janedoe", loggingNode.get("taskOwner").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 candidate group identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskGroupIdentityLinks").size());
        JsonNode identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        JsonNode identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("group1", identityLinkNode.get("groupId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("group2", identityLinkNode.get("groupId").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 candidate user identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskUserIdentityLinks").size());
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("johndoe", identityLinkNode.get("userId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("janedoe", identityLinkNode.get("userId").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 custom user identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskUserIdentityLinks").size());
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("businessAdministrator", identityLinkNode.get("type").asText());
        assertEquals("johndoe", identityLinkNode.get("userId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("someAdmin", identityLinkNode.get("type").asText());
        assertEquals("janedoe", identityLinkNode.get("userId").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 custom group identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskGroupIdentityLinks").size());
        identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("businessAdministrator", identityLinkNode.get("type").asText());
        assertEquals("group3", identityLinkNode.get("groupId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("someAdmin", identityLinkNode.get("type").asText());
        assertEquals("group4", identityLinkNode.get("groupId").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        LoggingSessionLoggerOutput.printLogNodes(FlowableLoggingListener.TEST_LOGGING_NODES);
        
        FlowableLoggingListener.clear();
        loggingItemCounter = 0;
        loggingNumberCounter = 1;
        
        runtimeService.setVariable(processInstance.getId(), "numVar", 123);
        assertEquals(2, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_CREATE, loggingNode.get("type").asText());
        assertEquals("Variable 'numVar' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementType"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals("numVar", loggingNode.get("variableName").asText());
        assertEquals("integer", loggingNode.get("variableType").asText());
        assertEquals("123", loggingNode.get("variableValue").asText());
        assertEquals(123, loggingNode.get("variableRawValue").asInt());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
        loggingItemCounter = 0;
        loggingNumberCounter = 1;
        
        runtimeService.setVariable(processInstance.getId(), "boolVar", true);
        assertEquals(2, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_CREATE, loggingNode.get("type").asText());
        assertEquals("Variable 'boolVar' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementType"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals("boolVar", loggingNode.get("variableName").asText());
        assertEquals("boolean", loggingNode.get("variableType").asText());
        assertEquals("true", loggingNode.get("variableValue").asText());
        assertTrue(loggingNode.get("variableRawValue").asBoolean());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(loggingNumberCounter++, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testVariableUpdateLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion().singleResult();
            
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
            
        assertEquals(17, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        FlowableLoggingListener.clear();
        runtimeService.setVariable(processInstance.getId(), "newVariable", "updatedValue");
        assertEquals(2, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_UPDATE, loggingNode.get("type").asText());
        assertEquals("Variable 'newVariable' updated", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementType"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals("newVariable", loggingNode.get("variableName").asText());
        assertEquals("string", loggingNode.get("variableType").asText());
        assertEquals("updatedValue", loggingNode.get("variableValue").asText());
        assertEquals("updatedValue", loggingNode.get("variableRawValue").asText());
        assertEquals("string", loggingNode.get("oldVariableType").asText());
        assertEquals("test", loggingNode.get("oldVariableValue").asText());
        assertEquals("test", loggingNode.get("oldVariableRawValue").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(2, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testVariableDeleteLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion().singleResult();
            
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
            
        assertEquals(17, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        FlowableLoggingListener.clear();
        runtimeService.removeVariable(processInstance.getId(), "newVariable");
        assertEquals(2, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_DELETE, loggingNode.get("type").asText());
        assertEquals("Variable 'newVariable' deleted", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertFalse(loggingNode.has("elementId"));
        assertFalse(loggingNode.has("elementType"));
        assertFalse(loggingNode.has("elementName"));
        assertEquals("newVariable", loggingNode.get("variableName").asText());
        assertEquals("string", loggingNode.get("variableType").asText());
        assertEquals("test", loggingNode.get("variableValue").asText());
        assertEquals("test", loggingNode.get("variableRawValue").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(2, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testCompleteTaskLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion().singleResult();
            
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
            
        assertEquals(17, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        FlowableLoggingListener.clear();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("newVariable", "newValue");
        variableMap.put("numVar", 123);
        taskService.complete(task.getId(), variableMap);
        
        assertEquals(7, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_UPDATE, loggingNode.get("type").asText());
        assertEquals("Variable 'newVariable' updated", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertEquals("newVariable", loggingNode.get("variableName").asText());
        assertEquals("string", loggingNode.get("variableType").asText());
        assertEquals("newValue", loggingNode.get("variableValue").asText());
        assertEquals("newValue", loggingNode.get("variableRawValue").asText());
        assertEquals("string", loggingNode.get("oldVariableType").asText());
        assertEquals("test", loggingNode.get("oldVariableValue").asText());
        assertEquals("test", loggingNode.get("oldVariableRawValue").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_CREATE, loggingNode.get("type").asText());
        assertEquals("Variable 'numVar' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertEquals("numVar", loggingNode.get("variableName").asText());
        assertEquals("integer", loggingNode.get("variableType").asText());
        assertEquals(123, loggingNode.get("variableValue").asInt());
        assertEquals(2, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertEquals("123", loggingNode.get("variableRawValue").asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_COMPLETE, loggingNode.get("type").asText());
        assertEquals("User task 'Some user task' completed", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertFalse(loggingNode.has("taskCategory"));
        assertFalse(loggingNode.has("taskDescription"));
        assertEquals("someKey", loggingNode.get("taskFormKey").asText());
        assertEquals(50, loggingNode.get("taskPriority").asInt());
        assertFalse(loggingNode.has("taskDueDate"));
        assertEquals(3, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow3, userTask --> theEnd", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("flow3", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("userTask", loggingNode.get("sourceRef").asText());
        assertEquals("theEnd", loggingNode.get("targetRef").asText());
        assertEquals(4, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In EndEvent, executing NoneEndEventActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theEnd", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("EndEvent", loggingNode.get("elementType").asText());
        assertEquals("NoneEndEventActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(5, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertEquals(LoggingSessionConstants.TYPE_PROCESS_COMPLETED, loggingNode.get("type").asText());
        assertEquals("Completed process instance with id " + processInstance.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(6, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(6);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(7, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/logging/asyncServiceTaskAndUserTask.bpmn20.xml")
    public void testAsyncServiceTask() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion().singleResult();
            
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
            
        assertEquals(5, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_PROCESS_STARTED, loggingNode.get("type").asText());
        assertEquals("Started process instance with id " + processInstance.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In StartEvent, executing NoneStartEventActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("theStart", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("StartEvent", loggingNode.get("elementType").asText());
        assertEquals("NoneStartEventActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(2, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow1, theStart --> task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("flow1", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("theStart", loggingNode.get("sourceRef").asText());
        assertEquals("task", loggingNode.get("targetRef").asText());
        assertEquals(3, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_ASYNC_JOB, loggingNode.get("type").asText());
        assertEquals("Created async job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertEquals(4, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals(5, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertEquals(18, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB, loggingNode.get("type").asText());
        assertEquals("Locking job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        Map<String, ObjectNode> loggingMap = new HashMap<>();
        int groupCounter = 1;
        int userCounter = 1;
        int commandContextCounter = 1;
        for (ObjectNode logObjectNode : FlowableLoggingListener.TEST_LOGGING_NODES) {
            String logType = logObjectNode.get("type").asText();
            if (LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE.equals(logType)) {
                logType = logObjectNode.get("elementType").asText() + logType;
            } else if (LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS.equals(logType)) {
                logType = groupCounter + logType;
                groupCounter++;
            } else if (LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS.equals(logType)) {
                logType = userCounter + logType;
                userCounter++;
            } else if (LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE.equals(logType)) {
                logType = commandContextCounter + logType;
                commandContextCounter++;
            }
            loggingMap.put(logType, logObjectNode);
        }
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB, loggingNode.get("type").asText());
        assertEquals("Executing async job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        int beforeJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(beforeJobNumber > 0);
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("ServiceTask" + LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In ServiceTask, executing ClassDelegate", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        assertEquals("ClassDelegate", loggingNode.get("activityBehavior").asText());
        int newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER, loggingNode.get("type").asText());
        assertEquals("Executing service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertEquals(LoggingSessionConstants.TYPE_VARIABLE_CREATE, loggingNode.get("type").asText());
        assertEquals("Variable 'newVariable' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertFalse(loggingNode.has("subScopeId"));
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("newVariable", loggingNode.get("variableName").asText());
        assertEquals("string", loggingNode.get("variableType").asText());
        assertEquals("test", loggingNode.get("variableValue").asText());
        assertEquals("test", loggingNode.get("variableRawValue").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT, loggingNode.get("type").asText());
        assertEquals("Executed service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate", loggingNode.get("elementSubType").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB);
        assertEquals(LoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB, loggingNode.get("type").asText());
        assertEquals("Unlocking job for task, with job id " + job.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("task", loggingNode.get("elementId").asText());
        assertEquals("Test task", loggingNode.get("elementName").asText());
        assertEquals("ServiceTask", loggingNode.get("elementType").asText());
        assertEquals(job.getId(), loggingNode.get("jobId").asText());
        assertNotNull(loggingNode.get(LoggingSessionUtil.LOG_NUMBER));
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow2, task --> userTask", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("flow2", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("task", loggingNode.get("sourceRef").asText());
        assertEquals("userTask", loggingNode.get("targetRef").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("UserTask" + LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In UserTask, executing UserTaskActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("UserTaskActivityBehavior", loggingNode.get("activityBehavior").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_USER_TASK_CREATE);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_CREATE, loggingNode.get("type").asText());
        assertEquals("User task 'Some user task' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertFalse(loggingNode.has("taskCategory"));
        assertFalse(loggingNode.has("taskDescription"));
        assertEquals("someKey", loggingNode.get("taskFormKey").asText());
        assertEquals(50, loggingNode.get("taskPriority").asInt());
        assertFalse(loggingNode.has("taskDueDate"));
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_USER_TASK_SET_ASSIGNEE);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_ASSIGNEE, loggingNode.get("type").asText());
        assertEquals("Set task assignee value to johndoe", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals("johndoe", loggingNode.get("taskAssignee").asText());
        assertFalse(loggingNode.has("taskCategory"));
        assertFalse(loggingNode.has("taskDescription"));
        assertFalse(loggingNode.has("taskFormKey"));
        assertFalse(loggingNode.has("taskPriority"));
        assertFalse(loggingNode.has("taskDueDate"));
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_USER_TASK_SET_OWNER);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_OWNER, loggingNode.get("type").asText());
        assertEquals("Set task owner value to janedoe", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals("janedoe", loggingNode.get("taskOwner").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("1" + LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 candidate group identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskGroupIdentityLinks").size());
        JsonNode identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        JsonNode identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("group1", identityLinkNode.get("groupId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("group2", identityLinkNode.get("groupId").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("1" + LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 candidate user identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskUserIdentityLinks").size());
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("johndoe", identityLinkNode.get("userId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("candidate", identityLinkNode.get("type").asText());
        assertEquals("janedoe", identityLinkNode.get("userId").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("2" + LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 custom user identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskUserIdentityLinks").size());
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("businessAdministrator", identityLinkNode.get("type").asText());
        assertEquals("johndoe", identityLinkNode.get("userId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("someAdmin", identityLinkNode.get("type").asText());
        assertEquals("janedoe", identityLinkNode.get("userId").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("2" + LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS, loggingNode.get("type").asText());
        assertEquals("Added 2 custom group identity links to task", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertEquals("userTask", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("Some user task", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("Some user task", loggingNode.get("taskName").asText());
        assertEquals(2, loggingNode.get("taskGroupIdentityLinks").size());
        identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("businessAdministrator", identityLinkNode.get("type").asText());
        assertEquals("group3", identityLinkNode.get("groupId").asText());
        identityLinkNode = identityLinksArray.get(1);
        assertNotNull(identityLinkNode.get("id").asText());
        assertEquals("someAdmin", identityLinkNode.get("type").asText());
        assertEquals("group4", identityLinkNode.get("groupId").asText());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertTrue(newJobNumber > beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("1" + LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertNotNull(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = loggingMap.get("2" + LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(processDefinition.getKey(), loggingNode.get("scopeDefinitionKey").asText());
        assertEquals(processDefinition.getName(), loggingNode.get("scopeDefinitionName").asText());
        assertNotNull(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());

        FlowableLoggingListener.clear();
    }

    public static class ExceptionServiceTaskDelegate implements JavaDelegate {
        
        @Override
        public void execute(DelegateExecution execution) {
            throw new FlowableException("Test exception");
        }
    }
    
    public static class VariableServiceTaskDelegate implements JavaDelegate {
        
        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("newVariable", "test");
        }
    }
}

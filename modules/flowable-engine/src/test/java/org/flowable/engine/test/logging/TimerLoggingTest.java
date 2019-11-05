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

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.common.engine.impl.logging.LoggingSessionUtil;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TimerLoggingTest extends ResourceFlowableTestCase {
    
    public TimerLoggingTest() {
        super("org/flowable/engine/test/logging/logging.test.flowable.cfg.xml");
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/logging/userTaskWithTimer.bpmn20.xml")
    public void testBoundaryTimerEvent() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("userTaskWithTimer").latestVersion().singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskWithTimer");
        Execution execution = runtimeService.createExecutionQuery().activityId("userTask1").processInstanceId(processInstance.getId()).singleResult();
        Execution timerExecution = runtimeService.createExecutionQuery().activityId("timerBoundaryEvent").processInstanceId(processInstance.getId()).singleResult();
            
        assertEquals(7, FlowableLoggingListener.TEST_LOGGING_NODES.size());
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertEquals(LoggingSessionConstants.TYPE_PROCESS_STARTED, loggingNode.get("type").asText());
        assertEquals("Started process instance with id " + processInstance.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals(1, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In StartEvent, executing NoneStartEventActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertNotNull(loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals("theStart", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("StartEvent", loggingNode.get("elementType").asText());
        assertEquals("NoneStartEventActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(2, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertEquals(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE, loggingNode.get("type").asText());
        assertEquals("Sequence flow will be taken for flow1, theStart --> userTask1", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals("flow1", loggingNode.get("elementId").asText());
        assertFalse(loggingNode.has("elementName"));
        assertEquals("SequenceFlow", loggingNode.get("elementType").asText());
        assertEquals("theStart", loggingNode.get("sourceRef").asText());
        assertEquals("userTask1", loggingNode.get("targetRef").asText());
        assertEquals(3, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertEquals(LoggingSessionConstants.TYPE_BOUNDARY_TIMER_EVENT_CREATE, loggingNode.get("type").asText());
        assertEquals("Creating boundary event (TimerEventDefinition) for execution id " + timerExecution.getId(), loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(timerExecution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals("timerBoundaryEvent", loggingNode.get("elementId").asText());
        assertEquals("BoundaryEvent", loggingNode.get("elementType").asText());
        assertEquals("TimerEventDefinition", loggingNode.get("elementSubType").asText());
        assertEquals(4, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertEquals(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE, loggingNode.get("type").asText());
        assertEquals("In UserTask, executing UserTaskActivityBehavior", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals("userTask1", loggingNode.get("elementId").asText());
        assertEquals("User task 1", loggingNode.get("elementName").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("UserTaskActivityBehavior", loggingNode.get("activityBehavior").asText());
        assertEquals(5, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertEquals(LoggingSessionConstants.TYPE_USER_TASK_CREATE, loggingNode.get("type").asText());
        assertEquals("User task 'User task 1' created", loggingNode.get("message").asText());
        assertEquals(processInstance.getId(), loggingNode.get("scopeId").asText());
        assertEquals(ScopeTypes.BPMN, loggingNode.get("scopeType").asText());
        assertEquals(execution.getId(), loggingNode.get("subScopeId").asText());
        assertEquals(processDefinition.getId(), loggingNode.get("scopeDefinitionId").asText());
        assertEquals("userTask1", loggingNode.get("elementId").asText());
        assertEquals("UserTask", loggingNode.get("elementType").asText());
        assertEquals("User task 1", loggingNode.get("elementName").asText());
        assertNotNull(loggingNode.get("taskId").asText());
        assertEquals("User task 1", loggingNode.get("taskName").asText());
        assertEquals(60, loggingNode.get("taskPriority").asInt());
        assertNotNull(loggingNode.get("taskDueDate").asText());
        assertEquals(6, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(6);
        assertEquals(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE, loggingNode.get("type").asText());
        assertEquals("Closed command context for bpmn engine", loggingNode.get("message").asText());
        assertEquals("bpmn", loggingNode.get("engineType").asText());
        assertEquals(7, loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt());
        assertNotNull(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText());
        
        FlowableLoggingListener.clear();
    }
}

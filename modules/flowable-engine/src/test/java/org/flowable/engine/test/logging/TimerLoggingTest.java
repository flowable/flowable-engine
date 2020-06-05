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

import static org.assertj.core.api.Assertions.assertThat;

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
    @Deployment(resources = "org/flowable/engine/test/logging/userTaskWithTimer.bpmn20.xml")
    public void testBoundaryTimerEvent() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("userTaskWithTimer").latestVersion()
                .singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("userTaskWithTimer");
        Execution execution = runtimeService.createExecutionQuery().activityId("userTask1").processInstanceId(processInstance.getId()).singleResult();
        Execution timerExecution = runtimeService.createExecutionQuery().activityId("timerBoundaryEvent").processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(7);

        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_PROCESS_STARTED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Started process instance with id " + processInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In StartEvent, executing NoneStartEventActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theStart");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("StartEvent");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("NoneStartEventActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow1, theStart --> userTask1");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow1");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("theStart");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("userTask1");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_BOUNDARY_TIMER_EVENT_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Creating boundary event (TimerEventDefinition) for execution id " + timerExecution.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(timerExecution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("timerBoundaryEvent");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("BoundaryEvent");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("TimerEventDefinition");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In UserTask, executing UserTaskActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask1");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("User task 1");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("UserTaskActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(5);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("User task 'User task 1' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask1");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("User task 1");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("User task 1");
        assertThat(loggingNode.get("taskPriority").asInt()).isEqualTo(60);
        assertThat(loggingNode.get("taskDueDate").asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(6);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(6);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(7);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
    }
}

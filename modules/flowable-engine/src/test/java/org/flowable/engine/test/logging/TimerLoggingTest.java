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

import tools.jackson.databind.node.ObjectNode;

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
        assertThat(loggingNode.get("type").asString()).isEqualTo(LoggingSessionConstants.TYPE_PROCESS_STARTED);
        assertThat(loggingNode.get("message").asString()).isEqualTo("Started process instance with id " + processInstance.getId());
        assertThat(loggingNode.get("scopeId").asString()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asString()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asString()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asString()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asString()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asString()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asString()).isEqualTo("In StartEvent, executing NoneStartEventActivityBehavior");
        assertThat(loggingNode.get("scopeId").asString()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asString()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asString()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asString()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asString()).isEqualTo("theStart");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asString()).isEqualTo("StartEvent");
        assertThat(loggingNode.get("activityBehavior").asString()).isEqualTo("NoneStartEventActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asString()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get("type").asString()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asString()).isEqualTo("Sequence flow will be taken for flow1, theStart --> userTask1");
        assertThat(loggingNode.get("scopeId").asString()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asString()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asString()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asString()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asString()).isEqualTo("flow1");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asString()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asString()).isEqualTo("theStart");
        assertThat(loggingNode.get("targetRef").asString()).isEqualTo("userTask1");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asString()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get("type").asString()).isEqualTo(LoggingSessionConstants.TYPE_BOUNDARY_TIMER_EVENT_CREATE);
        assertThat(loggingNode.get("message").asString()).isEqualTo("Creating boundary event (TimerEventDefinition) for execution id " + timerExecution.getId());
        assertThat(loggingNode.get("scopeId").asString()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asString()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asString()).isEqualTo(timerExecution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asString()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asString()).isEqualTo("timerBoundaryEvent");
        assertThat(loggingNode.get("elementType").asString()).isEqualTo("BoundaryEvent");
        assertThat(loggingNode.get("elementSubType").asString()).isEqualTo("TimerEventDefinition");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asString()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get("type").asString()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asString()).isEqualTo("In UserTask, executing UserTaskActivityBehavior");
        assertThat(loggingNode.get("scopeId").asString()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asString()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asString()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asString()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asString()).isEqualTo("userTask1");
        assertThat(loggingNode.get("elementName").asString()).isEqualTo("User task 1");
        assertThat(loggingNode.get("elementType").asString()).isEqualTo("UserTask");
        assertThat(loggingNode.get("activityBehavior").asString()).isEqualTo("UserTaskActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(5);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asString()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertThat(loggingNode.get("type").asString()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_CREATE);
        assertThat(loggingNode.get("message").asString()).isEqualTo("User task 'User task 1' created");
        assertThat(loggingNode.get("scopeId").asString()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asString()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asString()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asString()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("elementId").asString()).isEqualTo("userTask1");
        assertThat(loggingNode.get("elementType").asString()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asString()).isEqualTo("User task 1");
        assertThat(loggingNode.get("taskId").asString()).isNotNull();
        assertThat(loggingNode.get("taskName").asString()).isEqualTo("User task 1");
        assertThat(loggingNode.get("taskPriority").asInt()).isEqualTo(60);
        assertThat(loggingNode.get("taskDueDate").asString()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(6);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asString()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(6);
        assertThat(loggingNode.get("type").asString()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asString()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asString()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asString()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asString()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asString()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asString()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asString()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(7);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asString()).isNotNull();

        FlowableLoggingListener.clear();
    }
}

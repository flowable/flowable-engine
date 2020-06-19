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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.flowable.engine.test.bpmn.servicetask.TriggerableServiceTask;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ServiceTaskLoggingTest extends ResourceFlowableTestCase {

    public ServiceTaskLoggingTest() {
        super("org/flowable/engine/test/logging/logging.test.flowable.cfg.xml");
    }

    @AfterEach
    void tearDown() {
        FlowableLoggingListener.clear();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/serviceTask.bpmn20.xml")
    public void testServiceTaskException() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("startToEnd").latestVersion()
                .singleResult();

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("startToEnd"))
                .isInstanceOf(Exception.class);

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(7);

        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get(LoggingSessionUtil.ID).asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TRANSACTION_ID).asText()).isNotNull();
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_PROCESS_STARTED);
        assertThat(loggingNode.get("message").asText()).contains("Started process instance with id ");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get(LoggingSessionUtil.ID).asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TRANSACTION_ID).asText()).isNotNull();
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In StartEvent, executing NoneStartEventActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theStart");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("StartEvent");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("NoneStartEventActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get(LoggingSessionUtil.ID).asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TRANSACTION_ID).asText()).isNotNull();
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow1, theStart --> task");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow1");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("theStart");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("task");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get(LoggingSessionUtil.ID).asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TRANSACTION_ID).asText()).isNotNull();
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In ServiceTask, executing ClassDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementSubType").asText())
                .isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("ClassDelegate");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get(LoggingSessionUtil.ID).asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TRANSACTION_ID).asText()).isNotNull();
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Executing service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementSubType").asText())
                .isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(5);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertThat(loggingNode.get(LoggingSessionUtil.ID).asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TRANSACTION_ID).asText()).isNotNull();
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_EXCEPTION);
        assertThat(loggingNode.get("message").asText()).isEqualTo(
                "Service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate threw exception Test exception");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementSubType").asText())
                .isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$ExceptionServiceTaskDelegate");
        assertThat(loggingNode.get("exception").get("message").asText()).isEqualTo("Test exception");
        assertThat(loggingNode.get("exception").get("stackTrace").asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(6);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(6);
        assertThat(loggingNode.get(LoggingSessionUtil.ID).asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TRANSACTION_ID).asText()).isNotNull();
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Exception at closing command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(7);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        LoggingSessionLoggerOutput.printLogNodes(FlowableLoggingListener.TEST_LOGGING_NODES);
        FlowableLoggingListener.clear();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/logging/failingAsyncServiceTask.bpmn20.xml")
    public void testFailingServiceTaskException() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("failingServiceTask").latestVersion()
                .singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("failingServiceTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(5);

        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_PROCESS_STARTED);
        assertThat(loggingNode.get("message").asText()).contains("Started process instance with id ");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In StartEvent, executing NoneStartEventActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theStart");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("StartEvent");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("NoneStartEventActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow1, theStart --> task");
        assertThat(loggingNode.get("scopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isNotNull();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow1");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("theStart");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("task");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_ASYNC_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Created async job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(5);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(8);

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Locking job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

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
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Executing async job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        int beforeJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(beforeJobNumber).isGreaterThan(0);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("ServiceTask" + LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In ServiceTask, executing ServiceTaskExpressionActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("${failureExpressionValue}");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("ServiceTaskExpressionActivityBehavior");
        int newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Exception at closing command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(8);

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Locking job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testVariableCreateLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion()
                .singleResult();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(17);

        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;

        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_PROCESS_STARTED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Started process instance with id " + processInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In StartEvent, executing NoneStartEventActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theStart");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("StartEvent");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("NoneStartEventActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow1, theStart --> task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow1");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("theStart");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("task");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In ServiceTask, executing ClassDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("ClassDelegate");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Executing service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'newVariable' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("newVariable");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Executed service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow2, task --> userTask");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow2");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("task");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In UserTask, executing UserTaskActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("UserTaskActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("User task 'Some user task' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.has("taskCategory")).isFalse();
        assertThat(loggingNode.has("taskDescription")).isFalse();
        assertThat(loggingNode.get("taskFormKey").asText()).isEqualTo("someKey");
        assertThat(loggingNode.get("taskPriority").asInt()).isEqualTo(50);
        assertThat(loggingNode.has("taskDueDate")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_ASSIGNEE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Set task assignee value to johndoe");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskAssignee").asText()).isEqualTo("johndoe");
        assertThat(loggingNode.has("taskCategory")).isFalse();
        assertThat(loggingNode.has("taskDescription")).isFalse();
        assertThat(loggingNode.has("taskFormKey")).isFalse();
        assertThat(loggingNode.has("taskPriority")).isFalse();
        assertThat(loggingNode.has("taskDueDate")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_OWNER);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Set task owner value to janedoe");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskOwner").asText()).isEqualTo("janedoe");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 candidate group identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskGroupIdentityLinks")).hasSize(2);
        JsonNode identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        JsonNode identityLinkNode = identityLinksArray.get(0);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("candidate");
        assertThat(identityLinkNode.get("groupId").asText()).isEqualTo("group1");
        identityLinkNode = identityLinksArray.get(1);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("candidate");
        assertThat(identityLinkNode.get("groupId").asText()).isEqualTo("group2");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 candidate user identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskUserIdentityLinks")).hasSize(2);
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("candidate");
        assertThat(identityLinkNode.get("userId").asText()).isEqualTo("johndoe");
        identityLinkNode = identityLinksArray.get(1);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("candidate");
        assertThat(identityLinkNode.get("userId").asText()).isEqualTo("janedoe");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 custom user identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskUserIdentityLinks")).hasSize(2);
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("businessAdministrator");
        assertThat(identityLinkNode.get("userId").asText()).isEqualTo("johndoe");
        identityLinkNode = identityLinksArray.get(1);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("someAdmin");
        assertThat(identityLinkNode.get("userId").asText()).isEqualTo("janedoe");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 custom group identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskGroupIdentityLinks")).hasSize(2);
        identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        identityLinkNode = identityLinksArray.get(0);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("businessAdministrator");
        assertThat(identityLinkNode.get("groupId").asText()).isEqualTo("group3");
        identityLinkNode = identityLinksArray.get(1);
        assertThat(identityLinkNode.get("id").asText()).isNotNull();
        assertThat(identityLinkNode.get("type").asText()).isEqualTo("someAdmin");
        assertThat(identityLinkNode.get("groupId").asText()).isEqualTo("group4");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        LoggingSessionLoggerOutput.printLogNodes(FlowableLoggingListener.TEST_LOGGING_NODES);

        FlowableLoggingListener.clear();
        loggingItemCounter = 0;
        loggingNumberCounter = 1;

        runtimeService.setVariable(processInstance.getId(), "numVar", 123);
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(2);
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'numVar' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementType")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("numVar");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("integer");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("123");
        assertThat(loggingNode.get("variableRawValue").asInt()).isEqualTo(123);
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
        loggingItemCounter = 0;
        loggingNumberCounter = 1;

        runtimeService.setVariable(processInstance.getId(), "boolVar", true);
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(2);
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'boolVar' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementType")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("boolVar");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("boolean");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("true");
        assertThat(loggingNode.get("variableRawValue").asBoolean()).isTrue();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testVariableUpdateLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion()
                .singleResult();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(17);

        FlowableLoggingListener.clear();
        runtimeService.setVariable(processInstance.getId(), "newVariable", "updatedValue");
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(2);

        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_UPDATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'newVariable' updated");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementType")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("newVariable");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("updatedValue");
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("updatedValue");
        assertThat(loggingNode.get("oldVariableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("oldVariableValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get("oldVariableRawValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testVariableDeleteLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion()
                .singleResult();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(17);

        FlowableLoggingListener.clear();
        runtimeService.removeVariable(processInstance.getId(), "newVariable");
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(2);
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_DELETE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'newVariable' deleted");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementType")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("newVariable");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/logging/serviceAndUserTask.bpmn20.xml")
    public void testCompleteTaskLogging() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion()
                .singleResult();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(17);

        FlowableLoggingListener.clear();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("newVariable", "newValue");
        variableMap.put("numVar", 123);
        taskService.complete(task.getId(), variableMap);

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(7);
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_UPDATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'newVariable' updated");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("newVariable");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("newValue");
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("newValue");
        assertThat(loggingNode.get("oldVariableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("oldVariableValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get("oldVariableRawValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'numVar' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("numVar");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("integer");
        assertThat(loggingNode.get("variableValue").asInt()).isEqualTo(123);
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("123");

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_COMPLETE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("User task 'Some user task' completed");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.has("taskCategory")).isFalse();
        assertThat(loggingNode.has("taskDescription")).isFalse();
        assertThat(loggingNode.get("taskFormKey").asText()).isEqualTo("someKey");
        assertThat(loggingNode.get("taskPriority").asInt()).isEqualTo(50);
        assertThat(loggingNode.has("taskDueDate")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow3, userTask --> theEnd");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow3");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("theEnd");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In EndEvent, executing NoneEndEventActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theEnd");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("EndEvent");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("NoneEndEventActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(5);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_PROCESS_COMPLETED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Completed process instance with id " + processInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
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

    @Test
    @Deployment(resources = "org/flowable/engine/test/logging/asyncServiceTaskAndUserTask.bpmn20.xml")
    public void testAsyncServiceTask() {
        FlowableLoggingListener.clear();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("serviceAndUserTask").latestVersion()
                .singleResult();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("serviceAndUserTask");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(5);

        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_PROCESS_STARTED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Started process instance with id " + processInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In StartEvent, executing NoneStartEventActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theStart");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("StartEvent");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("NoneStartEventActivityBehavior");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow1, theStart --> task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow1");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("theStart");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("task");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_ASYNC_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Created async job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(5);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(18);

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Locking job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

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
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Executing async job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        int beforeJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(beforeJobNumber).isGreaterThan(0);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("ServiceTask" + LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In ServiceTask, executing ClassDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("ClassDelegate");
        int newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Executing service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'newVariable' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("newVariable");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("test");
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("test");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Executed service task with java class org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.engine.test.logging.ServiceTaskLoggingTest$VariableServiceTaskDelegate");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Unlocking job for task, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("task");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Test task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER)).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Sequence flow will be taken for flow2, task --> userTask");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("flow2");
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("SequenceFlow");
        assertThat(loggingNode.get("sourceRef").asText()).isEqualTo("task");
        assertThat(loggingNode.get("targetRef").asText()).isEqualTo("userTask");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("UserTask" + LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("In UserTask, executing UserTaskActivityBehavior");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("activityBehavior").asText()).isEqualTo("UserTaskActivityBehavior");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_USER_TASK_CREATE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("User task 'Some user task' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.has("taskCategory")).isFalse();
        assertThat(loggingNode.has("taskDescription")).isFalse();
        assertThat(loggingNode.get("taskFormKey").asText()).isEqualTo("someKey");
        assertThat(loggingNode.get("taskPriority").asInt()).isEqualTo(50);
        assertThat(loggingNode.has("taskDueDate")).isFalse();
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_USER_TASK_SET_ASSIGNEE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_ASSIGNEE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Set task assignee value to johndoe");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskAssignee").asText()).isEqualTo("johndoe");
        assertThat(loggingNode.has("taskCategory")).isFalse();
        assertThat(loggingNode.has("taskDescription")).isFalse();
        assertThat(loggingNode.has("taskFormKey")).isFalse();
        assertThat(loggingNode.has("taskPriority")).isFalse();
        assertThat(loggingNode.has("taskDueDate")).isFalse();
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get(LoggingSessionConstants.TYPE_USER_TASK_SET_OWNER);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_OWNER);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Set task owner value to janedoe");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskOwner").asText()).isEqualTo("janedoe");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("1" + LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 candidate group identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskGroupIdentityLinks")).hasSize(2);
        JsonNode identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        assertThatJson(identityLinksArray)
                .isEqualTo("[ {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'candidate',"
                        + "  groupId: 'group1'"
                        + "}, {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'candidate',"
                        + "  groupId: 'group2'"
                        + " } ]");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("1" + LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 candidate user identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskUserIdentityLinks")).hasSize(2);
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        assertThatJson(identityLinksArray)
                .isEqualTo("[ {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'candidate',"
                        + "  userId: 'johndoe'"
                        + "}, {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'candidate',"
                        + "  userId: 'janedoe'"
                        + " } ]");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("2" + LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_USER_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 custom user identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskUserIdentityLinks")).hasSize(2);
        identityLinksArray = loggingNode.get("taskUserIdentityLinks");
        assertThatJson(identityLinksArray)
                .isEqualTo("[ {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'businessAdministrator',"
                        + "  userId: 'johndoe'"
                        + "}, {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'someAdmin',"
                        + "  userId: 'janedoe'"
                        + " } ]");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("2" + LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_USER_TASK_SET_GROUP_IDENTITY_LINKS);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Added 2 custom group identity links to task");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(execution.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("userTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("UserTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Some user task");
        assertThat(loggingNode.get("taskGroupIdentityLinks")).hasSize(2);
        identityLinksArray = loggingNode.get("taskGroupIdentityLinks");
        assertThatJson(identityLinksArray)
                .isEqualTo("[ {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'businessAdministrator',"
                        + "  groupId: 'group3'"
                        + "}, {"
                        + "  id: '${json-unit.any-string}',"
                        + "  type: 'someAdmin',"
                        + "  groupId: 'group4'"
                        + " } ]");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("1" + LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        loggingNode = loggingMap.get("2" + LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for bpmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("bpmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(processInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.BPMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(processDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(processDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(processDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();

        FlowableLoggingListener.clear();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/TriggerableServiceTaskTest.testDelegateExpression.bpmn20.xml")
    void testTriggerableServiceTaskWithDelegateExpression() {
        FlowableLoggingListener.clear();

        TriggerableServiceTask delegate = new TriggerableServiceTask();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .transientVariable("triggerableServiceTask", delegate)
                .start();

        String processId = processInstance.getId();
        String processDefinitionId = processInstance.getProcessDefinitionId();

        List<ObjectNode> loggingNodes = FlowableLoggingListener.TEST_LOGGING_NODES;
        assertThat(loggingNodes)
                .extracting(node -> node.path("type").asText("__invalid"))
                .containsExactly(
                        LoggingSessionConstants.TYPE_PROCESS_STARTED,
                        LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE,
                        LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE,
                        LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE,
                        LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER,
                        LoggingSessionConstants.TYPE_VARIABLE_CREATE,
                        LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT,
                        LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE
                );

        assertThatJson(loggingNodes.get(0))
                .isEqualTo("{"
                        + "  type: 'processStarted',"
                        + "  message: 'Started process instance with id " + processId + "',"
                        + "  scopeId: '" + processId + "',"
                        + "  subScopeId: '${json-unit.any-string}',"
                        + "  scopeType: 'bpmn',"
                        + "  scopeDefinitionId: '" + processDefinitionId + "',"
                        + "  scopeDefinitionKey: 'process',"
                        + "  scopeDefinitionName: null,"
                        + "  __id: '${json-unit.any-string}',"
                        + "  __timeStamp: '${json-unit.any-string}',"
                        + "  __transactionId: '${json-unit.any-string}',"
                        + "  __logNumber: 1"
                        + "}");

        assertThatJson(loggingNodes.get(3))
                .isEqualTo("{"
                        + "  type: 'activityBehaviorExecute',"
                        + "  message: 'In ServiceTask, executing ServiceTaskDelegateExpressionActivityBehavior',"
                        + "  scopeId: '" + processId + "',"
                        + "  subScopeId: '${json-unit.any-string}',"
                        + "  scopeType: 'bpmn',"
                        + "  scopeDefinitionId: '" + processDefinitionId + "',"
                        + "  scopeDefinitionKey: 'process',"
                        + "  scopeDefinitionName: null,"
                        + "  elementId: 'service1',"
                        + "  elementType: 'ServiceTask',"
                        + "  elementSubType: '${triggerableServiceTask}',"
                        + "  activityBehavior: 'ServiceTaskDelegateExpressionActivityBehavior',"
                        + "  __id: '${json-unit.any-string}',"
                        + "  __timeStamp: '${json-unit.any-string}',"
                        + "  __transactionId: '${json-unit.any-string}',"
                        + "  __logNumber: 4"
                        + "}");

        assertThatJson(loggingNodes.get(4))
                .isEqualTo("{"
                        + "  type: 'serviceTaskEnter',"
                        + "  message: 'Executing service task with delegate " + delegate + "',"
                        + "  scopeId: '" + processId + "',"
                        + "  subScopeId: '${json-unit.any-string}',"
                        + "  scopeType: 'bpmn',"
                        + "  scopeDefinitionId: '" + processDefinitionId + "',"
                        + "  scopeDefinitionKey: 'process',"
                        + "  scopeDefinitionName: null,"
                        + "  elementId: 'service1',"
                        + "  elementType: 'ServiceTask',"
                        + "  elementSubType: '${triggerableServiceTask}',"
                        + "  __id: '${json-unit.any-string}',"
                        + "  __timeStamp: '${json-unit.any-string}',"
                        + "  __transactionId: '${json-unit.any-string}',"
                        + "  __logNumber: 5"
                        + "}");

        assertThatJson(loggingNodes.get(6))
                .isEqualTo("{"
                        + "  type: 'serviceTaskExit',"
                        + "  message: 'Executed service task with delegate " + delegate + "',"
                        + "  scopeId: '" + processId + "',"
                        + "  subScopeId: '${json-unit.any-string}',"
                        + "  scopeType: 'bpmn',"
                        + "  scopeDefinitionId: '" + processDefinitionId + "',"
                        + "  scopeDefinitionKey: 'process',"
                        + "  scopeDefinitionName: null,"
                        + "  elementId: 'service1',"
                        + "  elementType: 'ServiceTask',"
                        + "  elementSubType: '${triggerableServiceTask}',"
                        + "  __id: '${json-unit.any-string}',"
                        + "  __timeStamp: '${json-unit.any-string}',"
                        + "  __transactionId: '${json-unit.any-string}',"
                        + "  __logNumber: 7"
                        + "}");

        FlowableLoggingListener.clear();

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processId).activityId("service1").singleResult();
        runtimeService.trigger(execution.getId(), Collections.emptyMap(), Collections.singletonMap("triggerableServiceTask", delegate));

        loggingNodes = FlowableLoggingListener.TEST_LOGGING_NODES;

        assertThat(loggingNodes)
                .extracting(node -> node.path("type").asText("__invalid"))
                .containsExactly(
                        LoggingSessionConstants.TYPE_SERVICE_TASK_BEFORE_TRIGGER,
                        LoggingSessionConstants.TYPE_VARIABLE_UPDATE,
                        LoggingSessionConstants.TYPE_SERVICE_TASK_AFTER_TRIGGER,
                        LoggingSessionConstants.TYPE_SEQUENCE_FLOW_TAKE,
                        LoggingSessionConstants.TYPE_ACTIVITY_BEHAVIOR_EXECUTE,
                        LoggingSessionConstants.TYPE_USER_TASK_CREATE,
                        LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE
                );

        assertThatJson(loggingNodes.get(0))
                .isEqualTo("{"
                        + "  type: 'serviceTaskBeforeTrigger',"
                        + "  message: 'Triggering service task with delegate " + delegate + "',"
                        + "  scopeId: '" + processId + "',"
                        + "  subScopeId: '${json-unit.any-string}',"
                        + "  scopeType: 'bpmn',"
                        + "  scopeDefinitionId: '" + processDefinitionId + "',"
                        + "  scopeDefinitionKey: 'process',"
                        + "  scopeDefinitionName: null,"
                        + "  elementId: 'service1',"
                        + "  elementType: 'ServiceTask',"
                        + "  elementSubType: '${triggerableServiceTask}',"
                        + "  __id: '${json-unit.any-string}',"
                        + "  __timeStamp: '${json-unit.any-string}',"
                        + "  __transactionId: '${json-unit.any-string}',"
                        + "  __logNumber: 1"
                        + "}");

        assertThatJson(loggingNodes.get(2))
                .isEqualTo("{"
                        + "  type: 'serviceTaskAfterTrigger',"
                        + "  message: 'Triggered service task with delegate " + delegate + "',"
                        + "  scopeId: '" + processId + "',"
                        + "  subScopeId: '${json-unit.any-string}',"
                        + "  scopeType: 'bpmn',"
                        + "  scopeDefinitionId: '" + processDefinitionId + "',"
                        + "  scopeDefinitionKey: 'process',"
                        + "  scopeDefinitionName: null,"
                        + "  elementId: 'service1',"
                        + "  elementType: 'ServiceTask',"
                        + "  elementSubType: '${triggerableServiceTask}',"
                        + "  __id: '${json-unit.any-string}',"
                        + "  __timeStamp: '${json-unit.any-string}',"
                        + "  __transactionId: '${json-unit.any-string}',"
                        + "  __logNumber: 3"
                        + "}");

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

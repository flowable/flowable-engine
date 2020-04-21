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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableException;
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
        
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(6);
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_CASE_STARTED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Started case instance with id " + caseInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance created with type humantask, new state available");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("available");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Human task 'The Task' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("taskId").asText()).isEqualTo(task.getId());
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_SET_ASSIGNEE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Set task assignee value to johnDoe");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("taskId").asText()).isEqualTo(task.getId());
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance state change with type humantask, old state available, new state active");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("active");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/logging/oneServiceTaskCase.cmmn")
    public void testServiceTaskLogging() {
        FlowableLoggingListener.clear();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneServiceTaskCase").latestVersion().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneServiceTaskCase").start();
        
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(9);
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_CASE_STARTED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Started case instance with id " + caseInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance created with type servicetask, new state available");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("available");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Executing service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'javaDelegate' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("javaDelegate");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("string");
        assertThat(loggingNode.get("variableValue").asText()).isEqualTo("executed");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("executed");
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Executed service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Plan item instance state change with type servicetask, old state available, new state active");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("active");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Plan item instance state change with type servicetask, old state active, new state completed");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("completed");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("active");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("completed");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Completed case instance with id " + caseInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
    }
    
    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/logging/oneHumanTaskCase.cmmn")
    public void testCompleteTaskLogging() {
        FlowableLoggingListener.clear();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").latestVersion().singleResult();
            
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .variable("newVariable", "test")
                        .caseDefinitionKey("oneHumanTaskCase")
                        .start();
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(7);
        
        FlowableLoggingListener.clear();
        
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("newVariable", "newValue");
        variableMap.put("numVar", 123);
        cmmnRuntimeService.setVariables(caseInstance.getId(), variableMap);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(7);
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_VARIABLE_UPDATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Variable 'newVariable' updated");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
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
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.has("subScopeId")).isFalse();
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("variableName").asText()).isEqualTo("numVar");
        assertThat(loggingNode.get("variableType").asText()).isEqualTo("integer");
        assertThat(loggingNode.get("variableValue").asInt()).isEqualTo(123);
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get("variableRawValue").asText()).isEqualTo("123");
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_COMPLETE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Human task 'The Task' completed");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.has("taskCategory")).isFalse();
        assertThat(loggingNode.has("taskDescription")).isFalse();
        assertThat(loggingNode.get("taskFormKey").asText()).isEqualTo("someKey");
        assertThat(loggingNode.get("taskPriority").asInt()).isEqualTo(50);
        assertThat(loggingNode.has("taskDueDate")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance state change with type humantask, old state active, new state completed");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("completed");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("active");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("completed");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(5);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Completed case instance with id " + caseInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(6);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        FlowableLoggingListener.clear();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/logging/sentryConditionCase.cmmn")
    public void testSentryConditionLogging() {
        FlowableLoggingListener.clear();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("conditionCase").latestVersion().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("conditionCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                        .planItemDefinitionId(task.getTaskDefinitionKey())
                        .singleResult();
        
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(6);
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_CASE_STARTED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Started case instance with id " + caseInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance created with type humantask, new state available");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("taskA");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Task A");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("available");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance created with type stage, new state available");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("stage1");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Stage 1");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("Stage");
        assertThat(loggingNode.get("state").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("available");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_CREATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Human task 'Task A' created");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("taskId").asText()).isEqualTo(task.getId());
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Task A");
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("taskA");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Task A");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance state change with type humantask, old state available, new state active");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("taskA");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Task A");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("active");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        FlowableLoggingListener.clear();

        assertThatThrownBy(() -> cmmnTaskService.complete(task.getId()))
                .isInstanceOf(FlowableException.class);
        
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(5);

        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_HUMAN_TASK_COMPLETE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Human task 'Task A' completed");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("taskA");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Task A");
        assertThat(loggingNode.get("taskId").asText()).isNotNull();
        assertThat(loggingNode.get("taskName").asText()).isEqualTo("Task A");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(1);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance state change with type humantask, old state active, new state completed");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("taskA");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Task A");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("HumanTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("completed");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("active");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("completed");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(2);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(2);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_EVALUATE_SENTRY);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Evaluate sentry parts for Stage 1");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("stage1");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Stage 1");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("Stage");
        assertThat(loggingNode.get("onParts")).hasSize(1);
        assertThat(loggingNode.get("onParts").get(0).get("id").asText()).isEqualTo("sentryOnPart1");
        assertThat(loggingNode.get("onParts").get(0).get("source").asText()).isEqualTo("planItem1");
        assertThat(loggingNode.get("onParts").get(0).get("elementId").asText()).isEqualTo("taskA");
        assertThat(loggingNode.get("onParts").get(0).get("standardEvent").asText()).isEqualTo("complete");
        assertThat(loggingNode.get("ifPart").get("condition").asText()).isEqualTo("${gotoStage1}");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(3);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(3);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_EVALUATE_SENTRY_FAILED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("IfPart evaluation failed for Stage 1");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("stage1");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("Stage 1");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("Stage");
        assertThat(loggingNode.get("exception").get("message").asText()).isEqualTo("Unknown property used in expression: ${gotoStage1}");
        assertThat(loggingNode.get("exception").get("stackTrace").asText()).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(4);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(4);
        assertThat(loggingNode.get("type").asText()).isEqualTo(LoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE_FAILURE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Exception at closing command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(5);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        FlowableLoggingListener.clear();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/logging/oneAsyncServiceTaskCase.cmmn")
    public void testAsyncServiceTaskLogging() {
        FlowableLoggingListener.clear();
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneServiceTaskCase").latestVersion().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneServiceTaskCase").start();
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("theTask").singleResult();
        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(5);
        
        int loggingItemCounter = 0;
        int loggingNumberCounter = 1;
        
        ObjectNode loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_CASE_STARTED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Started case instance with id " + caseInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_CREATED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Plan item instance created with type servicetask, new state available");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("available");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ASYNC_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Created async job for theTask, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Plan item instance state change with type servicetask, old state available, new state async-active");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("async-active");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("available");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("async-active");
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(loggingItemCounter++);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(loggingNumberCounter++);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        FlowableLoggingListener.clear();
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000, 200, true);
        assertThat(FlowableLoggingListener.TEST_LOGGING_NODES).hasSize(11);
        
        loggingNode = FlowableLoggingListener.TEST_LOGGING_NODES.get(0);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Locking job for theTask, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("elementSubType").asText()).isEqualTo("org.flowable.cmmn.test.delegate.TestJavaDelegate");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt()).isEqualTo(1);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
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
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXECUTE_ASYNC_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Executing async job for theTask, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        int beforeJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(beforeJobNumber).isGreaterThan(0);
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ENTER);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Executing service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        int newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_EXIT);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Executed service task with java class org.flowable.cmmn.test.delegate.TestJavaDelegate");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Unlocking job for theTask, with job id " + job.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("jobId").asText()).isEqualTo(job.getId());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER)).isNotNull();
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = loggingMap.get("1" + CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Plan item instance state change with type servicetask, old state async-active, new state active");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("active");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("async-active");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("active");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = loggingMap.get("2" + CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_PLAN_ITEM_NEW_STATE);
        assertThat(loggingNode.get("message").asText())
                .isEqualTo("Plan item instance state change with type servicetask, old state active, new state completed");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("subScopeId").asText()).isEqualTo(planItemInstance.getId());
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get("elementId").asText()).isEqualTo("theTask");
        assertThat(loggingNode.get("elementName").asText()).isEqualTo("The Task");
        assertThat(loggingNode.get("elementType").asText()).isEqualTo("ServiceTask");
        assertThat(loggingNode.get("state").asText()).isEqualTo("completed");
        assertThat(loggingNode.get("oldState").asText()).isEqualTo("active");
        assertThat(loggingNode.get("newState").asText()).isEqualTo("completed");
        newJobNumber = loggingNode.get(LoggingSessionUtil.LOG_NUMBER).asInt();
        assertThat(newJobNumber).isGreaterThan(beforeJobNumber);
        beforeJobNumber = newJobNumber;
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).asText()).isNotNull();
        
        loggingNode = loggingMap.get(CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_CASE_COMPLETED);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Completed case instance with id " + caseInstance.getId());
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.has("elementId")).isFalse();
        assertThat(loggingNode.has("elementName")).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).isNull()).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).isNull()).isFalse();
        
        loggingNode = loggingMap.get("1" + CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).isNull()).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).isNull()).isFalse();
        
        loggingNode = loggingMap.get("2" + CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("type").asText()).isEqualTo(CmmnLoggingSessionConstants.TYPE_COMMAND_CONTEXT_CLOSE);
        assertThat(loggingNode.get("message").asText()).isEqualTo("Closed command context for cmmn engine");
        assertThat(loggingNode.get("engineType").asText()).isEqualTo("cmmn");
        assertThat(loggingNode.get("scopeId").asText()).isEqualTo(caseInstance.getId());
        assertThat(loggingNode.get("scopeType").asText()).isEqualTo(ScopeTypes.CMMN);
        assertThat(loggingNode.get("scopeDefinitionId").asText()).isEqualTo(caseDefinition.getId());
        assertThat(loggingNode.get("scopeDefinitionKey").asText()).isEqualTo(caseDefinition.getKey());
        assertThat(loggingNode.get("scopeDefinitionName").asText()).isEqualTo(caseDefinition.getName());
        assertThat(loggingNode.get(LoggingSessionUtil.LOG_NUMBER).isNull()).isFalse();
        assertThat(loggingNode.get(LoggingSessionUtil.TIMESTAMP).isNull()).isFalse();
    }
}

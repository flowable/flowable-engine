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

package org.flowable.engine.test.bpmn.sequenceflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class ConditionalSequenceFlowTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testUelExpression() {
        Map<String, Object> variables = CollectionUtil.singletonMap("input", "right");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("condSeqFlowUelExpr", variables);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task right");
    }

    @Test
    @Deployment
    public void testSkipExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", "right");
        variables.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables.put("skipLeft", true);
        variables.put("skipRight", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testSkipExpression", variables);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task left");
    }
    
    @Test
    @Deployment
    public void testSkipExpressionWithDefinitionInfoEnabled() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("testSkipExpression").singleResult();
        ObjectNode infoNode = dynamicBpmnService.enableSkipExpression();
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("input", "left");
        variables.put("skipLeft", false);
        variables.put("skipRight", true);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testSkipExpression", variables);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task right");
        
        dynamicBpmnService.removeEnableSkipExpression(infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);
        infoNode = dynamicBpmnService.getProcessDefinitionInfo(processDefinition.getId());
        assertThat(infoNode.get("bpmn").has(DynamicBpmnConstants.GLOBAL_PROCESS_DEFINITION_PROPERTIES)).isTrue();
        assertThat(infoNode.get("bpmn").get(DynamicBpmnConstants.GLOBAL_PROCESS_DEFINITION_PROPERTIES).has(DynamicBpmnConstants.ENABLE_SKIP_EXPRESSION)).isFalse();
        
        variables = new HashMap<>();
        variables.put("input", "left");
        variables.put("skipLeft", false);
        variables.put("skipRight", true);
        pi = runtimeService.startProcessInstanceByKey("testSkipExpression", variables);

        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task left");
        
        dynamicBpmnService.enableSkipExpression(infoNode);
        dynamicBpmnService.changeSkipExpression("flow1", "${skipOtherLeftVar}", infoNode);
        dynamicBpmnService.changeSkipExpression("flow2", "${skipOtherRightVar}", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);
        infoNode = dynamicBpmnService.getProcessDefinitionInfo(processDefinition.getId());
        assertThat(infoNode.get("bpmn").has(DynamicBpmnConstants.GLOBAL_PROCESS_DEFINITION_PROPERTIES)).isTrue();
        assertThat(infoNode.get("bpmn").get(DynamicBpmnConstants.GLOBAL_PROCESS_DEFINITION_PROPERTIES).has(DynamicBpmnConstants.ENABLE_SKIP_EXPRESSION)).isTrue();
        assertThat(infoNode.get("bpmn").get("flow1").get(DynamicBpmnConstants.TASK_SKIP_EXPRESSION).asText()).isEqualTo("${skipOtherLeftVar}");
        assertThat(infoNode.get("bpmn").get("flow2").get(DynamicBpmnConstants.TASK_SKIP_EXPRESSION).asText()).isEqualTo("${skipOtherRightVar}");
        
        variables = new HashMap<>();
        variables.put("input", "left");
        variables.put("skipOtherLeftVar", false);
        variables.put("skipOtherRightVar", true);
        pi = runtimeService.startProcessInstanceByKey("testSkipExpression", variables);

        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task right");
    }

    @Test
    @Deployment
    public void testDynamicExpression() {
        Map<String, Object> variables = CollectionUtil.singletonMap("input", "right");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("condSeqFlowUelExpr", variables);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task not left");
        taskService.complete(task.getId());

        ObjectNode infoNode = dynamicBpmnService.changeSequenceFlowCondition("flow1", "${input == 'right'}");
        dynamicBpmnService.changeSequenceFlowCondition("flow2", "${input != 'right'}", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(pi.getProcessDefinitionId(), infoNode);

        pi = runtimeService.startProcessInstanceByKey("condSeqFlowUelExpr", variables);

        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task left");
        taskService.complete(task.getId());

        variables = CollectionUtil.singletonMap("input", "right2");
        pi = runtimeService.startProcessInstanceByKey("condSeqFlowUelExpr", variables);

        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(task.getName()).isEqualTo("task not left");
        taskService.complete(task.getId());
    }
}

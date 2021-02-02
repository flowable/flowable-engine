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
package org.flowable.examples.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableClassLoadingException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 */
public class JavaServiceTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testJavaServiceDelegation() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("javaServiceDelegation", CollectionUtil.singletonMap("input", "Flowable BPM Engine"));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).activityId("waitState").singleResult();
        assertThat(runtimeService.getVariable(execution.getId(), "input")).isEqualTo("FLOWABLE BPM ENGINE");
    }

    @Test
    @Deployment
    public void testFieldInjection() {
        // Process contains 2 service-tasks using field-injection. One should
        // use the exposed setter,
        // the other is using the private field.
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("fieldInjection");
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).activityId("waitState").singleResult();

        assertThat(runtimeService.getVariable(execution.getId(), "var")).isEqualTo("HELLO WORLD");
        assertThat(runtimeService.getVariable(execution.getId(), "setterVar")).isEqualTo("HELLO SETTER");
    }

    @Test
    @Deployment
    public void testExpressionFieldInjection() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "kermit");
        vars.put("gender", "male");
        vars.put("genderBean", new GenderBean());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("expressionFieldInjection", vars);
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).activityId("waitState").singleResult();

        assertThat(runtimeService.getVariable(execution.getId(), "var2")).isEqualTo("timrek .rM olleH");
        assertThat(runtimeService.getVariable(execution.getId(), "var1")).isEqualTo("elam :si redneg ruoY");
    }
    
    @Test
    @Deployment
    public void testServiceTaskWithSkipExpression() {
      Map<String, Object> vars = new HashMap<>();
      vars.put("input", "test");
      vars.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
      vars.put("skip", true);
      
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("serviceTask", vars);
      
      Execution waitExecution = runtimeService.createExecutionQuery().processInstanceId(pi.getProcessInstanceId()).onlyChildExecutions().singleResult();
      assertThat(waitExecution).isNotNull();
      assertThat(waitExecution.getActivityId()).isEqualTo("waitState");
    }
    
    @Test
    @Deployment
    public void testAsyncServiceTaskWithSkipExpression() {
      Map<String, Object> vars = new HashMap<>();
      vars.put("input", "test");
      
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("asyncServiceTask", vars);
      Job job = managementService.createJobQuery().processInstanceId(pi.getProcessInstanceId()).singleResult();
      assertThat(job).isNotNull();
      
      vars = new HashMap<>();
      vars.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
      vars.put("skip", true);
      runtimeService.setVariables(pi.getProcessInstanceId(), vars);
      
      managementService.executeJob(job.getId());
      
      Execution waitExecution = runtimeService.createExecutionQuery().processInstanceId(pi.getProcessInstanceId()).onlyChildExecutions().singleResult();
      assertThat(waitExecution).isNotNull();
      assertThat(waitExecution.getActivityId()).isEqualTo("waitState");
    }

    @Test
    @Deployment
    public void testExpressionFieldInjectionWithSkipExpression() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "kermit");
        vars.put("gender", "male");
        vars.put("genderBean", new GenderBean());
        vars.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        vars.put("skip", false);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("expressionFieldInjectionWithSkipExpression", vars);
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).activityId("waitState").singleResult();

        assertThat(execution).isNotNull();

        assertThat(runtimeService.getVariable(execution.getId(), "var2")).isEqualTo("timrek .rM olleH");
        assertThat(runtimeService.getVariable(execution.getId(), "var1")).isEqualTo("elam :si redneg ruoY");

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("name", "kermit");
        vars2.put("gender", "male");
        vars2.put("genderBean", new GenderBean());
        vars2.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        vars2.put("skip", true);

        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("expressionFieldInjectionWithSkipExpression", vars2);
        Execution execution2 = runtimeService.createExecutionQuery().processInstanceId(pi2.getId()).activityId("waitState").singleResult();

        assertThat(execution2).isNotNull();

        Map<String, Object> pi2VarMap = runtimeService.getVariables(pi2.getProcessInstanceId());
        assertThat(pi2VarMap)
                .doesNotContainKey("var1")
                .doesNotContainKey("var2");
    }

    @Test
    @Deployment
    public void testUnexistingClassDelegation() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("unexistingClassDelegation"))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("couldn't instantiate class org.flowable.BogusClass")
                .hasCauseInstanceOf(FlowableClassLoadingException.class);
    }

    @Test
    public void testIllegalUseOfResultVariableName() {
        assertThatThrownBy(() -> deploymentIdsForAutoCleanup.add(
                repositoryService.createDeployment().addClasspathResource("org/flowable/examples/bpmn/servicetask/JavaServiceTaskTest.testIllegalUseOfResultVariableName.bpmn20.xml").deploy().getId()))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("resultVariable");
    }

    @Test
    @Deployment
    public void testExceptionHandling() {

        // If variable value is != 'throw-exception', process goes
        // through service task and ends immediately
        Map<String, Object> vars = new HashMap<>();
        vars.put("var", "no-exception");
        runtimeService.startProcessInstanceByKey("exceptionHandling", vars);
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

        // If variable value == 'throw-exception', process executes
        // service task, which generates and catches exception,
        // and takes sequence flow to user task
        vars.put("var", "throw-exception");
        runtimeService.startProcessInstanceByKey("exceptionHandling", vars);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Fix Exception");
    }

    @Test
    @Deployment
    public void testGetBusinessKeyFromDelegateExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("businessKeyProcess", "1234567890");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("businessKeyProcess").count()).isEqualTo(1);

        // Check if business-key was available from the process
        String key = (String) runtimeService.getVariable(processInstance.getId(), "businessKeySetOnExecution");
        assertThat(key).isEqualTo("1234567890");
    }

}

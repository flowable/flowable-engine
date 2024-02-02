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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Christian Stettler
 */
public class ExpressionServiceTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSetServiceResultToProcessVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("bean", new ValueBean("ok"));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setServiceResultToProcessVariables", variables);
        assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo("ok");
    }

    @Test
    @Deployment
    public void testSetServiceResultToProcessVariablesWithSkipExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("bean", new ValueBean("ok"));
        variables.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables.put("skip", false);
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setServiceResultToProcessVariablesWithSkipExpression", variables);
        assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo("ok");

        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("bean", new ValueBean("ok"));
        variables2.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables2.put("skip", true);
        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("setServiceResultToProcessVariablesWithSkipExpression", variables2);
        assertThat(runtimeService.getVariable(pi2.getId(), "result")).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricActivityInstance skipActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(pi2.getId())
                    .activityId("valueExpressionServiceWithResultVariableNameSet")
                    .singleResult();
            assertActivityInstancesAreSame(skipActivityInstance,
                    runtimeService.createActivityInstanceQuery().activityInstanceId(skipActivityInstance.getId()).singleResult());

            assertThat(skipActivityInstance).isNotNull();
        }

        Map<String, Object> variables3 = new HashMap<>();
        variables3.put("bean", new ValueBean("okBean"));
        variables3.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", false);
        ProcessInstance pi3 = runtimeService.startProcessInstanceByKey("setServiceResultToProcessVariablesWithSkipExpression", variables3);
        assertThat(runtimeService.getVariable(pi3.getId(), "result")).isEqualTo("okBean");
    }

    @Test
    @Deployment
    public void testBackwardsCompatibleExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("var", "---");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("BackwardsCompatibleExpressionProcess", variables);
        assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo("...---...");
    }

    @Test
    @Deployment
    public void testSetServiceResultWithParallelMultiInstance() {
        Map<String, Object> variables = new HashMap<>();
        List<ValueBean> beans = Arrays.asList(new ValueBean("OK"), new ValueBean("NOT_OK"));
        variables.put("beans", beans);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setServiceResultToWithParallelMultiInstance", variables);

        assertThat(runtimeService.getVariable(pi.getId(), "subProcessVar")).isEqualTo("NOT_OK");

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(pi.getProcessInstanceId())
                .list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("processWaitState");
    }

    @Test
    @Deployment
    public void testSetServiceLocalScopedResultWithParallelMultiInstance() {
        Map<String, Object> variables = new HashMap<>();
        List<ValueBean> beans = Arrays.asList(new ValueBean("OK"), new ValueBean("NOT_OK"));
        variables.put("beans", beans);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setServiceLocalScopedResultWithParallelMultiInstance", variables);

        assertThat(runtimeService.getVariable(pi.getId(), "subProcessVar"))
                .as("subProcessVar should not be on process instance scope")
                .isNull();

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(pi.getProcessInstanceId())
                .list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("subProcessWaitState");
    }

    @Test
    @Deployment
    public void testVarargs() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("bean", new TestVarargsBean());
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testVarargsInExpression", variables);
        assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo("ABC");
    }
}

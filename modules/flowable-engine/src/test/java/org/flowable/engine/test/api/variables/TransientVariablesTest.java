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
package org.flowable.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.TaskCompletionBuilder;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class TransientVariablesTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSetTransientVariableInServiceTask() {

        // Process has two service task: first sets transient vars,
        // second then processes transient var and puts data in real vars.
        // (mimicking a service + processing call)
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String message = (String) taskService.getVariable(task.getId(), "message");
        assertThat(message).isEqualTo("Hello World!");

        // Variable should not be there after user task
        assertThat(runtimeService.getVariable(processInstance.getId(), "response")).isNull();
    }

    @Test
    @Deployment
    public void testUseTransientVariableInExclusiveGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("responseOk");

        // Variable should not be there after user task
        assertThat(runtimeService.getVariable(processInstance.getId(), "response")).isNull();
    }

    @Test
    @Deployment
    public void testTaskCompleteWithTransientVariables() {
        Map<String, Object> persistentVars = new HashMap<>();
        persistentVars.put("persistentVar1", "Hello World");
        persistentVars.put("persistentVar2", 987654321);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest", persistentVars);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("My Task");

        persistentVars.clear();
        Map<String, Object> transientVars = new HashMap<>();
        transientVars.put("unusedTransientVar", "Hello there");
        transientVars.put("transientVar", "OK");

        TaskCompletionBuilder taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder
                .taskId(task.getId())
                .transientVariables(transientVars)
                .variables(persistentVars)
                .complete();

        // Combined var has been set by execution listener
        String combinedVar = (String) runtimeService.getVariable(processInstance.getId(), "combinedVar");
        assertThat(combinedVar).isEqualTo("Hello WorldABC123");

        assertThat(runtimeService.getVariable(processInstance.getId(), "persistentVar1")).isNotNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "persistentVar2")).isNotNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "unusedTransientVar")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "transientVar")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "secondTransientVar")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "thirdTransientVar")).isNull();
    }

    @Test
    @Deployment
    public void testTaskResolveWithTransientVariables() {
        Map<String, Object> persistentVars = new HashMap<>();
        persistentVars.put("persistentVar1", "Hello World");
        persistentVars.put("persistentVar2", 987654321);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest", persistentVars);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("My Task");

        persistentVars.clear();
        Map<String, Object> transientVars = new HashMap<>();
        transientVars.put("unusedTransientVar", "Hello there");
        transientVars.put("transientVar", "OK");

        TaskCompletionBuilder taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder
                .taskId(task.getId())
                .transientVariables(transientVars)
                .variables(persistentVars)
                .complete();

        // Combined var has been set by execution listener
        String combinedVar = (String) runtimeService.getVariable(processInstance.getId(), "combinedVar");
        assertThat(combinedVar).isEqualTo("Hello WorldABC123");

        assertThat(runtimeService.getVariable(processInstance.getId(), "persistentVar1")).isNotNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "persistentVar2")).isNotNull();

        assertThat(runtimeService.getVariable(processInstance.getId(), "unusedTransientVar")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "transientVar")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "secondTransientVar")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "thirdTransientVar")).isNull();
    }

    @Test
    @Deployment
    public void testTaskListenerWithTransientVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task after");

        String mergedResult = (String) taskService.getVariable(task.getId(), "mergedResult");
        assertThat(mergedResult).isEqualTo("transientVar01transientVar02transientVar03");
    }

    @Test
    @Deployment
    public void testTransientVariableShadowsPersistentVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest", CollectionUtil.singletonMap("theVar", "Hello World"));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String varValue = (String) taskService.getVariable(task.getId(), "resultVar");
        assertThat(varValue).isEqualTo("I am shadowed");
    }

    @Test
    @Deployment
    public void testTriggerWithTransientVars() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");

        Execution executionInWait1 = runtimeService.createExecutionQuery().activityId("wait1").singleResult();
        runtimeService.trigger(executionInWait1.getId(), CollectionUtil.singletonMap("persistentVar", "persistentValue01"));

        Execution executionInWait2 = runtimeService.createExecutionQuery().activityId("wait2").singleResult();
        runtimeService.trigger(executionInWait2.getId(), CollectionUtil.singletonMap("anotherPersistentVar", "persistentValue02"),
                CollectionUtil.singletonMap("transientVar", "transientValue"));

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String result = (String) taskService.getVariable(task.getId(), "result");
        assertThat(result).isEqualTo("persistentValue02persistentValue01transientValue");

        assertThat(runtimeService.getVariable(processInstance.getId(), "transientVar")).isNull();
    }

    @Test
    @Deployment
    public void testStartProcessInstanceByKey() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("transientVarsTest")
                .transientVariable("variable", "gotoA")
                .start();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("transientVarsTest")
                .transientVariable("variable", "gotoB")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("transientVarsTest")
                .transientVariable("variable", "somethingElse")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Default");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();
    }

    @Test
    @Deployment
    public void testStartProcessInstanceById() {

        String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinitionId)
                .transientVariable("variable", "gotoA")
                .start();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinitionId)
                .transientVariable("variable", "gotoB")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinitionId)
                .transientVariable("variable", "somethingElse")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Default");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();
    }

    @Test
    @Deployment
    public void testStartProcessInstanceByMessage() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .messageName("myMessage")
                .transientVariable("variable", "gotoA")
                .start();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("A");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        processInstance = runtimeService.createProcessInstanceBuilder()
                .messageName("myMessage")
                .transientVariable("variable", "gotoB")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("B");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();

        processInstance = runtimeService.createProcessInstanceBuilder()
                .messageName("myMessage")
                .transientVariable("variable", "somethingElse")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Default");
        assertThat(runtimeService.getVariables(processInstance.getId())).isEmpty();
    }

    @Test
    @Deployment
    public void testLoopingExclusiveGateway() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("loopingTransientVarsTest");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task1");

        TaskCompletionBuilder taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder
                .taskId(task.getId())
                .transientVariable("status", 201)
                .complete();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task2");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task1");

        taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder
                .taskId(task.getId())
                .transientVariable("status", 200)
                .complete();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task3");

        taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder
                .taskId(task.getId())
                .transientVariable("status2", 200)
                .complete();
    }

    /* Service task class for previous tests */

    /**
     * Mimics a service task that fetches data from a server and stored the whole thing in a transient variable.
     */
    public static class FetchDataServiceTask implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setTransientVariable("response", "author=kermit;version=3;message=Hello World");
            execution.setTransientVariable("status", 200);
        }
    }

    /**
     * Processes the transient variable and puts the relevant bits in real variables
     */
    public static class ServiceTask02 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String response = (String) execution.getTransientVariable("response");
            for (String s : response.split(";")) {
                String[] data = s.split("=");
                if ("message".equals(data[0])) {
                    execution.setVariable("message", data[1] + "!");
                }
            }
        }
    }

    public static class CombineVariablesExecutionListener implements ExecutionListener {

        @Override
        public void notify(DelegateExecution execution) {
            String persistentVar1 = (String) execution.getVariable("persistentVar1");

            Object unusedTransientVar = execution.getVariable("unusedTransientVar");
            if (unusedTransientVar != null) {
                throw new RuntimeException("Unused transient var should have been deleted");
            }

            String secondTransientVar = (String) execution.getVariable("secondTransientVar");
            Number thirdTransientVar = (Number) execution.getTransientVariable("thirdTransientVar");

            String combinedVar = persistentVar1 + secondTransientVar + thirdTransientVar.intValue();
            execution.setVariable("combinedVar", combinedVar);
        }
    }

    public static class GetDataDelegate implements JavaDelegate {

        private Expression variableName;

        @Override
        public void execute(DelegateExecution execution) {
            String var = (String) variableName.getValue(execution);
            execution.setTransientVariable(var, "author=kermit;version=3;message=" + var);
        }
    }

    public static class ProcessDataDelegate implements JavaDelegate {

        private Expression dataVariableName;
        private Expression resultVariableName;

        @Override
        public void execute(DelegateExecution execution) {
            String varName = (String) dataVariableName.getValue(execution);
            String resultVar = (String) resultVariableName.getValue(execution);

            // Sets the name of the variable as 'resultVar'
            for (String s : ((String) execution.getVariable(varName)).split(";")) {
                String[] data = s.split("=");
                if ("message".equals(data[0])) {
                    execution.setTransientVariable(resultVar, data[1]);
                }
            }
        }
    }

    public static class MergeTransientVariablesTaskListener implements TaskListener {

        @Override
        public void notify(DelegateTask delegateTask) {
            Map<String, Object> transientVariables = delegateTask.getTransientVariables();
            List<String> variableNames = new ArrayList<>(transientVariables.keySet());
            Collections.sort(variableNames);

            StringBuilder strb = new StringBuilder();
            for (String variableName : variableNames) {
                if (variableName.startsWith("transientResult")) {
                    String result = (String) transientVariables.get(variableName);
                    strb.append(result);
                }
            }

            delegateTask.setVariable("mergedResult", strb.toString());
        }
    }

    public static class MergeVariableValues implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            Map<String, Object> vars = execution.getVariables();
            List<String> varNames = new ArrayList<>(vars.keySet());
            Collections.sort(varNames);

            StringBuilder strb = new StringBuilder();
            for (String varName : varNames) {
                strb.append(vars.get(varName));
            }

            execution.setVariable("result", strb.toString());
        }
    }

}

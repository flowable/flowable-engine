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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * @author Joram Barrez
 */
public class TransientVariablesTest extends PluggableFlowableTestCase {

    @Deployment
    public void testSetTransientVariableInServiceTask() {

        // Process has two service task: first sets transient vars,
        // second then processes transient var and puts data in real vars.
        // (mimicking a service + processing call)
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String message = (String) taskService.getVariable(task.getId(), "message");
        assertEquals("Hello World!", message);

        // Variable should not be there after user task
        assertNull(runtimeService.getVariable(processInstance.getId(), "response"));
    }

    @Deployment
    public void testUseTransientVariableInExclusiveGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("responseOk", task.getTaskDefinitionKey());

        // Variable should not be there after user task
        assertNull(runtimeService.getVariable(processInstance.getId(), "response"));
    }

    @Deployment
    public void testTaskCompleteWithTransientVariables() {
        Map<String, Object> persistentVars = new HashMap<>();
        persistentVars.put("persistentVar1", "Hello World");
        persistentVars.put("persistentVar2", 987654321);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest", persistentVars);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("My Task", task.getName());

        persistentVars.clear();
        Map<String, Object> transientVars = new HashMap<>();
        transientVars.put("unusedTransientVar", "Hello there");
        transientVars.put("transientVar", "OK");
        taskService.complete(task.getId(), persistentVars, transientVars);

        // Combined var has been set by execution listener
        String combinedVar = (String) runtimeService.getVariable(processInstance.getId(), "combinedVar");
        assertEquals("Hello WorldABC123", combinedVar);

        assertNotNull(runtimeService.getVariable(processInstance.getId(), "persistentVar1"));
        assertNotNull(runtimeService.getVariable(processInstance.getId(), "persistentVar2"));

        assertNull(runtimeService.getVariable(processInstance.getId(), "unusedTransientVar"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "transientVar"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "secondTransientVar"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "thirdTransientVar"));
    }

    @Deployment
    public void testTaskResolveWithTransientVariables() {
        Map<String, Object> persistentVars = new HashMap<>();
        persistentVars.put("persistentVar1", "Hello World");
        persistentVars.put("persistentVar2", 987654321);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest", persistentVars);

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("My Task", task.getName());

        persistentVars.clear();
        Map<String, Object> transientVars = new HashMap<>();
        transientVars.put("unusedTransientVar", "Hello there");
        transientVars.put("transientVar", "OK");
        taskService.complete(task.getId(), persistentVars, transientVars);

        // Combined var has been set by execution listener
        String combinedVar = (String) runtimeService.getVariable(processInstance.getId(), "combinedVar");
        assertEquals("Hello WorldABC123", combinedVar);

        assertNotNull(runtimeService.getVariable(processInstance.getId(), "persistentVar1"));
        assertNotNull(runtimeService.getVariable(processInstance.getId(), "persistentVar2"));

        assertNull(runtimeService.getVariable(processInstance.getId(), "unusedTransientVar"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "transientVar"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "secondTransientVar"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "thirdTransientVar"));
    }

    @Deployment
    public void testTaskListenerWithTransientVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("Task after", task.getName());

        String mergedResult = (String) taskService.getVariable(task.getId(), "mergedResult");
        assertEquals("transientVar01transientVar02transientVar03", mergedResult);
    }

    @Deployment
    public void testTransientVariableShadowsPersistentVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest", CollectionUtil.singletonMap("theVar", "Hello World"));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String varValue = (String) taskService.getVariable(task.getId(), "resultVar");
        assertEquals("I am shadowed", varValue);
    }

    @Deployment
    public void testTriggerWithTransientVars() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transientVarsTest");

        Execution executionInWait1 = runtimeService.createExecutionQuery().activityId("wait1").singleResult();
        runtimeService.trigger(executionInWait1.getId(), CollectionUtil.singletonMap("persistentVar", "persistentValue01"));

        Execution executionInWait2 = runtimeService.createExecutionQuery().activityId("wait2").singleResult();
        runtimeService.trigger(executionInWait2.getId(), CollectionUtil.singletonMap("anotherPersistentVar", "persistentValue02"), CollectionUtil.singletonMap("transientVar", "transientValue"));

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String result = (String) taskService.getVariable(task.getId(), "result");
        assertEquals("persistentValue02persistentValue01transientValue", result);

        assertNull(runtimeService.getVariable(processInstance.getId(), "transientVar"));
    }

    @Deployment
    public void testStartProcessInstanceByKey() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("transientVarsTest")
                .transientVariable("variable", "gotoA")
                .start();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("transientVarsTest")
                .transientVariable("variable", "gotoB")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("B", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("transientVarsTest")
                .transientVariable("variable", "somethingElse")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("Default", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());
    }

    @Deployment
    public void testStartProcessInstanceById() {

        String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinitionId)
                .transientVariable("variable", "gotoA")
                .start();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinitionId)
                .transientVariable("variable", "gotoB")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("B", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinitionId)
                .transientVariable("variable", "somethingElse")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("Default", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());
    }

    @Deployment
    public void testStartProcessInstanceByMessage() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .messageName("myMessage")
                .transientVariable("variable", "gotoA")
                .start();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("A", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());

        processInstance = runtimeService.createProcessInstanceBuilder()
                .messageName("myMessage")
                .transientVariable("variable", "gotoB")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("B", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());

        processInstance = runtimeService.createProcessInstanceBuilder()
                .messageName("myMessage")
                .transientVariable("variable", "somethingElse")
                .start();
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("Default", task.getName());
        assertEquals(0, runtimeService.getVariables(processInstance.getId()).size());
    }

    @Deployment
    public void testLoopingExclusiveGateway() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("loopingTransientVarsTest");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task1", task.getTaskDefinitionKey());
        Map<String, Object> transientVarMap = new HashMap<>();
        transientVarMap.put("status", 201);
        taskService.complete(task.getId(), new HashMap<>(), transientVarMap);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task2", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task1", task.getTaskDefinitionKey());
        transientVarMap.clear();
        transientVarMap.put("status", 200);
        taskService.complete(task.getId(), new HashMap<>(), transientVarMap);

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("task3", task.getTaskDefinitionKey());
        transientVarMap.clear();
        transientVarMap.put("status2", 200);
        taskService.complete(task.getId(), new HashMap<>(), transientVarMap);
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
                if (data[0].equals("message")) {
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
                if (data[0].equals("message")) {
                    execution.setTransientVariable(resultVar, data[1]);
                }
            }
        }
    }

    public static class MergeTransientVariablesTaskListener implements TaskListener {
        @Override
        public void notify(DelegateTask delegateTask) {
            Map<String, Object> transientVariables = delegateTask.getTransientVariables();
            List<String> variableNames = new ArrayList(transientVariables.keySet());
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

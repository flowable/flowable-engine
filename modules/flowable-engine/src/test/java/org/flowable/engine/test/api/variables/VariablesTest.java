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
import static org.assertj.core.api.Assertions.entry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing various constructs with variables. Created to test the changes done in https://jira.codehaus.org/browse/ACT-1900.
 *
 * @author Joram Barrez
 */
public class VariablesTest extends PluggableFlowableTestCase {

    protected String processInstanceId;

    @BeforeEach
    protected void setUp() throws Exception {

        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml").deploy();

        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();
    }

    private Map<String, Object> generateVariables() {
        Map<String, Object> vars = new HashMap<>();

        // 10 Strings
        for (int i = 0; i < 10; i++) {
            vars.put("stringVar" + i, "stringVarValue-" + i);
        }

        // 10 integers
        for (int i = 0; i < 10; i++) {
            vars.put("intVar" + i, i * 100);
        }

        // 10 dates
        for (int i = 0; i < 10; i++) {
            vars.put("dateVar" + i, new Date());
        }

        // 10 joda local dates
        for (int i = 0; i < 10; i++) {
            vars.put("localdateVar" + i, new LocalDate());
        }

        // 10 joda local dates
        for (int i = 0; i < 10; i++) {
            vars.put("datetimeVar" + i, new DateTime());
        }

        // 10 booleans
        for (int i = 0; i < 10; i++) {
            vars.put("booleanValue" + i, (i % 2 == 0));
        }

        // 10 Serializables
        for (int i = 0; i < 10; i++) {
            vars.put("serializableValue" + i, new TestSerializableVariable(i));
        }
        return vars;
    }

    @AfterEach
    protected void tearDown() throws Exception {

        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

    }

    @Test
    public void testGetVariables() {

        // Regular getVariables after process instance start
        Map<String, Object> vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(70);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfDateTimes = 0;
        int nrOfBooleans = 0;
        int nrOfSerializable = 0;
        for (String variableName : vars.keySet()) {
            Object variableValue = vars.get(variableName);
            if (variableValue instanceof String) {
                nrOfStrings++;
            } else if (variableValue instanceof Integer) {
                nrOfInts++;
            } else if (variableValue instanceof Boolean) {
                nrOfBooleans++;
            } else if (variableValue instanceof Date) {
                nrOfDates++;
            } else if (variableValue instanceof LocalDate) {
                nrOfLocalDates++;
            } else if (variableValue instanceof DateTime) {
                nrOfDateTimes++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfDateTimes).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);

        // Trying the same after moving the process
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskName("Task 3").singleResult();
        String executionId = task.getExecutionId();
        assertThat(processInstanceId).isNotEqualTo(executionId);

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(70);
        nrOfStrings = 0;
        nrOfInts = 0;
        nrOfDates = 0;
        nrOfLocalDates = 0;
        nrOfDateTimes = 0;
        nrOfBooleans = 0;
        nrOfSerializable = 0;
        for (String variableName : vars.keySet()) {
            Object variableValue = vars.get(variableName);
            if (variableValue instanceof String) {
                nrOfStrings++;
            } else if (variableValue instanceof Integer) {
                nrOfInts++;
            } else if (variableValue instanceof Boolean) {
                nrOfBooleans++;
            } else if (variableValue instanceof Date) {
                nrOfDates++;
            } else if (variableValue instanceof LocalDate) {
                nrOfLocalDates++;
            } else if (variableValue instanceof DateTime) {
                nrOfDateTimes++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfDateTimes).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);
    }

    @Test
    public void testGetVariablesLocal() {

        // Regular getVariables after process instance start
        Map<String, Object> vars = runtimeService.getVariablesLocal(processInstanceId);
        assertThat(vars).hasSize(70);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfDateTimes = 0;
        int nrOfBooleans = 0;
        int nrOfSerializable = 0;
        for (String variableName : vars.keySet()) {
            Object variableValue = vars.get(variableName);
            if (variableValue instanceof String) {
                nrOfStrings++;
            } else if (variableValue instanceof Integer) {
                nrOfInts++;
            } else if (variableValue instanceof Boolean) {
                nrOfBooleans++;
            } else if (variableValue instanceof Date) {
                nrOfDates++;
            } else if (variableValue instanceof LocalDate) {
                nrOfLocalDates++;
            } else if (variableValue instanceof DateTime) {
                nrOfDateTimes++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfDateTimes).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);

        // Trying the same after moving the process
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskName("Task 3").singleResult();
        String executionId = task.getExecutionId();
        assertThat(processInstanceId).isNotEqualTo(executionId);

        // On the local scope level, the vars shouldn't be visible
        vars = runtimeService.getVariablesLocal(executionId);
        assertThat(vars).isEmpty();
    }

    @Test
    public void testGetVariable() {

        // This actually does a specific select. Before, this was not the case
        // (all variables were fetched)
        // See the logging to verify this

        String value = (String) runtimeService.getVariable(processInstanceId, "stringVar3");
        assertThat(value).isEqualTo("stringVarValue-3");
    }

    @Test
    public void testGetVariablesLocal2() {

        // Trying the same after moving the process
        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskName("Task 3").singleResult();
        String executionId = task.getExecutionId();
        assertThat(processInstanceId).isNotEqualTo(executionId);

        runtimeService.setVariableLocal(executionId, "stringVar1", "hello");
        runtimeService.setVariableLocal(executionId, "stringVar2", "world");
        runtimeService.setVariableLocal(executionId, "myVar", "test123");

        Map<String, Object> vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(70);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfDateTimes = 0;
        int nrOfBooleans = 0;
        int nrOfSerializable = 0;
        for (String variableName : vars.keySet()) {
            Object variableValue = vars.get(variableName);
            if (variableValue instanceof String) {
                nrOfStrings++;
            } else if (variableValue instanceof Integer) {
                nrOfInts++;
            } else if (variableValue instanceof Boolean) {
                nrOfBooleans++;
            } else if (variableValue instanceof Date) {
                nrOfDates++;
            } else if (variableValue instanceof LocalDate) {
                nrOfLocalDates++;
            } else if (variableValue instanceof DateTime) {
                nrOfDateTimes++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfDateTimes).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);

        assertThat(vars)
                .contains(
                        entry("stringVar1", "stringVarValue-1"),
                        entry("stringVar2", "stringVarValue-2")
                )
                .doesNotContainKey("myVar");

        // Execution local

        vars = runtimeService.getVariables(executionId);

        nrOfStrings = 0;
        nrOfInts = 0;
        nrOfDates = 0;
        nrOfLocalDates = 0;
        nrOfDateTimes = 0;
        nrOfBooleans = 0;
        nrOfSerializable = 0;
        for (String variableName : vars.keySet()) {
            Object variableValue = vars.get(variableName);
            if (variableValue instanceof String) {
                nrOfStrings++;
            } else if (variableValue instanceof Integer) {
                nrOfInts++;
            } else if (variableValue instanceof Boolean) {
                nrOfBooleans++;
            } else if (variableValue instanceof Date) {
                nrOfDates++;
            } else if (variableValue instanceof LocalDate) {
                nrOfLocalDates++;
            } else if (variableValue instanceof DateTime) {
                nrOfDateTimes++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(11);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfDateTimes).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);

        assertThat(vars)
                .contains(
                        entry("stringVar1", "hello"),
                        entry("stringVar2", "world"),
                        entry("myVar", "test123")
                );
    }

    @Test
    public void testGetVariablesWithCollectionThroughRuntimeService() {

        Map<String, Object> vars = runtimeService.getVariables(processInstanceId, Arrays.asList("intVar1", "intVar3", "intVar5", "intVar9"));
        assertThat(vars)
                .containsOnly(
                        entry("intVar1", 100),
                        entry("intVar3", 300),
                        entry("intVar5", 500),
                        entry("intVar9", 900)
                );

        assertThat(runtimeService.getVariablesLocal(processInstanceId, Arrays.asList("intVar1", "intVar3", "intVar5", "intVar9"))).hasSize(4);

        // Trying the same after moving the process
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskName("Task 3").singleResult();
        String executionId = task.getExecutionId();
        assertThat(processInstanceId).isNotEqualTo(executionId);

        assertThat(runtimeService.getVariablesLocal(executionId, Arrays.asList("intVar1", "intVar3", "intVar5", "intVar9"))).isEmpty();
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testGetVariableAllVariableFetchingDefault() {

        // Testing it the default way, all using getVariable("someVar");

        Map<String, Object> vars = generateVariables();
        vars.put("testVar", "hello");
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesFetchingTestProcess", vars).getId();

        taskService.complete(taskService.createTaskQuery().taskName("Task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("Task B").singleResult().getId()); // Triggers service task invocation

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(71);

        String varValue = (String) runtimeService.getVariable(processInstanceId, "testVar");
        assertThat(varValue).isEqualTo("HELLO world");
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testGetVariableAllVariableFetchingDisabled() {

        Map<String, Object> vars = generateVariables();
        vars.put("testVar", "hello");
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesFetchingTestProcess", vars).getId();

        taskService.complete(taskService.createTaskQuery().taskName("Task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("Task B").singleResult().getId()); // Triggers service task invocation

        String varValue = (String) runtimeService.getVariable(processInstanceId, "testVar");
        assertThat(varValue).isEqualTo("HELLO world!");
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testGetVariableInDelegateMixed() {

        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesFetchingTestProcess", vars).getId();

        taskService.complete(taskService.createTaskQuery().taskName("Task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("Task B").singleResult().getId()); // Triggers service task invocation

        assertThat((String) runtimeService.getVariable(processInstanceId, "testVar")).isEqualTo("test 1 2 3");
        assertThat((String) runtimeService.getVariable(processInstanceId, "testVar2")).isEqualTo("Hiya");
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testGetVariableInDelegateMixed2() {

        Map<String, Object> vars = generateVariables();
        vars.put("testVar", "1");
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesFetchingTestProcess", vars).getId();

        taskService.complete(taskService.createTaskQuery().taskName("Task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("Task B").singleResult().getId()); // Triggers service task invocation

        assertThat((String) runtimeService.getVariable(processInstanceId, "testVar")).isEqualTo("1234");
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testGetVariableInDelegateMixed3() {

        Map<String, Object> vars = generateVariables();
        vars.put("testVar1", "one");
        vars.put("testVar2", "two");
        vars.put("testVar3", "three");
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesFetchingTestProcess", vars).getId();

        taskService.complete(taskService.createTaskQuery().taskName("Task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("Task B").singleResult().getId()); // Triggers service task invocation

        assertThat((String) runtimeService.getVariable(processInstanceId, "testVar1")).isEqualTo("one-CHANGED");
        assertThat((String) runtimeService.getVariable(processInstanceId, "testVar2")).isEqualTo("two-CHANGED");
        assertThat(runtimeService.getVariable(processInstanceId, "testVar3")).isNull();
    }

    @Test
    public void testTaskGetVariables() {

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("Task 1").singleResult();
        Map<String, Object> vars = taskService.getVariables(task.getId());
        assertThat(vars).hasSize(70);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfDateTimes = 0;
        int nrOfBooleans = 0;
        int nrOfSerializable = 0;
        for (String variableName : vars.keySet()) {
            Object variableValue = vars.get(variableName);
            if (variableValue instanceof String) {
                nrOfStrings++;
            } else if (variableValue instanceof Integer) {
                nrOfInts++;
            } else if (variableValue instanceof Boolean) {
                nrOfBooleans++;
            } else if (variableValue instanceof Date) {
                nrOfDates++;
            } else if (variableValue instanceof LocalDate) {
                nrOfLocalDates++;
            } else if (variableValue instanceof DateTime) {
                nrOfDateTimes++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfDateTimes).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);

        // Get variables local
        assertThat(taskService.getVariablesLocal(task.getId())).isEmpty();

        // Get collection of variables
        assertThat(taskService.getVariables(task.getId(), Arrays.asList("intVar2", "intVar5"))).hasSize(2);
        assertThat(taskService.getVariablesLocal(task.getId(), Arrays.asList("intVar2", "intVar5"))).isEmpty();

        // Get Variable
        assertThat(taskService.getVariable(task.getId(), "stringVar3")).isEqualTo("stringVarValue-3");
        assertThat(taskService.getVariable(task.getId(), "stringVarDoesNotExist")).isNull();
        assertThat(taskService.getVariableLocal(task.getId(), "stringVar3")).isNull();

        // Set local variable
        taskService.setVariableLocal(task.getId(), "localTaskVar", "localTaskVarValue");
        assertThat(taskService.getVariables(task.getId())).hasSize(71);
        assertThat(taskService.getVariablesLocal(task.getId())).hasSize(1);
        assertThat(taskService.getVariables(task.getId(), Arrays.asList("intVar2", "intVar5"))).hasSize(2);
        assertThat(taskService.getVariablesLocal(task.getId(), Arrays.asList("intVar2", "intVar5"))).isEmpty();
        assertThat(taskService.getVariable(task.getId(), "localTaskVar")).isEqualTo("localTaskVarValue");
        assertThat(taskService.getVariableLocal(task.getId(), "localTaskVar")).isEqualTo("localTaskVarValue");

        // Override process variable
        Collection<String> varNames = new ArrayList<>();
        varNames.add("stringVar1");
        assertThat(taskService.getVariable(task.getId(), "stringVar1")).isEqualTo("stringVarValue-1");
        assertThat(taskService.getVariables(task.getId(), varNames)).containsEntry("stringVar1", "stringVarValue-1");
        taskService.setVariableLocal(task.getId(), "stringVar1", "Override");
        assertThat(taskService.getVariables(task.getId())).hasSize(71);
        assertThat(taskService.getVariable(task.getId(), "stringVar1")).isEqualTo("Override");
        assertThat(taskService.getVariables(task.getId(), varNames)).containsEntry("stringVar1", "Override");
    }

    @Test
    public void testLocalDateVariable() {

        Calendar todayCal = new GregorianCalendar();
        int todayYear = todayCal.get(Calendar.YEAR);
        int todayMonth = todayCal.get(Calendar.MONTH);
        int todayDate = todayCal.get(Calendar.DAY_OF_MONTH);

        // Regular getVariables after process instance start
        LocalDate date1 = (LocalDate) runtimeService.getVariable(processInstanceId, "localdateVar1");
        assertThat(date1.getYear()).isEqualTo(todayYear);
        assertThat(date1.getMonthOfYear()).isEqualTo(todayMonth + 1);
        assertThat(date1.getDayOfMonth()).isEqualTo(todayDate);

        date1 = new LocalDate(2010, 11, 10);
        runtimeService.setVariable(processInstanceId, "localdateVar1", date1);
        date1 = (LocalDate) runtimeService.getVariable(processInstanceId, "localdateVar1");
        assertThat(date1.getYear()).isEqualTo(2010);
        assertThat(date1.getMonthOfYear()).isEqualTo(11);
        assertThat(date1.getDayOfMonth()).isEqualTo(10);

        LocalDate queryDate = new LocalDate(2010, 11, 9);
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("localdateVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);

        queryDate = new LocalDate(2010, 11, 10);
        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("localdateVar1", queryDate).singleResult();
        assertThat(processInstance).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("localdateVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
    }

    @Test
    public void testLocalDateTimeVariable() {

        Calendar todayCal = new GregorianCalendar();
        int todayYear = todayCal.get(Calendar.YEAR);
        int todayMonth = todayCal.get(Calendar.MONTH);
        int todayDate = todayCal.get(Calendar.DAY_OF_MONTH);

        // Regular getVariables after process instance start
        DateTime date1 = (DateTime) runtimeService.getVariable(processInstanceId, "datetimeVar1");
        assertThat(date1.getYear()).isEqualTo(todayYear);
        assertThat(date1.getMonthOfYear()).isEqualTo(todayMonth + 1);
        assertThat(date1.getDayOfMonth()).isEqualTo(todayDate);

        date1 = new DateTime(2010, 11, 10, 10, 15);
        runtimeService.setVariable(processInstanceId, "datetimeVar1", date1);
        date1 = (DateTime) runtimeService.getVariable(processInstanceId, "datetimeVar1");
        assertThat(date1.getYear()).isEqualTo(2010);
        assertThat(date1.getMonthOfYear()).isEqualTo(11);
        assertThat(date1.getDayOfMonth()).isEqualTo(10);
        assertThat(date1.getHourOfDay()).isEqualTo(10);
        assertThat(date1.getMinuteOfHour()).isEqualTo(15);

        DateTime queryDate = new DateTime(2010, 11, 10, 9, 15);
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("datetimeVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);

        queryDate = new DateTime(2010, 11, 10, 10, 15);
        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("datetimeVar1", queryDate).singleResult();
        assertThat(processInstance).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("datetimeVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
    }

    // Class to test variable serialization
    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;
        private int number;

        public TestSerializableVariable(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

    }

    // Test delegates
    public static class TestJavaDelegate1 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String var = (String) execution.getVariable("testVar");
            execution.setVariable("testVar", var.toUpperCase());
        }
    }

    public static class TestJavaDelegate2 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String var = (String) execution.getVariable("testVar");
            execution.setVariable("testVar", var + " world");
        }
    }

    public static class TestJavaDelegate3 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {

        }
    }

    // ////////////////////////////////////////

    public static class TestJavaDelegate4 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String var = (String) execution.getVariable("testVar", false);
            execution.setVariable("testVar", var.toUpperCase());
        }
    }

    public static class TestJavaDelegate5 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String var = (String) execution.getVariable("testVar", false);
            execution.setVariable("testVar", var + " world");
        }
    }

    public static class TestJavaDelegate6 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String var = (String) execution.getVariable("testVar", false);
            execution.setVariable("testVar", var + "!");
        }
    }

    // ////////////////////////////////////////

    public static class TestJavaDelegate7 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {

            // Setting variable through 'default' way of setting variable
            execution.setVariable("testVar", "test");

        }
    }

    public static class TestJavaDelegate8 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String var = (String) execution.getVariable("testVar", false);
            execution.setVariable("testVar", var + " 1 2 3");
        }
    }

    public static class TestJavaDelegate9 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("testVar2", "Hiya");
        }
    }

    // ////////////////////////////////////////

    public static class TestJavaDelegate10 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String testVar = (String) execution.getVariable("testVar", false);
            execution.setVariable("testVar", testVar + "2");
        }
    }

    public static class TestJavaDelegate11 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String testVar = (String) execution.getVariable("testVar", false);
            execution.setVariable("testVar", testVar + "3");
        }
    }

    public static class TestJavaDelegate12 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String testVar = (String) execution.getVariable("testVar");
            execution.setVariable("testVar", testVar + "4");
        }
    }

    // ////////////////////////////////////////

    public static class TestJavaDelegate13 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            Map<String, Object> vars = execution.getVariables(Arrays.asList("testVar1", "testVar2", "testVar3"), false);

            String testVar1 = (String) vars.get("testVar1");
            String testVar2 = (String) vars.get("testVar2");
            String testVar3 = (String) vars.get("testVar3");

            execution.setVariable("testVar1", testVar1 + "-CHANGED", false);
            execution.setVariable("testVar2", testVar2 + "-CHANGED", false);
            execution.setVariable("testVar3", testVar3 + "-CHANGED", false);

            execution.setVariableLocal("localVar", "localValue", false);
        }
    }

    public static class TestJavaDelegate14 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            String value = (String) execution.getVariable("testVar2");
            String localVarValue = (String) execution.getVariableLocal("localValue");
            execution.setVariableLocal("testVar2", value + localVarValue);
        }
    }

    public static class TestJavaDelegate15 implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.removeVariable("testVar3");
        }
    }

}

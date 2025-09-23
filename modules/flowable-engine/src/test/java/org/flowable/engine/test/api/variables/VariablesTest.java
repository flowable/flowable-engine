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
import static org.assertj.core.api.Assertions.within;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.assertj.core.groups.Tuple;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.JodaDateTimeFallbackType;
import org.flowable.variable.service.impl.types.JodaDateFallbackType;
import org.junit.jupiter.api.Test;

/**
 * Testing various constructs with variables.
 *
 * @author Joram Barrez
 */
public class VariablesTest extends PluggableFlowableTestCase {

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

        // 10 local dates
        for (int i = 0; i < 10; i++) {
            vars.put("localdateVar" + i, LocalDate.now());
        }
        
        // 10 big decimals
        for (int i = 0; i < 10; i++) {
            vars.put("bigDecimalVar" + i, new BigDecimal(24.5 + i));
        }
        
        // 10 big integers
        for (int i = 0; i < 10; i++) {
            vars.put("bigIntegerVar" + i, new BigInteger("" + (24 + i)));
        }

        // 10 instants
        for (int i = 0; i < 10; i++) {
            vars.put("instantVar" + i, Instant.now());
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

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testGetVariables() {

        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        // Regular getVariables after process instance start
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(90);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfInstants = 0;
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
            } else if (variableValue instanceof Instant) {
                nrOfInstants++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfInstants).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);

        // Trying the same after moving the process
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskName("Task 3").singleResult();
        String executionId = task.getExecutionId();
        assertThat(processInstanceId).isNotEqualTo(executionId);

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(90);
        nrOfStrings = 0;
        nrOfInts = 0;
        nrOfDates = 0;
        nrOfLocalDates = 0;
        nrOfInstants = 0;
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
            } else if (variableValue instanceof Instant) {
                nrOfInstants++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfInstants).isEqualTo(10);
        assertThat(nrOfInts).isEqualTo(10);
        assertThat(nrOfSerializable).isEqualTo(10);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testGetVariablesLocal() {
        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        // Regular getVariables after process instance start
        vars = runtimeService.getVariablesLocal(processInstanceId);
        assertThat(vars).hasSize(90);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfInstants = 0;
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
            } else if (variableValue instanceof Instant) {
                nrOfInstants++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfInstants).isEqualTo(10);
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
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testGetVariable() {
        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        // This actually does a specific select. Before, this was not the case
        // (all variables were fetched)
        // See the logging to verify this

        String value = (String) runtimeService.getVariable(processInstanceId, "stringVar3");
        assertThat(value).isEqualTo("stringVarValue-3");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testGetVariablesLocal2() {
        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        // Trying the same after moving the process
        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskName("Task 3").singleResult();
        String executionId = task.getExecutionId();
        assertThat(processInstanceId).isNotEqualTo(executionId);

        runtimeService.setVariableLocal(executionId, "stringVar1", "hello");
        runtimeService.setVariableLocal(executionId, "stringVar2", "world");
        runtimeService.setVariableLocal(executionId, "myVar", "test123");

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(90);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfBigDecimals = 0;
        int nrOfBigIntegers = 0;
        int nrOfInstants = 0;
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
            } else if (variableValue instanceof BigDecimal) {
                nrOfBigDecimals++;
            } else if (variableValue instanceof BigInteger) {
                nrOfBigIntegers++;
            } else if (variableValue instanceof Instant) {
                nrOfInstants++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfBigDecimals).isEqualTo(10);
        assertThat(nrOfBigIntegers).isEqualTo(10);
        assertThat(nrOfInstants).isEqualTo(10);
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
        nrOfBigDecimals = 0;
        nrOfBigIntegers = 0;
        nrOfInstants = 0;
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
            } else if (variableValue instanceof BigDecimal) {
                nrOfBigDecimals++;
            } else if (variableValue instanceof BigInteger) {
                nrOfBigIntegers++;
            } else if (variableValue instanceof Instant) {
                nrOfInstants++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(11);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfBigDecimals).isEqualTo(10);
        assertThat(nrOfBigIntegers).isEqualTo(10);
        assertThat(nrOfInstants).isEqualTo(10);
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
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testGetVariablesWithCollectionThroughRuntimeService() {
        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        vars = runtimeService.getVariables(processInstanceId, Arrays.asList("intVar1", "intVar3", "intVar5", "intVar9"));
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
        assertThat(vars).hasSize(91);

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
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testTaskGetVariables() {
        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskName("Task 1").singleResult();
        vars = taskService.getVariables(task.getId());
        assertThat(vars).hasSize(90);
        int nrOfStrings = 0;
        int nrOfInts = 0;
        int nrOfDates = 0;
        int nrOfLocalDates = 0;
        int nrOfInstants = 0;
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
            } else if (variableValue instanceof Instant) {
                nrOfInstants++;
            } else if (variableValue instanceof TestSerializableVariable) {
                nrOfSerializable++;
            }
        }

        assertThat(nrOfStrings).isEqualTo(10);
        assertThat(nrOfBooleans).isEqualTo(10);
        assertThat(nrOfDates).isEqualTo(10);
        assertThat(nrOfLocalDates).isEqualTo(10);
        assertThat(nrOfInstants).isEqualTo(10);
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
        assertThat(taskService.getVariables(task.getId())).hasSize(91);
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
        assertThat(taskService.getVariables(task.getId())).hasSize(91);
        assertThat(taskService.getVariable(task.getId(), "stringVar1")).isEqualTo("Override");
        assertThat(taskService.getVariables(task.getId(), varNames)).containsEntry("stringVar1", "Override");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testLocalDateVariable() {
        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        Calendar todayCal = new GregorianCalendar();
        int todayYear = todayCal.get(Calendar.YEAR);
        int todayMonth = todayCal.get(Calendar.MONTH);
        int todayDate = todayCal.get(Calendar.DAY_OF_MONTH);

        // Regular getVariables after process instance start
        LocalDate date1 = (LocalDate) runtimeService.getVariable(processInstanceId, "localdateVar1");
        assertThat(date1.getYear()).isEqualTo(todayYear);
        assertThat(date1.getMonthValue()).isEqualTo(todayMonth + 1);
        assertThat(date1.getDayOfMonth()).isEqualTo(todayDate);

        date1 = LocalDate.of(2010, Month.NOVEMBER, 10);
        runtimeService.setVariable(processInstanceId, "localdateVar1", date1);
        date1 = (LocalDate) runtimeService.getVariable(processInstanceId, "localdateVar1");
        assertThat(date1.getYear()).isEqualTo(2010);
        assertThat(date1.getMonthValue()).isEqualTo(11);
        assertThat(date1.getDayOfMonth()).isEqualTo(10);

        LocalDate queryDate = LocalDate.of(2010, Month.NOVEMBER, 9);
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("localdateVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);

        queryDate = LocalDate.of(2010, Month.NOVEMBER, 10);
        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("localdateVar1", queryDate).singleResult();
        assertThat(processInstance).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("localdateVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testBigDecimalVariable() {
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        BigDecimal decimal1 = (BigDecimal) runtimeService.getVariable(processInstanceId, "bigDecimalVar1");
        assertThat(decimal1).isEqualTo(new BigDecimal(25.5));

        decimal1 = new BigDecimal(34.1);
        runtimeService.setVariable(processInstanceId, "bigDecimalVar1", decimal1);
        decimal1 = (BigDecimal) runtimeService.getVariable(processInstanceId, "bigDecimalVar1");
        assertThat(decimal1).isEqualTo(new BigDecimal(34.1));
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testBigIntegerVariable() {
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        BigInteger integerVar = (BigInteger) runtimeService.getVariable(processInstanceId, "bigIntegerVar1");
        assertThat(integerVar).isEqualTo(new BigInteger("25"));

        integerVar = new BigInteger("34");
        runtimeService.setVariable(processInstanceId, "bigIntegerVar1", integerVar);
        integerVar = (BigInteger) runtimeService.getVariable(processInstanceId, "bigIntegerVar1");
        assertThat(integerVar).isEqualTo(new BigInteger("34"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testInstantVariable() {
        // Creating 50 vars in total
        Map<String, Object> vars = generateVariables();
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        // Regular getVariables after process instance start
        Instant date1 = (Instant) runtimeService.getVariable(processInstanceId, "instantVar1");
        assertThat(date1.atZone(ZoneId.systemDefault()))
                .isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.HOURS));

        Instant updatedDate = LocalDateTime.of(2010, Month.NOVEMBER, 10, 10, 15).atZone(ZoneId.systemDefault()).toInstant();
        runtimeService.setVariable(processInstanceId, "instantVar1", updatedDate);
        date1 = (Instant) runtimeService.getVariable(processInstanceId, "instantVar1");
        assertThat(date1).isEqualTo(updatedDate);

        Instant queryDate = LocalDateTime.of(2010, Month.NOVEMBER, 10, 9, 15).atZone(ZoneId.systemDefault()).toInstant();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("instantVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);

        queryDate = LocalDateTime.of(2010, Month.NOVEMBER, 10, 10, 15).atZone(ZoneId.systemDefault()).toInstant();
        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThan("instantVar1", queryDate).singleResult();
        assertThat(processInstance).isNull();

        processInstance = runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("instantVar1", queryDate).singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getId()).isEqualTo(processInstanceId);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testUpdateMetaInfo() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVariable", "Hello World");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("variablesTest", variables);

        VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(variableInstance.getMetaInfo()).isNull();

        managementService.executeCommand(commandContext -> {
            List<VariableInstanceEntity> variablesInstances = CommandContextUtil.getVariableService(commandContext)
                    .findVariableInstancesByExecutionId(processInstance.getId());
            assertThat(variablesInstances).extracting(ValueFields::getName).containsExactly("myVariable");

            VariableInstanceEntity variableInstanceEntity = variablesInstances.get(0);
            variableInstanceEntity.setMetaInfo("test meta info");
            VariableServiceConfiguration variableServiceConfiguration = processEngineConfiguration.getVariableServiceConfiguration();
            variableServiceConfiguration.getVariableInstanceEntityManager().update(variableInstanceEntity);
            if (variableServiceConfiguration.getInternalHistoryVariableManager() != null) {
                variableServiceConfiguration.getInternalHistoryVariableManager()
                        .recordVariableUpdate(variableInstanceEntity, commandContext.getClock().getCurrentTime());
            }
            return null;
        });

        variableInstance = runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(variableInstance.getMetaInfo()).isEqualTo("test meta info");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicVariableInstance.getMetaInfo()).isEqualTo("test meta info");
        }
    }

    @Test
    public void testCreateAndUpdateWithValue() {
        List<Object> toDelete = new LinkedList<>();
        try {
            managementService.executeCommand(commandContext -> {

                VariableServiceConfiguration variableServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                        .getVariableServiceConfiguration();
                VariableService variableService = variableServiceConfiguration.getVariableService();
                VariableInstanceEntity variableInstanceEntity = variableService
                        .createVariableInstance("myVariable");
                variableInstanceEntity.setScopeId("testScopeId");
                variableInstanceEntity.setScopeType("testScopeType");
                variableService.insertVariableInstanceWithValue(variableInstanceEntity, "myStringValue", "myTenantId");
                if (variableServiceConfiguration.getInternalHistoryVariableManager() != null) {
                    variableServiceConfiguration.getInternalHistoryVariableManager()
                            .recordVariableCreate(variableInstanceEntity, commandContext.getClock().getCurrentTime());
                }
                return null;
            });

            managementService.executeCommand(commandContext -> {
                List<VariableInstanceEntity> variablesInstances = CommandContextUtil.getVariableService(commandContext)
                        .findVariableInstanceByScopeIdAndScopeType("testScopeId", "testScopeType");
                assertThat(variablesInstances).extracting(ValueFields::getName, ValueFields::getTextValue, VariableInstanceEntity::getTypeName)
                        .containsExactly(Tuple.tuple("myVariable", "myStringValue", "string"));

                VariableInstanceEntity variableInstanceEntity = variablesInstances.get(0);
                VariableServiceConfiguration variableServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                        .getVariableServiceConfiguration();
                variableServiceConfiguration.getVariableInstanceValueModifier().updateVariableValue(variableInstanceEntity, 42, "myTenantId");
                variableServiceConfiguration.getVariableInstanceEntityManager().update(variableInstanceEntity);
                if (variableServiceConfiguration.getInternalHistoryVariableManager() != null) {
                    variableServiceConfiguration.getInternalHistoryVariableManager()
                            .recordVariableUpdate(variableInstanceEntity, commandContext.getClock().getCurrentTime());
                }

                return null;
            });

            VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().variableName("myVariable").singleResult();
            assertThat(variableInstance.getValue()).isEqualTo(42);
            assertThat(variableInstance.getTypeName()).isEqualTo("integer");
            toDelete.add(variableInstance);
            HistoricVariableInstance historicVariableInstance;
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                        .variableName("myVariable").singleResult();
                assertThat(historicVariableInstance.getValue()).isEqualTo(42);
                toDelete.add(historicVariableInstance);
            }
        } finally {
            managementService.executeCommand(commandContext -> {
                toDelete.forEach(var -> {
                    if (var instanceof VariableInstanceEntity) {
                        CommandContextUtil.getVariableService(commandContext).deleteVariableInstance((VariableInstanceEntity) var);
                    }
                    if (var instanceof HistoricVariableInstance) {
                        CommandContextUtil.getHistoricVariableService(commandContext).deleteHistoricVariableInstance((HistoricVariableInstanceEntity) var);
                    }
                });
                return null;
            });
        }
    }

    @Test
    @Deployment
    public void testSettingGettingMultipleTimesInSameTransaction() {

        TestSetGetVariablesDelegate.REMOVE_VARS_IN_LAST_ROUND = true;

        ProcessInstance processInstance1 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testSettingGettingMultipleTimesInSameTransaction")
                .start();
        assertThat(runtimeService.getVariables(processInstance1.getId())).isEmpty();

        TestSetGetVariablesDelegate.REMOVE_VARS_IN_LAST_ROUND = false;
        ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testSettingGettingMultipleTimesInSameTransaction")
                .start();
        Map<String, Object> variables = runtimeService.getVariables(processInstance2.getId());
        assertThat(variables).hasSize(100);
        for (String variableName : variables.keySet()) {
            assertThat(variables.get(variableName)).isNotNull();
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testJodaFallbackVariables() {

        // Creating long values since the JodaDateType and JodaDateTimeType were storing the values as long.
        // We are manually going to change the type name in the variable instance after they are created to mimic going from having joda to not having it
        Map<String, Object> vars = Map.of(
                "jodaDateTimeVar", Instant.parse("2025-09-18T09:40:56Z").toEpochMilli(),
                "jodaDateVar", LocalDate.parse("2025-08-25").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        );
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        // We have to manually store this to achieve the functionality of having something stored in the joda date / joda date time respectively
        managementService.executeCommand(commandContext -> {
            List<VariableInstanceEntity> variableInstances = CommandContextUtil.getVariableService(commandContext)
                    .findVariableInstancesByExecutionId(processInstanceId);

            VariableServiceConfiguration variableServiceConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                    .getVariableServiceConfiguration();
            VariableTypes variableTypes = variableServiceConfiguration
                    .getVariableTypes();
            HistoricVariableInstanceEntityManager historicVariableInstanceEntityManager = variableServiceConfiguration.getHistoricVariableInstanceEntityManager();

            VariableType jodaDateTimeType = variableTypes.getVariableType(JodaDateTimeFallbackType.TYPE_NAME);
            VariableType jodaDateType = variableTypes.getVariableType(JodaDateFallbackType.TYPE_NAME);

            for (VariableInstanceEntity variableInstanceEntity : variableInstances) {
                VariableType variableType;
                if ("jodaDateTimeVar".equals(variableInstanceEntity.getName())) {
                    variableType = jodaDateTimeType;
                } else if ("jodaDateVar".equals(variableInstanceEntity.getName())) {
                    variableType = jodaDateType;
                } else {
                    variableType = null;
                }
                if (variableType != null) {
                    variableInstanceEntity.setType(variableType);
                    variableInstanceEntity.setTypeName(variableType.getTypeName());
                    HistoricVariableInstanceEntity historicVariableInstance = historicVariableInstanceEntityManager.findById(variableInstanceEntity.getId());
                    if (historicVariableInstance != null) {
                        historicVariableInstance.setVariableType(variableType);
                    }
                }
            }

            return null;
        });

        assertThat(runtimeService.getVariables(processInstanceId))
                .containsOnly(
                        entry("jodaDateTimeVar", Instant.parse("2025-09-18T09:40:56Z")),
                        entry("jodaDateVar", LocalDate.parse("2025-08-25"))
                );
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

    public static class TestSetGetVariablesDelegate implements JavaDelegate {

        public static boolean REMOVE_VARS_IN_LAST_ROUND = true;

        @Override
        public void execute(DelegateExecution execution) {
            String processInstanceId = execution.getProcessInstanceId();
            RuntimeService runtimeService = CommandContextUtil.getProcessEngineConfiguration().getRuntimeService();

            int nrOfLoops = 100;
            for (int nrOfRounds = 0; nrOfRounds < 4; nrOfRounds++) {

                // Set
                for (int i = 0; i < nrOfLoops; i++) {
                    runtimeService.setVariable(processInstanceId, "test_" + i, i);
                }

                // Get
                for (int i = 0; i < nrOfLoops; i++) {
                    if (runtimeService.getVariable(processInstanceId, "test_" + i) == null) {
                        throw new RuntimeException("This exception shouldn't happen");
                    }
                }

                // Remove
                if (REMOVE_VARS_IN_LAST_ROUND && nrOfRounds == 3) {
                    for (int i = 0; i < nrOfLoops; i++) {
                        runtimeService.removeVariable(processInstanceId, "test_" + i);
                    }
                }

            }
        }
    }

}

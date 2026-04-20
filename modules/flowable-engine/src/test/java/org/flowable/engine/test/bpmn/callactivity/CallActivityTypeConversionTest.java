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
package org.flowable.engine.test.bpmn.callactivity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests for type conversion of in/out parameters on call activities.
 * In parameters support a targetType attribute, out parameters support a sourceType attribute.
 *
 * @author Tijs Rademakers
 */
public class CallActivityTypeConversionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityTypeConversionTest.testInParameterTypeConversion.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityTypeConversionTest.childProcess.bpmn20.xml"
    })
    public void testInParameterTypeConversion() {
        Instant testInstant = Instant.parse("2025-06-15T10:30:00Z");
        LocalDate testLocalDate = LocalDate.of(2025, 6, 15);
        Date testDate = Date.from(testInstant);

        Map<String, Object> variables = new HashMap<>();
        // Source variables for string conversion
        variables.put("intVar", 42);
        variables.put("doubleVar", 3.14);
        variables.put("booleanVar", true);

        // Source variables for integer conversion
        variables.put("stringIntVar", "123");
        variables.put("longVar", 456L);

        // Source variables for long conversion
        variables.put("stringLongVar", "789012345678");

        // Source variables for double conversion
        variables.put("stringDoubleVar", "2.718");

        // Source variables for boolean conversion
        variables.put("stringBooleanVar", "true");

        // Source variables for date conversion
        variables.put("stringDateVar", "2025-06-15T10:30:00Z");
        variables.put("localDateVar", testLocalDate);
        variables.put("instantVar", testInstant);

        // Source variables for localdate conversion
        variables.put("stringLocalDateVar", "2025-06-15");
        variables.put("dateVar", testDate);

        // Source variables for duration-to-date conversion
        variables.put("periodVar", "P10D");
        variables.put("durationVar", "PT10H");

        // Source variables for JSON conversion
        variables.put("jsonStringVar", "{\"name\":\"flowable\",\"version\":7}");

        // Source variables for array conversion
        variables.put("arrayStringVar", "[1,2,3]");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parentProcess", variables);

        // Get the child process instance
        ProcessInstance childProcessInstance = runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(processInstance.getId())
                .singleResult();
        assertThat(childProcessInstance).isNotNull();

        // Verify string conversions
        assertThat(runtimeService.getVariable(childProcessInstance.getId(), "intToString")).isEqualTo("42");
        assertThat(runtimeService.getVariable(childProcessInstance.getId(), "doubleToString")).isEqualTo("3.14");
        assertThat(runtimeService.getVariable(childProcessInstance.getId(), "booleanToString")).isEqualTo("true");

        // Verify integer conversions
        Object stringToInt = runtimeService.getVariable(childProcessInstance.getId(), "stringToInt");
        assertThat(stringToInt).isInstanceOf(Integer.class).isEqualTo(123);

        Object longToInt = runtimeService.getVariable(childProcessInstance.getId(), "longToInt");
        assertThat(longToInt).isInstanceOf(Integer.class).isEqualTo(456);

        Object doubleToInt = runtimeService.getVariable(childProcessInstance.getId(), "doubleToInt");
        assertThat(doubleToInt).isInstanceOf(Integer.class).isEqualTo(3);

        // Verify long conversions
        Object stringToLong = runtimeService.getVariable(childProcessInstance.getId(), "stringToLong");
        assertThat(stringToLong).isInstanceOf(Long.class).isEqualTo(789012345678L);

        Object intToLong = runtimeService.getVariable(childProcessInstance.getId(), "intToLong");
        assertThat(intToLong).isInstanceOf(Long.class).isEqualTo(42L);

        // Verify double conversions
        Object stringToDouble = runtimeService.getVariable(childProcessInstance.getId(), "stringToDouble");
        assertThat(stringToDouble).isInstanceOf(Double.class).isEqualTo(2.718);

        Object intToDouble = runtimeService.getVariable(childProcessInstance.getId(), "intToDouble");
        assertThat(intToDouble).isInstanceOf(Double.class).isEqualTo(42.0);

        Object longToDouble = runtimeService.getVariable(childProcessInstance.getId(), "longToDouble");
        assertThat(longToDouble).isInstanceOf(Double.class).isEqualTo(456.0);

        // Verify boolean conversion
        Object stringToBoolean = runtimeService.getVariable(childProcessInstance.getId(), "stringToBoolean");
        assertThat(stringToBoolean).isInstanceOf(Boolean.class).isEqualTo(true);

        // Verify date conversions
        Object stringToDate = runtimeService.getVariable(childProcessInstance.getId(), "stringToDate");
        assertThat(stringToDate).isInstanceOf(Date.class);
        assertThat(((Date) stringToDate).toInstant()).isEqualTo(testInstant);

        Object localDateToDate = runtimeService.getVariable(childProcessInstance.getId(), "localDateToDate");
        assertThat(localDateToDate).isInstanceOf(Date.class);
        assertThat(((Date) localDateToDate).toInstant()).isEqualTo(testLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Object instantToDate = runtimeService.getVariable(childProcessInstance.getId(), "instantToDate");
        assertThat(instantToDate).isInstanceOf(Date.class);
        assertThat(((Date) instantToDate).toInstant()).isEqualTo(testInstant);

        // Verify period/duration to date conversions
        Date now = new Date();
        Object periodToDate = runtimeService.getVariable(childProcessInstance.getId(), "periodToDate");
        assertThat(periodToDate).isInstanceOf(Date.class);
        assertThat(Duration.between(now.toInstant(), ((Date) periodToDate).toInstant()))
                .isBetween(Duration.ofDays(9).plusHours(23), Duration.ofDays(10).plusMinutes(1));

        Object durationToDate = runtimeService.getVariable(childProcessInstance.getId(), "durationToDate");
        assertThat(durationToDate).isInstanceOf(Date.class);
        assertThat(Duration.between(now.toInstant(), ((Date) durationToDate).toInstant()))
                .isBetween(Duration.ofHours(9).plusMinutes(59), Duration.ofHours(10).plusMinutes(1));

        // Verify localdate conversions
        Object stringToLocalDate = runtimeService.getVariable(childProcessInstance.getId(), "stringToLocalDate");
        assertThat(stringToLocalDate).isInstanceOf(LocalDate.class).isEqualTo(testLocalDate);

        Object dateToLocalDate = runtimeService.getVariable(childProcessInstance.getId(), "dateToLocalDate");
        assertThat(dateToLocalDate).isInstanceOf(LocalDate.class).isEqualTo(testLocalDate);

        Object instantToLocalDate = runtimeService.getVariable(childProcessInstance.getId(), "instantToLocalDate");
        assertThat(instantToLocalDate).isInstanceOf(LocalDate.class).isEqualTo(testLocalDate);

        // Verify period/duration to localdate conversions
        LocalDate today = LocalDate.now();
        Object periodToLocalDate = runtimeService.getVariable(childProcessInstance.getId(), "periodToLocalDate");
        assertThat(periodToLocalDate).isInstanceOf(LocalDate.class).isEqualTo(today.plusDays(10));

        Object durationToLocalDate = runtimeService.getVariable(childProcessInstance.getId(), "durationToLocalDate");
        assertThat(durationToLocalDate).isInstanceOf(LocalDate.class);
        // PT10H from now could be today or tomorrow depending on time of day
        assertThat((LocalDate) durationToLocalDate).isBetween(today, today.plusDays(1));

        // Verify JSON conversion
        Object stringToJson = runtimeService.getVariable(childProcessInstance.getId(), "stringToJson");
        assertThat(stringToJson).isInstanceOf(ObjectNode.class);
        ObjectNode jsonNode = (ObjectNode) stringToJson;
        assertThat(jsonNode.path("name").asString()).isEqualTo("flowable");
        assertThat(jsonNode.path("version").asInt()).isEqualTo(7);

        // Verify array conversion
        Object stringToArray = runtimeService.getVariable(childProcessInstance.getId(), "stringToArray");
        assertThat(stringToArray).isInstanceOf(ArrayNode.class);
        ArrayNode arrayNode = (ArrayNode) stringToArray;
        assertThat(arrayNode).hasSize(3);
        assertThat(arrayNode.get(0).asInt()).isEqualTo(1);
        assertThat(arrayNode.get(1).asInt()).isEqualTo(2);
        assertThat(arrayNode.get(2).asInt()).isEqualTo(3);
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivityTypeConversionTest.testOutParameterTypeConversion.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivityTypeConversionTest.childProcess.bpmn20.xml"
    })
    public void testOutParameterTypeConversion() {
        Instant testInstant = Instant.parse("2025-06-15T10:30:00Z");
        LocalDate testLocalDate = LocalDate.of(2025, 6, 15);
        Date testDate = Date.from(testInstant);

        Map<String, Object> variables = new HashMap<>();
        variables.put("dummy", "dummy");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parentProcessOut", variables);

        // Get the child process instance and set variables on it
        ProcessInstance childProcessInstance = runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(processInstance.getId())
                .singleResult();
        assertThat(childProcessInstance).isNotNull();

        // Set variables on the child process that will be mapped back with type conversion
        runtimeService.setVariable(childProcessInstance.getId(), "intVar", 42);
        runtimeService.setVariable(childProcessInstance.getId(), "doubleVar", 3.14);
        runtimeService.setVariable(childProcessInstance.getId(), "stringIntVar", "123");
        runtimeService.setVariable(childProcessInstance.getId(), "longVar", 456L);
        runtimeService.setVariable(childProcessInstance.getId(), "stringLongVar", "789012345678");
        runtimeService.setVariable(childProcessInstance.getId(), "stringDoubleVar", "2.718");
        runtimeService.setVariable(childProcessInstance.getId(), "stringBooleanVar", "true");
        runtimeService.setVariable(childProcessInstance.getId(), "stringDateVar", "2025-06-15T10:30:00Z");
        runtimeService.setVariable(childProcessInstance.getId(), "localDateVar", testLocalDate);
        runtimeService.setVariable(childProcessInstance.getId(), "stringLocalDateVar", "2025-06-15");
        runtimeService.setVariable(childProcessInstance.getId(), "dateVar", testDate);
        runtimeService.setVariable(childProcessInstance.getId(), "jsonStringVar", "{\"name\":\"flowable\"}");
        runtimeService.setVariable(childProcessInstance.getId(), "arrayStringVar", "[4,5,6]");

        // Complete the child task to trigger out parameter mapping
        Task childTask = taskService.createTaskQuery().processInstanceId(childProcessInstance.getId()).singleResult();
        assertThat(childTask).isNotNull();
        taskService.complete(childTask.getId());

        // Verify string conversions
        assertThat(runtimeService.getVariable(processInstance.getId(), "intToString")).isEqualTo("42");
        assertThat(runtimeService.getVariable(processInstance.getId(), "doubleToString")).isEqualTo("3.14");

        // Verify integer conversions
        Object stringToInt = runtimeService.getVariable(processInstance.getId(), "stringToInt");
        assertThat(stringToInt).isInstanceOf(Integer.class).isEqualTo(123);

        Object longToInt = runtimeService.getVariable(processInstance.getId(), "longToInt");
        assertThat(longToInt).isInstanceOf(Integer.class).isEqualTo(456);

        // Verify long conversions
        Object stringToLong = runtimeService.getVariable(processInstance.getId(), "stringToLong");
        assertThat(stringToLong).isInstanceOf(Long.class).isEqualTo(789012345678L);

        Object intToLong = runtimeService.getVariable(processInstance.getId(), "intToLong");
        assertThat(intToLong).isInstanceOf(Long.class).isEqualTo(42L);

        // Verify double conversion
        Object stringToDouble = runtimeService.getVariable(processInstance.getId(), "stringToDouble");
        assertThat(stringToDouble).isInstanceOf(Double.class).isEqualTo(2.718);

        Object intToDouble = runtimeService.getVariable(processInstance.getId(), "intToDouble");
        assertThat(intToDouble).isInstanceOf(Double.class).isEqualTo(42.0);

        // Verify boolean conversion
        Object stringToBoolean = runtimeService.getVariable(processInstance.getId(), "stringToBoolean");
        assertThat(stringToBoolean).isInstanceOf(Boolean.class).isEqualTo(true);

        // Verify date conversions
        Object stringToDate = runtimeService.getVariable(processInstance.getId(), "stringToDate");
        assertThat(stringToDate).isInstanceOf(Date.class);
        assertThat(((Date) stringToDate).toInstant()).isEqualTo(testInstant);

        Object localDateToDate = runtimeService.getVariable(processInstance.getId(), "localDateToDate");
        assertThat(localDateToDate).isInstanceOf(Date.class);
        assertThat(((Date) localDateToDate).toInstant()).isEqualTo(testLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Verify localdate conversions
        Object stringToLocalDate = runtimeService.getVariable(processInstance.getId(), "stringToLocalDate");
        assertThat(stringToLocalDate).isInstanceOf(LocalDate.class).isEqualTo(testLocalDate);

        Object dateToLocalDate = runtimeService.getVariable(processInstance.getId(), "dateToLocalDate");
        assertThat(dateToLocalDate).isInstanceOf(LocalDate.class).isEqualTo(testLocalDate);

        // Verify JSON conversion
        Object stringToJson = runtimeService.getVariable(processInstance.getId(), "stringToJson");
        assertThat(stringToJson).isInstanceOf(ObjectNode.class);
        ObjectNode jsonNode = (ObjectNode) stringToJson;
        assertThat(jsonNode.path("name").asString()).isEqualTo("flowable");

        // Verify array conversion
        Object stringToArray = runtimeService.getVariable(processInstance.getId(), "stringToArray");
        assertThat(stringToArray).isInstanceOf(ArrayNode.class);
        ArrayNode arrayNode = (ArrayNode) stringToArray;
        assertThat(arrayNode).hasSize(3);
        assertThat(arrayNode.get(0).asInt()).isEqualTo(4);
        assertThat(arrayNode.get(1).asInt()).isEqualTo(5);
        assertThat(arrayNode.get(2).asInt()).isEqualTo(6);
    }
}

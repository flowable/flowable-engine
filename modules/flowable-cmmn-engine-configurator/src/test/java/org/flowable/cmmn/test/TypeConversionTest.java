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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * Tests for type conversion of in/out parameters across BPMN and CMMN engines.
 *
 * @author Tijs Rademakers
 */
public class TypeConversionTest extends AbstractProcessEngineIntegrationTest {

    /**
     * Tests CMMN process task (CMMN calling BPMN) with typed in/out parameters.
     */
    @Test
    @CmmnDeployment
    @org.flowable.engine.test.Deployment(resources = "org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
    public void testProcessTaskTypeConversion() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("processDefinitionKey", "oneTask");
        variables.put("stringIntVar", "123");
        variables.put("intVar", 42);
        variables.put("stringBooleanVar", "true");
        variables.put("stringDoubleVar", "3.14");
        variables.put("stringLongVar", "789012345678");
        variables.put("periodVar", "P10D");
        variables.put("durationVar", "PT10H");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("processTaskTypeConversion")
                .variables(variables)
                .start();

        // Get the child process instance
        Task processTask = processEngine.getTaskService().createTaskQuery().singleResult();
        assertThat(processTask).isNotNull();

        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(processTask.getProcessInstanceId())
                .singleResult();

        // Verify in parameter type conversions on the child process
        assertThat(processEngine.getRuntimeService().getVariable(processInstance.getId(), "stringToInt"))
                .isInstanceOf(Integer.class).isEqualTo(123);
        assertThat(processEngine.getRuntimeService().getVariable(processInstance.getId(), "intToString"))
                .isInstanceOf(String.class).isEqualTo("42");
        assertThat(processEngine.getRuntimeService().getVariable(processInstance.getId(), "stringToBoolean"))
                .isInstanceOf(Boolean.class).isEqualTo(true);
        assertThat(processEngine.getRuntimeService().getVariable(processInstance.getId(), "stringToDouble"))
                .isInstanceOf(Double.class).isEqualTo(3.14);
        assertThat(processEngine.getRuntimeService().getVariable(processInstance.getId(), "stringToLong"))
                .isInstanceOf(Long.class).isEqualTo(789012345678L);

        // Verify period/duration conversions
        Date now = new Date();
        Object periodToDate = processEngine.getRuntimeService().getVariable(processInstance.getId(), "periodToDate");
        assertThat(periodToDate).isInstanceOf(Date.class);
        assertThat(Duration.between(now.toInstant(), ((Date) periodToDate).toInstant()))
                .isBetween(Duration.ofDays(9).plusHours(23), Duration.ofDays(10).plusMinutes(1));

        Object durationToLocalDate = processEngine.getRuntimeService().getVariable(processInstance.getId(), "durationToLocalDate");
        assertThat(durationToLocalDate).isInstanceOf(LocalDate.class);
        LocalDate today = LocalDate.now();
        assertThat((LocalDate) durationToLocalDate).isBetween(today, today.plusDays(1));

        // Set variables on the process instance for out parameter testing
        processEngine.getRuntimeService().setVariable(processInstance.getId(), "processIntVar", "456");
        processEngine.getRuntimeService().setVariable(processInstance.getId(), "processStringVar", 789);

        // Complete the process task to trigger out parameter mapping
        processEngine.getTaskService().complete(processTask.getId());

        // Verify out parameter type conversions on the parent case
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "outStringToInt"))
                .isInstanceOf(Integer.class).isEqualTo(456);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "outIntToString"))
                .isInstanceOf(String.class).isEqualTo("789");
    }

    /**
     * Tests CMMN case task (CMMN calling CMMN) with typed in/out parameters.
     */
    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/TypeConversionTest.testCaseTaskTypeConversion.cmmn",
            "org/flowable/cmmn/test/oneHumanTaskCase.cmmn"
    })
    public void testCaseTaskTypeConversion() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringIntVar", "123");
        variables.put("intVar", 42);
        variables.put("stringBooleanVar", "true");
        variables.put("periodVar", "P10D");
        variables.put("durationVar", "PT10H");

        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("parentCaseTypeConversion")
                .variables(variables)
                .start();

        // Get the child case instance
        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceParentId(parentCaseInstance.getId())
                .singleResult();
        assertThat(childCaseInstance).isNotNull();

        // Verify in parameter type conversions on the child case
        assertThat(cmmnRuntimeService.getVariable(childCaseInstance.getId(), "stringToInt"))
                .isInstanceOf(Integer.class).isEqualTo(123);
        assertThat(cmmnRuntimeService.getVariable(childCaseInstance.getId(), "intToString"))
                .isInstanceOf(String.class).isEqualTo("42");
        assertThat(cmmnRuntimeService.getVariable(childCaseInstance.getId(), "stringToBoolean"))
                .isInstanceOf(Boolean.class).isEqualTo(true);

        // Verify period/duration conversions
        Date now = new Date();
        Object periodToDate = cmmnRuntimeService.getVariable(childCaseInstance.getId(), "periodToDate");
        assertThat(periodToDate).isInstanceOf(Date.class);
        assertThat(Duration.between(now.toInstant(), ((Date) periodToDate).toInstant()))
                .isBetween(Duration.ofDays(9).plusHours(23), Duration.ofDays(10).plusMinutes(1));

        Object durationToLocalDate = cmmnRuntimeService.getVariable(childCaseInstance.getId(), "durationToLocalDate");
        assertThat(durationToLocalDate).isInstanceOf(LocalDate.class);
        LocalDate today = LocalDate.now();
        assertThat((LocalDate) durationToLocalDate).isBetween(today, today.plusDays(1));

        // Set variables on the child case for out parameter testing
        cmmnRuntimeService.setVariable(childCaseInstance.getId(), "childIntVar", "456");
        cmmnRuntimeService.setVariable(childCaseInstance.getId(), "childStringVar", 789);

        // Complete the child case task
        Task childTask = cmmnTaskService.createTaskQuery().caseInstanceId(childCaseInstance.getId()).singleResult();
        assertThat(childTask).isNotNull();
        cmmnTaskService.complete(childTask.getId());

        // Verify out parameter type conversions on the parent case
        assertThat(cmmnRuntimeService.getVariable(parentCaseInstance.getId(), "outStringToInt"))
                .isInstanceOf(Integer.class).isEqualTo(456);
        assertThat(cmmnRuntimeService.getVariable(parentCaseInstance.getId(), "outIntToString"))
                .isInstanceOf(String.class).isEqualTo("789");
    }

    /**
     * Tests BPMN case service task (BPMN calling CMMN) with typed in/out parameters.
     */
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
    public void testCaseServiceTaskTypeConversion() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/TypeConversionTest.caseServiceTaskTypeConversion.bpmn20.xml")
                .deploy();

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("stringIntVar", "123");
            variables.put("intVar", 42);
            variables.put("stringBooleanVar", "true");

            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("caseServiceTaskTypeConversion", variables);

            // Get the child case instance
            Execution execution = processEngineRuntimeService.createExecutionQuery().onlyChildExecutions()
                    .processInstanceId(processInstance.getId())
                    .activityId("caseTask")
                    .singleResult();

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceCallbackId(execution.getId())
                    .caseInstanceCallbackType(CallbackTypes.EXECUTION_CHILD_CASE)
                    .singleResult();
            assertThat(caseInstance).isNotNull();

            // Verify in parameter type conversions on the child case
            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringToInt"))
                    .isInstanceOf(Integer.class).isEqualTo(123);
            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "intToString"))
                    .isInstanceOf(String.class).isEqualTo("42");
            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringToBoolean"))
                    .isInstanceOf(Boolean.class).isEqualTo(true);

            // Set variables on the child case for out parameter testing
            cmmnRuntimeService.setVariable(caseInstance.getId(), "childIntVar", "456");
            cmmnRuntimeService.setVariable(caseInstance.getId(), "childStringVar", 789);

            // Complete the child case task to trigger out parameter mapping
            Task caseTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(caseTask).isNotNull();
            cmmnTaskService.complete(caseTask.getId());

            // Verify out parameter type conversions on the parent process
            assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "outStringToInt"))
                    .isInstanceOf(Integer.class).isEqualTo(456);
            assertThat(processEngineRuntimeService.getVariable(processInstance.getId(), "outIntToString"))
                    .isInstanceOf(String.class).isEqualTo("789");

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
}

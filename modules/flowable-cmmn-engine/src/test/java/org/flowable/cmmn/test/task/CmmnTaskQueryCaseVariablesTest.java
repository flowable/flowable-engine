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
package org.flowable.cmmn.test.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.data.MapEntry;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Christopher Welsch
 */
public class CmmnTaskQueryCaseVariablesTest extends FlowableCmmnTestCase {

    private List<String> taskIds = new ArrayList<>();

    @Before
    public void deploy() {
        deployOneHumanTaskCaseModel();

    }

    @After
    public void tearDown() throws Exception {
        cmmnEngineConfiguration.getIdmIdentityService()
                .deleteGroup("accountancy");
        cmmnEngineConfiguration.getIdmIdentityService()
                .deleteGroup("management");
        cmmnEngineConfiguration.getIdmIdentityService()
                .deleteUser("fozzie");
        cmmnEngineConfiguration.getIdmIdentityService()
                .deleteUser("gonzo");
        cmmnEngineConfiguration.getIdmIdentityService()
                .deleteUser("kermit");
        cmmnTaskService.deleteTasks(taskIds, true);
    }

    @Test
    public void testCaseVariableValueEquals() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance()
                .getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        // Start case-instance with all types of variables
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();
        // Test query matches
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("longVar", 928374L)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("shortVar", (short) 123)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("integerVar", 1234)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("stringVar", "stringValue")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("booleanVar", true)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("dateVar", date)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("nullVar", null)
                .count()).isEqualTo(1);

        // Test query for other values on existing variables
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("longVar", 999L)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("shortVar", (short) 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("integerVar", 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("stringVar", "999")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("booleanVar", false)
                .count()).isZero();
        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("dateVar", otherDate.getTime())
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("nullVar", "999")
                .count()).isZero();

        // Test querying for task variables don't match the case-variables
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("longVar", 928374L)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("shortVar", (short) 123)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("integerVar", 1234)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("stringVar", "stringValue")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("booleanVar", true)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("dateVar", date)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("nullVar", null)
                .count()).isZero();

        // Test querying for task variables not equals
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("longVar", 999L)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("shortVar", (short) 999)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("integerVar", 999)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("stringVar", "999")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("booleanVar", false)
                .count()).isEqualTo(1);

        // and query for the existing variable with NOT should result in nothing found:
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("longVar", 928374L)
                .count()).isZero();

        // Test value-only variable equals
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(928374L)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals((short) 123)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(1234)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("stringValue")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(true)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(date)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(null)
                .count()).isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(999999L)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals((short) 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(9999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("unexistingstringvalue")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(false)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(otherDate.getTime())
                .count()).isZero();

        // Test combination of task-variable and case-variable
        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "theValue");
        cmmnTaskService.setVariableLocal(task.getId(), "longVar", 928374L);

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("longVar", 928374L)
                .taskVariableValueEquals("taskVar", "theValue")
                .count())
                .isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("longVar", 928374L)
                .taskVariableValueEquals("longVar", 928374L)
                .count())
                .isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(928374L)
                .taskVariableValueEquals("theValue")
                .count()).isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals(928374L)
                .taskVariableValueEquals(928374L)
                .count()).isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("longVar", 928374L)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("shortVar", (short) 123)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("integerVar", 1234)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("stringVar", "stringValue")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("booleanVar", true)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("dateVar", date)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("nullVar", null)
                .count()).isEqualTo(1);

        // Test query for other values on existing variables
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("longVar", 999L)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("shortVar", (short) 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("integerVar", 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("stringVar", "999")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("booleanVar", false)
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals("dateVar", otherDate.getTime())
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals("nullVar", "999")
                .count()).isZero();

        // Test querying for task variables don't match the case-variables
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals("longVar", 928375L)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals("shortVar", (short) 123)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals("integerVar", 1234)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals("stringVar", "stringValue")
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals("booleanVar", true)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals("dateVar", date)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .taskVariableValueEquals("nullVar", null)
                .count()).isZero();

        // Test querying for task variables not equals
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueNotEquals("longVar", 999L)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueNotEquals("shortVar", (short) 999)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueNotEquals("integerVar", 999)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueNotEquals("stringVar", "999")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueNotEquals("booleanVar", false)
                .count()).isEqualTo(1);

        // and query for the existing variable with NOT should result in nothing found:
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueNotEquals("longVar", 928374L)
                .count()).isZero();

        // Test value-only variable equals
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(928374L)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals((short) 123)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(1234)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals("stringValue")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(true)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(date)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(null)
                .count()).isEqualTo(1);

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(999999L)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals((short) 999)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(9999)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals("unexistingstringvalue")
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(false)
                .count()).isZero();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(otherDate.getTime())
                .count()).isZero();

        // Test combination of task-variable and case-variable
        task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "theValue");
        cmmnTaskService.setVariableLocal(task.getId(), "longVar", 928374L);
        waitForAsyncHistoryExecutorToProcessAllJobs();

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals("longVar", 928374L)
                .taskVariableValueEquals("taskVar", "theValue")
                .count())
                .isEqualTo(1);

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals("longVar", 928374L)
                .taskVariableValueEquals("longVar", 928374L)
                .count())
                .isEqualTo(1);

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(928374L)
                .taskVariableValueEquals("theValue")
                .count()).isEqualTo(1);

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEquals(928374L)
                .taskVariableValueEquals(928374L)
                .count()).isEqualTo(1);
    }

    @Test
    public void testCaseVariableValueEqualsOr() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "stringValue");
        variables.put("booleanVar", true);
        Date date = Calendar.getInstance()
                .getTime();
        variables.put("dateVar", date);
        variables.put("nullVar", null);

        // Start case-instance with all types of variables
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        // Test query matches
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("longVar", 928374L)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("shortVar", (short) 123)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("integerVar", 1234)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("stringVar", "stringValue")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("booleanVar", true)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("dateVar", date)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("nullVar", null)
                .count()).isEqualTo(1);

        // Test query for other values on existing variables
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("longVar", 999L)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("shortVar", (short) 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("integerVar", 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("stringVar", "999")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("booleanVar", false)
                .count()).isZero();
        Calendar otherDate = Calendar.getInstance();
        otherDate.add(Calendar.YEAR, 1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("dateVar", otherDate.getTime())
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("nullVar", "999")
                .count()).isZero();

        // Test querying for task variables don't match the case-variables
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .taskVariableValueEquals("longVar", 928374L)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .taskVariableValueEquals("shortVar", (short) 123)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .taskVariableValueEquals("integerVar", 1234)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .taskVariableValueEquals("stringVar", "stringValue")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .taskVariableValueEquals("booleanVar", true)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .taskVariableValueEquals("dateVar", date)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .taskVariableValueEquals("nullVar", null)
                .count()).isZero();

        // Test querying for task variables not equals
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueNotEquals("longVar", 999L)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueNotEquals("shortVar", (short) 999)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueNotEquals("integerVar", 999)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueNotEquals("stringVar", "999")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueNotEquals("booleanVar", false)
                .count()).isEqualTo(1);

        // and query for the existing variable with NOT should result in nothing found:
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("longVar", 928374L)
                .count()).isZero();

        // Test value-only variable equals
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(928374L)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals((short) 123)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(1234)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("stringValue")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(true)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(date)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(null)
                .count()).isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(999999L)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals((short) 999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(9999)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals("unexistingstringvalue")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(false)
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .or()
                .taskId("invalid")
                .caseVariableValueEquals(otherDate.getTime())
                .count()).isZero();
    }

    @Test
    public void testVariableValueEqualsIgnoreCase() {

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(task).isNotNull();

        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");
        variables.put("upper", "AZERTY");
        variables.put("lower", "azerty");
        cmmnTaskService.setVariablesLocal(task.getId(), variables);

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("mixed", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("mixed", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("mixed", "uiop")
                .count()).isZero();

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("upper", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("upper", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("upper", "uiop")
                .count()).isZero();

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("lower", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("lower", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("lower", "uiop")
                .count()).isZero();

        // Test not-equals
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("mixed", "azerTY")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("mixed", "azerty")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("mixed", "uiop")
                .count()).isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("upper", "azerTY")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("upper", "azerty")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("upper", "uiop")
                .count()).isEqualTo(1);

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("lower", "azerTY")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("lower", "azerty")
                .count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("lower", "uiop")
                .count()).isEqualTo(1);

    }

    @Test
    public void testCaseVariableValueEqualsIgnoreCase() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");
        variables.put("upper", "AZERTY");
        variables.put("lower", "azerty");

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("mixed", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("mixed", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("mixed", "uiop")
                .count()).isZero();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("upper", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("upper", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("upper", "uiop")
                .count()).isZero();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("lower", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("lower", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("lower", "uiop")
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("mixed", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("mixed", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("mixed", "uiop")
                .count()).isZero();

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("upper", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("upper", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("upper", "uiop")
                .count()).isZero();

        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("lower", "azerTY")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("lower", "azerty")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueEqualsIgnoreCase("lower", "uiop")
                .count()).isZero();
    }

    @Test
    public void testCaseVariableValueLike() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLike("mixed", "Azer%")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLike("mixed", "A%")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLike("mixed", "a%")
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLike("mixed", "Azer%")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLike("mixed", "A%")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLike("mixed", "a%")
                .count()).isZero();

    }

    @Test
    public void testCaseVariableValueLikeIgnoreCase() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("mixed", "AzerTY");

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLikeIgnoreCase("mixed", "azer%")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLikeIgnoreCase("mixed", "a%")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLikeIgnoreCase("mixed", "Azz%")
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLikeIgnoreCase("mixed", "azer%")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLikeIgnoreCase("mixed", "a%")
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLikeIgnoreCase("mixed", "Azz%")
                .count()).isZero();
    }

    @Test
    public void testCaseVariableValueGreaterThan() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueGreaterThan("number", 5)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueGreaterThan("number", 10)
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueGreaterThan("number", 5)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueGreaterThan("number", 10)
                .count()).isZero();

    }

    @Test
    public void testCaseVariableValueExistsNotExists() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableExists("number")
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableNotExists("NotNumber")
                .count()).isEqualTo(1);
    }

    @Test
    public void testCaseVariableValueGreaterThanOrEquals() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueGreaterThanOrEqual("number", 5)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueGreaterThanOrEqual("number", 10)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueGreaterThanOrEqual("number", 11)
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueGreaterThanOrEqual("number", 5)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueGreaterThanOrEqual("number", 10)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueGreaterThanOrEqual("number", 11)
                .count()).isZero();
    }

    @Test
    public void testCaseVariableValueLessThan() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLessThan("number", 12)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLessThan("number", 10)
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLessThan("number", 12)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLessThan("number", 10)
                .count()).isZero();
    }

    @Test
    public void testCaseVariableValueLessThanOrEquals() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("number", 10);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLessThanOrEqual("number", 12)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLessThanOrEqual("number", 10)
                .count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueLessThanOrEqual("number", 8)
                .count()).isZero();

        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLessThanOrEqual("number", 12)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLessThanOrEqual("number", 10)
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                .caseVariableValueLessThanOrEqual("number", 8)
                .count()).isZero();

    }

    @Test
    public void testIncludeCaseVariablesWithPaging() {

        for (int i = 0; i < 10; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variables(Collections.singletonMap("caseVar", "value" + i))
                    .start();
            Task task = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(task).isNotNull();

            task.setName("task" + i);
            cmmnTaskService.saveTask(task);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                        .taskId(task.getId())
                        .singleResult()).isNotNull();
            }

            cmmnTaskService.setPriority(task.getId(), i);
        }

        assertThat(cmmnTaskService.createTaskQuery()
                .count()).isEqualTo(10);
        assertThat(cmmnTaskService.createTaskQuery()
                .list()).hasSize(10);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        tasks = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        Task task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables())
                .containsOnly(entry("caseVar", "value1"));
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .list()).hasSize(10);

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            HistoricTaskInstance historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getCaseVariables())
                    .containsOnly(entry("caseVar", "value1"));
        }
    }

    @Test
    public void testIncludeCaseVariablesAndTaskLocalVariablesWithPaging() {

        for (int i = 0; i < 10; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variables(Collections.singletonMap("caseVar", "value" + i))
                    .start();
            Task task = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(task).isNotNull();

            task.setName("task" + i);
            cmmnTaskService.saveTask(task);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                        .taskId(task.getId())
                        .singleResult()).isNotNull();
            }

            cmmnTaskService.setPriority(task.getId(), i);
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + i);
        }

        assertThat(cmmnTaskService.createTaskQuery()
                .count()).isEqualTo(10);
        assertThat(cmmnTaskService.createTaskQuery()
                .list()).hasSize(10);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        tasks = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        Task task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables())
                .containsOnly(entry("caseVar", "value1"));
        assertThat(task.getTaskLocalVariables()).isEmpty();

        tasks = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .includeTaskLocalVariables()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables())
                .containsOnly(entry("caseVar", "value1"));
        assertThat(task.getTaskLocalVariables())
                .containsOnly(entry("taskVar", "taskValue1"));

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .list()).hasSize(10);

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            HistoricTaskInstance historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getCaseVariables())
                    .containsOnly(entry("caseVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables()).isEmpty();

            historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .includeTaskLocalVariables()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getCaseVariables())
                    .containsOnly(entry("caseVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables())
                    .containsOnly(entry("taskVar", "taskValue1"));
        }
    }

    @Test
    public void testIncludeCaseVariablesAndTaskLocalVariablesAndIncludeIdentityLinksWithPaging() {

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .count()).isEqualTo(0);
        }

        taskIds.clear();

        for (int i = 0; i < 10; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variables(Collections.singletonMap("caseVar", "value" + i))
                    .start();
            Task task = cmmnTaskService.createTaskQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(task).isNotNull();

            task.setName("task" + i);
            cmmnTaskService.saveTask(task);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                        .taskId(task.getId())
                        .singleResult()).isNotNull();
            }

            cmmnTaskService.setPriority(task.getId(), i);

            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + i);
            cmmnTaskService.addGroupIdentityLink(task.getId(), "group" + i, IdentityLinkType.CANDIDATE);
            cmmnTaskService.addGroupIdentityLink(task.getId(), "otherGroup" + i, IdentityLinkType.CANDIDATE);
            cmmnTaskService.addUserIdentityLink(task.getId(), "user" + i, IdentityLinkType.CANDIDATE);
        }

        assertThat(cmmnTaskService.createTaskQuery()
                .count()).isEqualTo(10);
        assertThat(cmmnTaskService.createTaskQuery()
                .list()).hasSize(10);

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        tasks = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        Task task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables())
                .containsOnly(entry("caseVar", "value1"));
        assertThat(task.getTaskLocalVariables()).isEmpty();
        assertThat(task.getIdentityLinks()).isEmpty();

        tasks = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .includeTaskLocalVariables()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables())
                .containsOnly(entry("caseVar", "value1"));
        assertThat(task.getTaskLocalVariables())
                .containsOnly(entry("taskVar", "taskValue1"));
        assertThat(task.getIdentityLinks()).isEmpty();

        tasks = cmmnTaskService.createTaskQuery()
                .includeCaseVariables()
                .includeTaskLocalVariables()
                .includeIdentityLinks()
                .orderByTaskPriority()
                .asc()
                .listPage(0, 4);
        assertThat(tasks)
                .extracting(TaskInfo::getName)
                .containsExactly("task0", "task1", "task2", "task3");

        task = tasks.get(1);
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables())
                .containsOnly(entry("caseVar", "value1"));
        assertThat(task.getTaskLocalVariables())
                .containsOnly(entry("taskVar", "taskValue1"));
        assertThat(task.getIdentityLinks())
                .extracting(IdentityLinkInfo::getGroupId, IdentityLinkInfo::getUserId)
                .containsExactlyInAnyOrder(
                        tuple("group1", null),
                        tuple("otherGroup1", null),
                        tuple(null, "user1")
                );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .count()).isEqualTo(10);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .list()).hasSize(10);

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            HistoricTaskInstance historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getCaseVariables())
                    .containsOnly(entry("caseVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables()).isEmpty();
            assertThat(historicTask.getIdentityLinks()).isEmpty();

            historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .includeTaskLocalVariables()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getCaseVariables())
                    .containsOnly(entry("caseVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables())
                    .containsOnly(entry("taskVar", "taskValue1"));
            assertThat(historicTask.getIdentityLinks()).isEmpty();

            historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .includeTaskLocalVariables()
                    .includeIdentityLinks()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 4);
            assertThat(historicTasks)
                    .extracting(TaskInfo::getName)
                    .containsExactly("task0", "task1", "task2", "task3");

            historicTask = historicTasks.get(1);
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getCaseVariables())
                    .containsOnly(entry("caseVar", "value1"));
            assertThat(historicTask.getTaskLocalVariables())
                    .containsOnly(entry("taskVar", "taskValue1"));
            assertThat(historicTask.getIdentityLinks())
                    .extracting(IdentityLinkInfo::getGroupId, IdentityLinkInfo::getUserId)
                    .containsExactlyInAnyOrder(
                            tuple("group1", null),
                            tuple("otherGroup1", null),
                            tuple(null, "user1")
                    );
        }
    }

    /**
     * Test confirming fix for ACT-1731
     */
    @Test
    public void testIncludeBinaryVariables() {

        // Start case with a binary variable
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(Collections.singletonMap("binaryVariable", "It is I, le binary".getBytes()))
                .start();
        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(task).isNotNull();
        cmmnTaskService.setVariableLocal(task.getId(), "binaryTaskVariable", "It is I, le binary".getBytes());

        // Query task, including caseVariables
        task = cmmnTaskService.createTaskQuery()
                .taskId(task.getId())
                .includeCaseVariables()
                .singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables()).isNotNull();
        byte[] bytes = (byte[]) task.getCaseVariables()
                .get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");

        // Query task, including taskVariables
        task = cmmnTaskService.createTaskQuery()
                .taskId(task.getId())
                .includeTaskLocalVariables()
                .singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskLocalVariables()).isNotNull();
        bytes = (byte[]) task.getTaskLocalVariables()
                .get("binaryTaskVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");
    }

    /**
     * Test confirming fix for ACT-1731
     */
    @Test
    public void testIncludeBinaryVariablesOr() {
        // Start case with a binary variable

        CaseInstance caseInstance = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(Collections.singletonMap("binaryVariable", "It is I, le binary".getBytes()))
                .start();
        Task task = cmmnTaskService.createTaskQuery()
                .or()
                .taskName("invalid")
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(task).isNotNull();
        cmmnTaskService.setVariableLocal(task.getId(), "binaryTaskVariable", "It is I, le binary".getBytes());

        // Query task, including caseVariables
        task = cmmnTaskService.createTaskQuery()
                .or()
                .taskName("invalid")
                .taskId(task.getId())
                .includeCaseVariables()
                .singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getCaseVariables()).isNotNull();
        byte[] bytes = (byte[]) task.getCaseVariables()
                .get("binaryVariable");
        assertThat(new String(bytes)).isEqualTo("It is I, le binary");
    }

    @Test
    public void testIncludeTaskLocalAndCaseInstanceVariableHasTenant() {

        addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                .tenantId("testTenant")
                .deploy()
        );
        for (int i = 0; i < 10; i++) {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variables(Collections.singletonMap("simpleVar", "simpleVarValue"))
                    .tenantId("testTenant")
                    .start();
        }

        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseDefinitionKey("oneTaskCase")
                .includeCaseVariables()
                .includeTaskLocalVariables()
                .list();

        assertThat(tasks).hasSize(10);
        for (Task task : tasks) {
            assertThat(task.getTenantId()).isEqualTo("testTenant");
        }
    }

    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {

        Task taskWithStringValue = cmmnTaskService.createTaskBuilder()
                .name("With string value")
                .create();
        taskIds.add(taskWithStringValue.getId());
        cmmnTaskService.setVariable(taskWithStringValue.getId(), "var", "TEST");

        Task taskWithNullValue = cmmnTaskService.createTaskBuilder()
                .name("With null value")
                .create();
        taskIds.add(taskWithNullValue.getId());
        cmmnTaskService.setVariable(taskWithNullValue.getId(), "var", null);

        Task taskWithLongValue = cmmnTaskService.createTaskBuilder()
                .name("With long value")
                .create();
        taskIds.add(taskWithLongValue.getId());
        cmmnTaskService.setVariable(taskWithLongValue.getId(), "var", 100L);

        Task taskWithDoubleValue = cmmnTaskService.createTaskBuilder()
                .name("With double value")
                .create();
        taskIds.add(taskWithDoubleValue.getId());
        cmmnTaskService.setVariable(taskWithDoubleValue.getId(), "var", 45.55);

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEquals("var", "TEST")
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("var", "TEST")
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEquals("var", 100L)
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId()),
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("var", 100L)
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With long value", taskWithLongValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEquals("var", 45.55)
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId()),
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("var", 45.55)
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEquals("var", "test")
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId()),
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueNotEqualsIgnoreCase("var", "test")
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", taskWithNullValue.getId()),
                        tuple("With long value", taskWithLongValue.getId()),
                        tuple("With double value", taskWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEquals("var", "test")
                .list())
                .extracting(Task::getName, Task::getId)
                .isEmpty();

        assertThat(cmmnTaskService.createTaskQuery()
                .taskVariableValueEqualsIgnoreCase("var", "test")
                .list())
                .extracting(Task::getName, Task::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", taskWithStringValue.getId())
                );

    }

    @Test
    public void testQueryCaseVariableValueEqualsAndNotEquals() {

        CaseInstance caseWithStringValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        CaseInstance caseWithNullValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With null value")
                .variable("var", null)
                .start();

        CaseInstance caseWithLongValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With long value")
                .variable("var", 100L)
                .start();

        CaseInstance caseWithDoubleValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("var", "TEST")
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithNullValue.getId()),
                        tuple("The Task", caseWithLongValue.getId()),
                        tuple("The Task", caseWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("var", "TEST")
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithStringValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("var", 100L)
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithStringValue.getId()),
                        tuple("The Task", caseWithNullValue.getId()),
                        tuple("The Task", caseWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("var", 100L)
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithLongValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("var", 45.55)
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithStringValue.getId()),
                        tuple("The Task", caseWithNullValue.getId()),
                        tuple("The Task", caseWithLongValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("var", 45.55)
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueNotEquals("var", "test")
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithStringValue.getId()),
                        tuple("The Task", caseWithNullValue.getId()),
                        tuple("The Task", caseWithLongValue.getId()),
                        tuple("The Task", caseWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .includeTaskLocalVariables()
                .caseVariableValueNotEqualsIgnoreCase("var", "test")
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithNullValue.getId()),
                        tuple("The Task", caseWithLongValue.getId()),
                        tuple("The Task", caseWithDoubleValue.getId())
                );

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEquals("var", "test")
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .isEmpty();

        assertThat(cmmnTaskService.createTaskQuery()
                .caseVariableValueEqualsIgnoreCase("var", "test")
                .list())
                .extracting(Task::getName, Task::getScopeId)
                .containsExactlyInAnyOrder(
                        tuple("The Task", caseWithStringValue.getId())
                );
    }

    @Test
    public void testOrQueryMultipleVariableValues() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            Map<String, Object> startMap = new HashMap<>();
            startMap.put("caseVar", true);
            startMap.put("anotherProcessVar", 123);
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variables(startMap)
                    .start();
            startMap.put("anotherProcessVar", 999);
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variables(startMap)
                    .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();
            HistoricTaskInstanceQuery query0 = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .or();
            for (int i = 0; i < 20; i++) {
                query0 = query0.caseVariableValueEquals("anotherProcessVar", i);
            }
            query0 = query0.endOr();
            assertThat(query0.singleResult()).isNull();

            HistoricTaskInstanceQuery query1 = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .includeCaseVariables()
                    .or()
                    .caseVariableValueEquals("anotherProcessVar", 123);
            for (int i = 0; i < 20; i++) {
                query1 = query1.caseVariableValueEquals("anotherProcessVar", i);
            }
            waitForAsyncHistoryExecutorToProcessAllJobs();
            query1 = query1.endOr();
            HistoricTaskInstance task = query1.singleResult();
            assertThat(task.getCaseVariables())
                    .containsOnly(
                            entry("caseVar", true),
                            entry("anotherProcessVar", 123)
                    );
        }
    }

    @Test
    public void testQueryVariableExists() {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {

            Map<String, Object> varMap = Collections.singletonMap("caseVar", "test");

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variables(varMap)
                    .start();
            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<HistoricTaskInstance> tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .caseVariableExists("caseVar")
                    .list();
            assertThat(tasks).hasSize(1);

            tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .caseVariableNotExists("caseVar")
                    .list();
            assertThat(tasks).isEmpty();

            tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .or()
                    .caseVariableExists("caseVar")
                    .caseDefinitionId("undexisting")
                    .endOr()
                    .list();
            assertThat(tasks).hasSize(1);

            tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .or()
                    .caseVariableNotExists("caseVar")
                    .caseInstanceId(caseInstance.getId())
                    .endOr()
                    .list();
            assertThat(tasks).hasSize(1);

            cmmnRuntimeService.setVariable(caseInstance.getId(), "caseVar2", "test2");
            waitForAsyncHistoryExecutorToProcessAllJobs();

            tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .caseVariableExists("caseVar")
                    .caseVariableValueEquals("caseVar2", "test2")
                    .list();
            assertThat(tasks).hasSize(1);

            tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .caseVariableNotExists("caseVar")
                    .caseVariableValueEquals("caseVar2", "test2")
                    .list();
            assertThat(tasks).isEmpty();

            tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .or()
                    .caseVariableExists("caseVar")
                    .caseVariableValueEquals("caseVar2", "test2")
                    .endOr()
                    .list();
            assertThat(tasks).hasSize(1);

            tasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .or()
                    .caseVariableNotExists("caseVar")
                    .caseVariableValueEquals("caseVar2", "test2")
                    .endOr()
                    .list();
            assertThat(tasks).hasSize(1);
        }
    }

    public static <K, V> MapEntry<K, V> entry(K key, V value) {
        return MapEntry.entry(key, value);
    }

}

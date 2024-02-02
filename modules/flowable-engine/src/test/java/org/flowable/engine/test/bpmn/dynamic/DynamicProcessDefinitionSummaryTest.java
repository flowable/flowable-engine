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
package org.flowable.engine.test.bpmn.dynamic;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.dynamic.DynamicProcessDefinitionSummary;
import org.flowable.engine.dynamic.PropertiesParserConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Created by Pardo David on 1/12/2016.
 */
public class DynamicProcessDefinitionSummaryTest extends PluggableFlowableTestCase implements DynamicBpmnConstants, PropertiesParserConstants {

    private static final String TASK_ONE_SID = "sid-B94D5D22-E93E-4401-ADC5-C5C073E1EEB4";
    private static final String TASK_TWO_SID = "sid-B1C37EBE-A273-4DDE-B909-89302638526A";
    private static final String SCRIPT_TASK_SID = "sid-A403BAE0-E367-449A-90B2-48834FCAA2F9";

    @Test
    public void testProcessDefinitionInfoCacheIsEnabledWithPluggableActivitiTestCase() throws Exception {
        assertThat(processEngineConfiguration.isEnableProcessDefinitionInfoCache()).isTrue();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testIfNoProcessInfoIsAvailableTheBpmnModelIsUsed() throws Exception {
        // setup
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        DynamicProcessDefinitionSummary summary = dynamicBpmnService.getDynamicProcessDefinitionSummary(processInstance.getProcessDefinitionId());

        // first task
        JsonNode jsonNode = summary.getElement(TASK_ONE_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "    userTaskName: {"
                        + "       bpmnmodel: 'Taak 1'"
                        + "    },"
                        + "    userTaskAssignee: { },"
                        + "    userTaskCandidateUsers: {"
                        + "       bpmnmodel: [ 'david' ]"
                        + "    },"
                        + "    userTaskCandidateGroups: {"
                        + "       bpmnmodel: [ ]"
                        + "    }"
                        + " }");

        // second task
        jsonNode = summary.getElement(TASK_TWO_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "    userTaskName: {"
                        + "       bpmnmodel: 'Taak 2'"
                        + "    },"
                        + "    userTaskAssignee: { },"
                        + "    userTaskCandidateUsers: {"
                        + "       bpmnmodel: [ 'david' ]"
                        + "    },"
                        + "    userTaskCandidateGroups: {"
                        + "       bpmnmodel: [ 'HR', 'SALES' ]"
                        + "    }"
                        + " }");

        // script task
        jsonNode = summary.getElement(SCRIPT_TASK_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(jsonNode)
                .isEqualTo("{"
                        + "    scriptTaskScript: {"
                        + "        bpmnmodel: 'var test = \"hallo\";'"
                        + "    }"
                        + " }");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testTheCandidateUserOfTheFirstTasksIsChanged() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateUser(TASK_ONE_SID, "bob", false);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, processInfo);

        DynamicProcessDefinitionSummary summary = dynamicBpmnService.getDynamicProcessDefinitionSummary(processDefinitionId);

        JsonNode taskOneNode = summary.getElement(TASK_ONE_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(taskOneNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    userTaskCandidateUsers: {"
                        + "       bpmnmodel: [ 'david' ],"
                        + "       dynamic:   [ 'bob']"
                        + "    }"
                        + " }");

        // verify if runtime is up to date
        runtimeService.startProcessInstanceById(processDefinitionId);
        // bob and david both should have a single task.
        org.flowable.task.api.Task bobTask = taskService.createTaskQuery().taskCandidateUser("bob").singleResult();
        assertThat(bobTask)
                .as("Bob must have one task")
                .isNotNull();

        org.flowable.task.api.Task davidTask = taskService.createTaskQuery().taskCandidateUser("david").singleResult();
        assertThat(davidTask)
                .as("David must have one task")
                .isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testTheCandidateUserOfTheFirstTasksIsChangedMultipleTimes() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateUser(TASK_ONE_SID, "bob", false);
        dynamicBpmnService.changeUserTaskCandidateUser(TASK_ONE_SID, "david", false, processInfo);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, processInfo);

        DynamicProcessDefinitionSummary summary = dynamicBpmnService.getDynamicProcessDefinitionSummary(processDefinitionId);

        JsonNode taskOneNode = summary.getElement(TASK_ONE_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(taskOneNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "    userTaskCandidateUsers: {"
                        + "       bpmnmodel: [ 'david' ],"
                        + "       dynamic:   [ 'david', 'bob']"
                        + "    }"
                        + " }");

        // verify if runtime is up to date
        runtimeService.startProcessInstanceById(processDefinitionId);

        org.flowable.task.api.Task bobTask = taskService.createTaskQuery().taskCandidateUser("bob").singleResult();
        assertThat(bobTask)
                .as("Bob must have one task")
                .isNotNull();

        List<org.flowable.task.api.Task> davidTasks = taskService.createTaskQuery().taskCandidateUser("david").list();
        assertThat(davidTasks)
                .as("David must have one task")
                .isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testTheCandidateGroupOfTheFirstTasksIsChanged() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateGroup(TASK_ONE_SID, "HR", false);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, processInfo);

        DynamicProcessDefinitionSummary summary = dynamicBpmnService.getDynamicProcessDefinitionSummary(processDefinitionId);

        JsonNode taskOneNode = summary.getElement(TASK_ONE_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(taskOneNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                .isEqualTo("{"
                        + "    userTaskCandidateGroups: {"
                        + "       dynamic:   [ 'HR']"
                        + "    }"
                        + " }");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testTheCandidateGroupOfTheFirstTasksIsChangedMultipleTimes() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ObjectNode processInfo = dynamicBpmnService.changeUserTaskCandidateGroup(TASK_ONE_SID, "HR", false);
        dynamicBpmnService.changeUserTaskCandidateGroup(TASK_ONE_SID, "SALES", false, processInfo);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, processInfo);

        DynamicProcessDefinitionSummary summary = dynamicBpmnService.getDynamicProcessDefinitionSummary(processDefinitionId);
        JsonNode taskOneNode = summary.getElement(TASK_ONE_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(taskOneNode)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_EXTRA_ARRAY_ITEMS)
                .isEqualTo("{"
                        + "    userTaskCandidateGroups: {"
                        + "       dynamic:   [ 'HR', 'SALES' ]"
                        + "    }"
                        + " }");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testTheScriptOfTheScriptTasksIsChanged() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ObjectNode jsonNodes = dynamicBpmnService.changeScriptTaskScript(SCRIPT_TASK_SID, "var x = \"hallo\";");
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, jsonNodes);

        DynamicProcessDefinitionSummary summary = dynamicBpmnService.getDynamicProcessDefinitionSummary(processDefinitionId);
        JsonNode scriptTaskNode = summary.getElement(SCRIPT_TASK_SID).get(ELEMENT_PROPERTIES);
        assertThatJson(scriptTaskNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    scriptTaskScript: {"
                        + "        dynamic: 'var x = \"hallo\";'"
                        + "    }"
                        + " }");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/dynamic/dynamic-bpmn-test-process.bpmn20.xml" })
    public void testItShouldBePossibleToResetDynamicCandidateUsers() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("dynamicServiceTest");
        String processDefinitionId = processInstance.getProcessDefinitionId();

        ObjectNode jsonNodes = dynamicBpmnService.changeUserTaskCandidateUser(TASK_ONE_SID, "bob", false);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, jsonNodes);

        // delete
        jsonNodes = dynamicBpmnService.getProcessDefinitionInfo(processDefinitionId);
        dynamicBpmnService.resetProperty(TASK_ONE_SID, USER_TASK_CANDIDATE_USERS, jsonNodes);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, jsonNodes);

        runtimeService.startProcessInstanceByKey("dynamicServiceTest");

        long count = taskService.createTaskQuery().taskCandidateUser("david").count();
        assertThat(count).isEqualTo(2);

        // additional checks of summary
        DynamicProcessDefinitionSummary summary = dynamicBpmnService.getDynamicProcessDefinitionSummary(processDefinitionId);
        JsonNode candidateUsers = summary.getElement(TASK_ONE_SID).get(ELEMENT_PROPERTIES).get(USER_TASK_CANDIDATE_USERS);
        assertThatJson(candidateUsers)
                .isEqualTo("{"
                        + "     bpmnmodel: [ 'david' ]"
                        + " }");
    }
}

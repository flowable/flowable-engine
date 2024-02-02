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
package org.flowable.examples.taskforms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.TaskCompletionBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class TaskFormsTest extends PluggableFlowableTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        identityService.saveUser(identityService.newUser("fozzie"));
        identityService.saveGroup(identityService.newGroup("management"));
        identityService.createMembership("fozzie", "management");
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteGroup("management");
        identityService.deleteUser("fozzie");
    }

    @Test
    @Deployment(resources = { "org/flowable/examples/taskforms/VacationRequest_deprecated_forms.bpmn20.xml", "org/flowable/examples/taskforms/approve.form",
            "org/flowable/examples/taskforms/request.form", "org/flowable/examples/taskforms/adjustRequest.form" })
    public void testTaskFormsWithVacationRequestProcess() {

        // Get start form
        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        Object startForm = formService.getRenderedStartForm(procDefId);
        assertThat(startForm).isNotNull();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        String processDefinitionId = processDefinition.getId();
        assertThat(formService.getStartFormData(processDefinitionId).getFormKey()).isEqualTo("org/flowable/examples/taskforms/request.form");

        // Define variables that would be filled in through the form
        Map<String, String> formProperties = new HashMap<>();
        formProperties.put("employeeName", "kermit");
        formProperties.put("numberOfDays", "4");
        formProperties.put("vacationMotivation", "I'm tired");
        formService.submitStartFormData(procDefId, formProperties);

        // Management should now have a task assigned to them
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskCandidateGroup("management").singleResult();
        assertThat(task.getDescription()).isEqualTo("Vacation request by kermit");
        Object taskForm = formService.getRenderedTaskForm(task.getId());
        assertThat(taskForm).isNotNull();

        // Rejecting the task should put the process back to first task
        TaskCompletionBuilder taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder
                .taskId(task.getId())
                .variables(CollectionUtil.singletonMap("vacationApproved", "false"))
                .complete();

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Adjust vacation request");
    }

    @Test
    @Deployment
    public void testTaskFormUnavailable() {
        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        assertThat(formService.getRenderedStartForm(procDefId)).isNull();

        runtimeService.startProcessInstanceByKey("noStartOrTaskForm");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(formService.getRenderedTaskForm(task.getId())).isNull();
    }

}

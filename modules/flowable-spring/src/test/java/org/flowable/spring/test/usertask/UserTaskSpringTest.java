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
package org.flowable.spring.test.usertask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.form.FormProperty;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Tijs Rademakers
 */
@ContextConfiguration("classpath:flowable-context.xml")
public class UserTaskSpringTest extends SpringFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/spring/test/usertask/VacationRequest.bpmn20.xml")
    public void testFormProperties() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("vacationRequest").singleResult();
        assertThat(formService.getStartFormKey(processDefinition.getId())).isNull();
        StartFormData startFormData = formService.getStartFormData(processDefinition.getId());
        assertThat(startFormData.getFormProperties()).hasSize(3);

        List<FormProperty> formProperties = startFormData.getFormProperties();
        assertThat(formProperties.get(0).getId()).isEqualTo("numberOfDays");
        assertThat(formProperties.get(1).getId()).isEqualTo("startDate");
        assertThat(formProperties.get(2).getId()).isEqualTo("vacationMotivation");

        Map<String, String> startProperties = new HashMap<>();
        startProperties.put("numberOfDays", "10");
        startProperties.put("startDate", "02-02-2018 12:00");
        startProperties.put("vacationMotivation", "Badly needed");
        ProcessInstance processInstance = formService.submitStartFormData(processDefinition.getId(), startProperties);

        Task requestTask = taskService.createTaskQuery().taskDefinitionKey("handleRequest").processInstanceId(processInstance.getId()).singleResult();
        assertThat(requestTask).isNotNull();

        TaskFormData taskFormData = formService.getTaskFormData(requestTask.getId());
        assertThat(taskFormData.getFormProperties()).hasSize(2);

        formProperties = taskFormData.getFormProperties();
        assertThat(formProperties.get(0).getId()).isEqualTo("vacationApproved");
        assertThat(formProperties.get(1).getId()).isEqualTo("managerMotivation");

        Map<String, String> taskProperties = new HashMap<>();
        taskProperties.put("vacationApproved", "true");
        formService.submitTaskFormData(requestTask.getId(), taskProperties);

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult()).isNull();
    }

}

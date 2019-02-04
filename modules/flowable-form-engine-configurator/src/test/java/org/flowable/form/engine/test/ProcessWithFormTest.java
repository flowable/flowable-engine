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
package org.flowable.form.engine.test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngines;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
@FlowableTest
@ConfigurationResource("flowable.startProcessWithFormTest.cfg.xml")
public class ProcessWithFormTest {

    @AfterEach
    public void resetSideEffect() {
        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        formRepositoryService.createDeploymentQuery().list().
            forEach(
                formDeployment -> formRepositoryService.deleteDeployment(formDeployment.getId())
            );
        SideEffectExecutionListener.reset();
    }

    @Test
    @Deployment(
        resources = {
            "org/flowable/form/engine/test/deployment/simple.form",
            "org/flowable/form/engine/test/deployment/oneTaskWithFormKeySideEffectProcess.bpmn20.xml"
        }
    )
    public void throwExceptionValidationOnStartProcess(RuntimeService runtimeService, RepositoryService repositoryService) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskWithFormSideEffectProcess")
            .singleResult();
        assertThrows(RuntimeException.class,
            () -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", Collections.singletonMap("name", "nameValue"), "test"),
            "validation failed"
        );

        Assertions.assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }

    @Test
    @Deployment(
        resources = {
            "org/flowable/form/engine/test/deployment/simple.form",
            "org/flowable/form/engine/test/deployment/oneTaskWithFormKeySideEffectProcess.bpmn20.xml"
        }
    )
    public void throwExceptionValidationOnStartProcessWithoutVariables(RuntimeService runtimeService, RepositoryService repositoryService) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskWithFormSideEffectProcess")
            .singleResult();
        assertThrows(RuntimeException.class,
            () -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", null, "test"),
            "validation failed"
        );

        Assertions.assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }

    @Test
    @Deployment(
        resources = {
            "org/flowable/form/engine/test/deployment/simple.form",
            "org/flowable/form/engine/test/deployment/oneTaskWithFormKeySideEffectProcess.bpmn20.xml"
        }
    )
    public void throwExceptionValidationOnCompleteTask(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormSideEffectProcess");
        Assertions.assertEquals(1, SideEffectExecutionListener.getSideEffect());
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThrows( RuntimeException.class,
            () ->taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.emptyMap())
        );

        Assertions.assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }
}

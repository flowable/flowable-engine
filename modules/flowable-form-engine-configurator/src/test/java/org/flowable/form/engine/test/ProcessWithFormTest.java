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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.FlowableTest;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngines;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
@FlowableTest
@ConfigurationResource("flowable.startProcessWithFormTest.cfg.xml")
public class ProcessWithFormTest {

    @AfterEach
    public void resetSideEffect(RepositoryService repositoryService) {
        repositoryService.createDeploymentQuery().list().forEach(
            deployment -> repositoryService.deleteDeployment(deployment.getId(), true)
        );
        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        formRepositoryService.createDeploymentQuery().list().
            forEach(
                formDeployment -> formRepositoryService.deleteDeployment(formDeployment.getId())
            );
        SideEffectExecutionListener.reset();
    }

    @BeforeEach
    public void deployModels(RepositoryService repositoryService) {
        repositoryService.createDeployment().
            addClasspathResource("org/flowable/form/engine/test/deployment/simple.form").
            addClasspathResource("org/flowable/form/engine/test/deployment/oneTaskWithFormKeySideEffectProcess.bpmn20.xml").
            deploy();
    }

    @Test
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
    public void startProcessWithFormWithoutValidationOnConfiguration(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService, RepositoryService repositoryService) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(false);
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskWithFormSideEffectProcess")
                .singleResult();
            ProcessInstance processInstance = runtimeService
                .startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", Collections.singletonMap("name", "nameValue"), "test");
            assertNotNull(processInstance);
            Assertions.assertEquals(1, SideEffectExecutionListener.getSideEffect());
        } finally {
            ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(true);
        }
    }

    @Test
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

    @Test
    public void completeTaskWithoutValidationOnConfiguration(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService, TaskService taskService) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(false);
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormSideEffectProcess");
            Assertions.assertEquals(1, SideEffectExecutionListener.getSideEffect());
            SideEffectExecutionListener.reset();

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

            FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

            taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.emptyMap());

            Assertions.assertEquals(1, SideEffectExecutionListener.getSideEffect());
        } finally {
            ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(true);
        }
    }

}

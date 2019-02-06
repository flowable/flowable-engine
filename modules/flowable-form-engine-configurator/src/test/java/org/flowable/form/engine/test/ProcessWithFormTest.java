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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.FlowableTest;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngines;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
@FlowableTest
@ConfigurationResource("flowable.startProcessWithFormTest.cfg.xml")
public class ProcessWithFormTest {

    protected static final String ONE_TASK_PROCESS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<definitions\n"
        + "  xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n"
        + "  xmlns:flowable=\"http://flowable.org/bpmn\"\n"
        + "  targetNamespace=\"Examples\">\n"
        + "\n"
        + "  <process id=\"oneTaskWithFormSideEffectProcess\" name=\"The One Task Process\">\n"
        + "    <documentation>This is a process for testing purposes</documentation>\n"
        + "  \n"
        + "    <startEvent id=\"theStart\" flowable:formKey=\"form1\" flowable:formFieldValidation=\"START_EVENT_VALIDATION\"/>\n"
        + "    <sequenceFlow id=\"flow1\" sourceRef=\"theStart\" targetRef=\"theTask\" />\n"
        + "    <userTask id=\"theTask\" name=\"my task\" flowable:formKey=\"form1\" flowable:assignee=\"myAssignee\" flowable:owner=\"myOwner\"\n"
        + "    \tflowable:priority=\"60\" flowable:category=\"myCategory\" flowable:dueDate=\"2021-01-01\" flowable:formFieldValidation=\"USER_TASK_VALIDATION\">\n"
        + "      <extensionElements>\n"
        + "        <flowable:executionListener event=\"start\" class=\"org.flowable.form.engine.test.SideEffectExecutionListener\"></flowable:executionListener>\n"
        + "        <flowable:executionListener event=\"end\" class=\"org.flowable.form.engine.test.SideEffectExecutionListener\"></flowable:executionListener>\n"
        + "      </extensionElements>\n"
        + "    </userTask>\n"
        + "\n"
        + "    <sequenceFlow id=\"flow2\" sourceRef=\"theTask\" targetRef=\"theEnd\" />\n"
        + "    <endEvent id=\"theEnd\" />\n"
        + "    \n"
        + "  </process>\n"
        + "\n"
        + "</definitions>\n";

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
            addString("oneTaskWithFormKeySideEffectProcess.bpmn20.xml", ONE_TASK_PROCESS.
                replace("START_EVENT_VALIDATION", "true").
                replace("USER_TASK_VALIDATION", "true")
            ).
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

        assertEquals(0, SideEffectExecutionListener.getSideEffect());
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
            assertEquals(1, SideEffectExecutionListener.getSideEffect());
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

        assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }

    @Test
    public void throwExceptionValidationOnCompleteTask(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormSideEffectProcess");
        assertEquals(1, SideEffectExecutionListener.getSideEffect());
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThrows( RuntimeException.class,
            () ->taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.emptyMap())
        );

        assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }

    @Test
    public void completeTaskWithoutValidationOnConfiguration(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService, TaskService taskService) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(false);
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormSideEffectProcess");
            assertEquals(1, SideEffectExecutionListener.getSideEffect());
            SideEffectExecutionListener.reset();

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

            FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

            taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.emptyMap());

            assertEquals(1, SideEffectExecutionListener.getSideEffect());
        } finally {
            ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevel(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService,
        TaskService taskService, RepositoryService repositoryService) {

        Deployment deployment = repositoryService.createDeployment().
            addString("oneTaskWithFormKeySideEffectProcess.bpmn20.xml",
                ONE_TASK_PROCESS.
                    replace("START_EVENT_VALIDATION", "false").
                    replace("USER_TASK_VALIDATION", "false")
            ).
            deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        ProcessInstance processInstance = runtimeService.startProcessInstanceWithForm(processDefinition.getId(),"__COMPLETE", Collections.emptyMap(),
            "oneTaskWithFormSideEffectProcess");
        assertEquals(1, SideEffectExecutionListener.getSideEffect());
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

        taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator"));

        assertEquals(1, SideEffectExecutionListener.getSideEffect());
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelExpression(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService,
        TaskService taskService, RepositoryService repositoryService) {

        Deployment deployment = repositoryService.createDeployment().
            addString("oneTaskWithFormKeySideEffectProcess.bpmn20.xml",
                ONE_TASK_PROCESS.
                    replace("START_EVENT_VALIDATION", "${true}").
                    replace("USER_TASK_VALIDATION", "${allowValidation}")
            ).
            deploy();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        assertEquals(
            "Unable to resolve formFieldValidationExpression without variable container",
            assertThrows(
                FlowableException.class,
                () -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "__COMPLETE",
                    Collections.singletonMap("allowValidation", Boolean.TRUE),
                    "oneTaskWithFormSideEffectProcess")
            ).getMessage()
        );

        assertEquals(0, SideEffectExecutionListener.getSideEffect());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "oneTaskWithFormSideEffectProcess",
            Collections.singletonMap("allowValidation", Boolean.TRUE)
        );
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

        assertThrows(
            RuntimeException.class,
            () -> taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator"))
        );

        assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelBadExpression(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService,
        TaskService taskService, RepositoryService repositoryService) {

        Deployment deployment = repositoryService.createDeployment().
            addString("oneTaskWithFormKeySideEffectProcess.bpmn20.xml",
                ONE_TASK_PROCESS.
                    replace("START_EVENT_VALIDATION", "true").
                    replace("USER_TASK_VALIDATION", "${BAD_EXPRESSION}")
            ).
            deploy();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "oneTaskWithFormSideEffectProcess",
            Collections.emptyMap()
        );
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

        assertEquals(
            "Unknown property used in expression: ${BAD_EXPRESSION}",
            assertThrows(
                FlowableException.class,
                () -> taskService
                    .completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator"))

            ).getMessage()
        );
        assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelStringExpression(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService,
        TaskService taskService, RepositoryService repositoryService) {

        Deployment deployment = repositoryService.createDeployment().
            addString("oneTaskWithFormKeySideEffectProcess.bpmn20.xml",
                ONE_TASK_PROCESS.
                    replace("START_EVENT_VALIDATION", "true").
                    replace("USER_TASK_VALIDATION", "${true}")
            ).
            deploy();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "oneTaskWithFormSideEffectProcess",
            Collections.emptyMap()
        );
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

        assertEquals(
            "validation failed",
            assertThrows(
            RuntimeException.class,
            () -> taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator"))
        ).getMessage()
        );

        assertEquals(0, SideEffectExecutionListener.getSideEffect());
    }

}

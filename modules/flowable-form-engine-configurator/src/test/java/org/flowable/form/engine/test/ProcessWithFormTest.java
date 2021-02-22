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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        formRepositoryService.createDeploymentQuery().list()
            .forEach(
                formDeployment -> formRepositoryService.deleteDeployment(formDeployment.getId(), true)
            );
        SideEffectExecutionListener.reset();
    }

    @BeforeEach
    public void deployModels(RepositoryService repositoryService) {
        repositoryService.createDeployment().
            addClasspathResource("org/flowable/form/engine/test/deployment/simple.form")
                .addString("oneTaskWithFormKeySideEffectProcess.bpmn20.xml", ONE_TASK_PROCESS
                        .replace("START_EVENT_VALIDATION", "true")
                        .replace("USER_TASK_VALIDATION", "true")
            ).
            deploy();
    }

    @Test
    public void throwExceptionValidationOnStartProcess(RuntimeService runtimeService, RepositoryService repositoryService) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskWithFormSideEffectProcess")
            .singleResult();
        assertThatThrownBy(() -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", Collections.singletonMap("name", "nameValue"), "test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("validation failed");

        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();
    }

    @Test
    public void startProcessWithFormWithoutValidationOnConfiguration(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService, RepositoryService repositoryService) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(false);
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskWithFormSideEffectProcess")
                .singleResult();
            ProcessInstance processInstance = runtimeService
                .startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", Collections.singletonMap("name", "nameValue"), "test");
            assertThat(processInstance).isNotNull();
            assertThat(SideEffectExecutionListener.getSideEffect()).isEqualTo(1);
        } finally {
            ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void throwExceptionValidationOnStartProcessWithoutVariables(RuntimeService runtimeService, RepositoryService repositoryService) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskWithFormSideEffectProcess")
            .singleResult();
        assertThatThrownBy(() -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", null, "test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("validation failed");

        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();
    }

    @Test
    public void throwExceptionValidationOnCompleteTask(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormSideEffectProcess");
        assertThat(SideEffectExecutionListener.getSideEffect()).isEqualTo(1);
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.emptyMap()))
                .isInstanceOf(FlowableException.class);

        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithoutValidationOnConfiguration(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService, TaskService taskService) {
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(false);
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormSideEffectProcess");
            assertThat(SideEffectExecutionListener.getSideEffect()).isEqualTo(1);
            SideEffectExecutionListener.reset();

            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

            FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

            taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator"));

            assertThat(SideEffectExecutionListener.getSideEffect()).isEqualTo(1);
        } finally {
            ((ProcessEngineConfigurationImpl) processEngineConfiguration).setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevel(RuntimeService runtimeService,
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
        assertThat(SideEffectExecutionListener.getSideEffect()).isEqualTo(1);
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

        taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator"));

        assertThat(SideEffectExecutionListener.getSideEffect()).isEqualTo(1);
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

        assertThatThrownBy(() -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "__COMPLETE",
                Collections.singletonMap("allowValidation", Boolean.TRUE),"oneTaskWithFormSideEffectProcess"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("validation failed");

        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
            "oneTaskWithFormSideEffectProcess",
            Collections.singletonMap("allowValidation", Boolean.TRUE)
        );
        SideEffectExecutionListener.reset();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        FormRepositoryService formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator")))
                .isInstanceOf(RuntimeException.class);

        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelBadExpression(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService,
        TaskService taskService, RepositoryService repositoryService) {

        repositoryService.createDeployment().
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

        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator")))
                .isInstanceOf(FlowableException.class)
                .hasMessage("Unknown property used in expression: ${BAD_EXPRESSION}");
        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithValidationOnModelLevelStringExpression(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService,
        TaskService taskService, RepositoryService repositoryService) {

        repositoryService.createDeployment().
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

        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");

        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithValidationOnMissingModelLevel(ProcessEngineConfiguration processEngineConfiguration, RuntimeService runtimeService,
        TaskService taskService, RepositoryService repositoryService) {

        repositoryService.createDeployment().
            addString("oneTaskWithFormKeySideEffectProcess.bpmn20.xml",
                ONE_TASK_PROCESS.
                    replace("flowable:formFieldValidation=\"START_EVENT_VALIDATION\"", "").
                    replace("flowable:formFieldValidation=\"USER_TASK_VALIDATION\"", "")
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

        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("initiator", "someInitiator")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");

        assertThat(SideEffectExecutionListener.getSideEffect()).isZero();
    }

}

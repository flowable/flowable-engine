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
package org.flowable.engine.test.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormDeploymentQuery;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;

/**
 * @author Filip Hrisafov
 */
@MockitoSettings
class ProcessWithFormTest extends PluggableFlowableTestCase {

    @Mock
    protected FormEngineConfigurationApi formEngineConfiguration;

    @Mock
    protected FormService formService;

    @Mock
    protected FormRepositoryService formRepositoryService;

    protected boolean originalFormFieldValidationEnabled;
    protected FormFieldHandler originalFormFieldHandler;

    @BeforeEach
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void setUp() {
        originalFormFieldValidationEnabled = processEngineConfiguration.isFormFieldValidationEnabled();
        originalFormFieldHandler = processEngineConfiguration.getFormFieldHandler();
        Map engineConfigurations = processEngineConfiguration.getEngineConfigurations();
        engineConfigurations.put(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration);
    }

    @AfterEach
    void tearDown() {
        processEngineConfiguration.setFormFieldValidationEnabled(originalFormFieldValidationEnabled);
        processEngineConfiguration.setFormFieldHandler(originalFormFieldHandler);
        processEngineConfiguration.getEngineConfigurations().remove(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void startProcessInstanceWithFormVariables() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").latestVersion().singleResult();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        FormInfo formInfo = new FormInfo();
        Map<String, Object> formVariables = Collections.singletonMap("intVar", 42);
        when(formService.getVariablesFromFormSubmission("theStart", "startEvent", null, processDefinition.getId(), 
                ScopeTypes.BPMN, formInfo, formVariables, "simple"))
                .thenReturn(Collections.singletonMap("otherIntVar", 150));

        String procId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(formVariables, formInfo, "simple")
                .start()
                .getId();

        assertThat(runtimeService.getVariables(procId))
                .containsOnly(
                        entry("otherIntVar", 150)
                );
    }

    @Test
    void startProcessInstanceWithInvalidFormVariables() {
        assertThatThrownBy(() -> runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .formVariables(Collections.singletonMap("intVar", "42"), null, "simple"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("formInfo is null");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskWithFormKeyProcess.bpmn20.xml")
    void testGetFormDefinitionsForProcessDefinition(
            @Mock FormDefinition formDefinition,
            @Mock(answer = Answers.RETURNS_SELF) FormDefinitionQuery formDefinitionQuery,
            @Mock(answer = Answers.RETURNS_SELF) FormDeploymentQuery formDeploymentQuery
    ) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .processDefinitionKey("oneTaskWithFormProcess")
                .singleResult();

        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);
        when(formRepositoryService.createFormDefinitionQuery()).thenReturn(formDefinitionQuery);
        when(formRepositoryService.createDeploymentQuery()).thenReturn(formDeploymentQuery);
        when(formDeploymentQuery.list()).thenReturn(Collections.emptyList());

        when(formDefinitionQuery.singleResult()).thenReturn(formDefinition);
        List<FormDefinition> definitions = repositoryService.getFormDefinitionsForProcessDefinition(processDefinition.getId());
        assertThat(definitions)
                .containsExactly(formDefinition);

        verify(formDefinitionQuery).formDefinitionKey("myFormKey");
        verify(formDefinitionQuery).latestVersion();
        verify(formDefinitionQuery).singleResult();
        verify(formDeploymentQuery).parentDeploymentId(processDefinition.getDeploymentId());
        verify(formDeploymentQuery).list();
        verifyNoMoreInteractions(formDefinitionQuery);
        verifyNoMoreInteractions(formDeploymentQuery);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/form/processWithStartForm.bpmn20.xml")
    void throwExceptionValidationOnStartProcess() {
        processEngineConfiguration.setFormFieldValidationEnabled(true);
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("emptyProcess")
                .singleResult();

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("test", processDefinition.getDeploymentId())).thenReturn(formInfo);

        Map<String, Object> startFormVariables = Collections.singletonMap("name", "nameValue");
        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields("start", "startEvent", null, processDefinition.getId(), 
                        ScopeTypes.BPMN, formInfo, startFormVariables);

        assertThatThrownBy(() -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", startFormVariables, "test"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/form/processWithStartForm.bpmn20.xml")
    void throwExceptionValidationOnStartProcessWithoutVariables() {
        processEngineConfiguration.setFormFieldValidationEnabled(true);
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("emptyProcess")
                .singleResult();

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("test", processDefinition.getDeploymentId())).thenReturn(formInfo);

        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields("start", "startEvent", null, processDefinition.getId(), 
                        ScopeTypes.BPMN, formInfo, Collections.emptyMap());

        assertThatThrownBy(() -> runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", Collections.emptyMap(), "test"))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/form/processWithStartForm.bpmn20.xml")
    void startProcessWithFormWithValidationOnConfiguration() {
        processEngineConfiguration.setFormFieldValidationEnabled(true);
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("emptyProcess")
                .singleResult();

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("test", processDefinition.getDeploymentId())).thenReturn(formInfo);

        Map<String, Object> startFormVariables = Collections.singletonMap("name", "nameValue");
        doNothing().when(formService)
                .validateFormFields("start", "startEvent", null, processDefinition.getId(), 
                        ScopeTypes.BPMN, formInfo, startFormVariables);

        when(formService.getVariablesFromFormSubmission("start", "startEvent", null, processDefinition.getId(), 
                ScopeTypes.BPMN, formInfo, startFormVariables, "COMPLETE"))
                .thenReturn(Collections.singletonMap("nameVar", "Test name"));

        ProcessInstance processInstance = runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", startFormVariables, "test");

        assertThat(processInstance.getProcessVariables())
                .containsOnly(entry("nameVar", "Test name"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/form/processWithStartForm.bpmn20.xml")
    void startProcessWithFormWithoutValidationOnConfiguration() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("emptyProcess")
                .singleResult();

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("test", processDefinition.getDeploymentId())).thenReturn(formInfo);

        Map<String, Object> startFormVariables = Collections.singletonMap("name", "nameValue");
        when(formService.getVariablesFromFormSubmission("start", "startEvent", null, processDefinition.getId(), 
                ScopeTypes.BPMN, formInfo, startFormVariables, "COMPLETE"))
                .thenReturn(Collections.singletonMap("nameVar", "Test name"));

        ProcessInstance processInstance = runtimeService.startProcessInstanceWithForm(processDefinition.getId(), "COMPLETE", startFormVariables, "test");

        assertThat(processInstance.getProcessVariables())
                .containsOnly(entry("nameVar", "Test name"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskWithFormKeyProcess.bpmn20.xml")
    void throwExceptionValidationOnCompleteTask() {
        processEngineConfiguration.setFormFieldValidationEnabled(true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "userTask", processInstance.getId(), processInstance.getProcessDefinitionId(), 
                        ScopeTypes.BPMN, formInfo, Collections.emptyMap());

        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", Collections.emptyMap()))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");

        Task taskAfterComplete = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterComplete).isNotNull();

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskWithFormKeyProcess.bpmn20.xml", tenantId = "flowable")
    void completeTaskWithoutValidationOnConfiguration(
            @Mock FormFieldHandler formFieldHandler
    ) {
        processEngineConfiguration.setFormFieldHandler(formFieldHandler);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskWithFormProcess", "flowable");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        Map<String, Object> completeVariables = Collections.singletonMap("completeVar", "test");
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "userTask", processInstance.getId(), processInstance.getProcessDefinitionId(), 
                ScopeTypes.BPMN, formInfo, completeVariables, "__COMPLETE"))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));

        taskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", completeVariables);

        Task taskAfterComplete = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterComplete).isNull();

        verify(formService).saveFormInstance(completeVariables, formInfo, task.getId(), processInstance.getId(), processInstance.getProcessDefinitionId(),
                "flowable", "__COMPLETE");

        verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, task.getId(), processInstance.getId(), null, null,
                Collections.singletonMap("completeVar2", "Testing"), "flowable");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/form/oneTaskWithFormKeyAndValidationProcess.bpmn20.xml", tenantId = "flowable")
    void completeTaskWithoutValidationOnModelLevel(
            @Mock FormFieldHandler formFieldHandler
    ) {
        processEngineConfiguration.setFormFieldValidationEnabled(true);
        processEngineConfiguration.setFormFieldHandler(formFieldHandler);
        ProcessInstance processInstance = runtimeService
                .createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskWithFormProcess")
                .tenantId("flowable")
                .variable("validateForm", false)
                .start();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        Map<String, Object> completeVariables = Collections.singletonMap("completeVar", "test");
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "userTask", processInstance.getId(), processInstance.getProcessDefinitionId(), 
                ScopeTypes.BPMN, formInfo, completeVariables, "__COMPLETE"))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));

        taskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", completeVariables);

        Task taskAfterComplete = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterComplete).isNull();

        verify(formService).saveFormInstance(completeVariables, formInfo, task.getId(), processInstance.getId(), processInstance.getProcessDefinitionId(),
                "flowable", "__COMPLETE");

        verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, task.getId(), processInstance.getId(), null, null,
                Collections.singletonMap("completeVar2", "Testing"), "flowable");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/form/oneTaskWithFormKeyAndValidationProcess.bpmn20.xml", tenantId = "flowable")
    void completeTaskWithoutValidationOnModelLevelEnabled(
            @Mock FormFieldHandler formFieldHandler
    ) {
        processEngineConfiguration.setFormFieldValidationEnabled(true);
        processEngineConfiguration.setFormFieldHandler(formFieldHandler);
        ProcessInstance processInstance = runtimeService
                .createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskWithFormProcess")
                .tenantId("flowable")
                .variable("validateForm", true)
                .start();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        Map<String, Object> completeVariables = Collections.singletonMap("completeVar", "test");
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "userTask", processInstance.getId(), processInstance.getProcessDefinitionId(), 
                ScopeTypes.BPMN, formInfo, completeVariables, "__COMPLETE"))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));
        doNothing().when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "userTask", processInstance.getId(), processInstance.getProcessDefinitionId(), 
                        ScopeTypes.BPMN, formInfo, completeVariables);

        taskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", completeVariables);

        Task taskAfterComplete = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterComplete).isNull();

        verify(formService).saveFormInstance(completeVariables, formInfo, task.getId(), processInstance.getId(), processInstance.getProcessDefinitionId(),
                "flowable", "__COMPLETE");

        verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, task.getId(), processInstance.getId(), null, null,
                Collections.singletonMap("completeVar2", "Testing"), "flowable");
    }

}

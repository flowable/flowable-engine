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
package org.flowable.cmmn.test.validate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.test.AbstractProcessEngineIntegrationTest;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * @author martin.grofcik
 */
public class CaseWithFormTest extends AbstractProcessEngineIntegrationTest {

    public static final String ONE_TASK_CASE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<definitions xmlns=\"http://www.omg.org/spec/CMMN/20151109/MODEL\"\n"
            + "             xmlns:flowable=\"http://flowable.org/cmmn\"\n"
            + "\n"
            + "             targetNamespace=\"http://flowable.org/cmmn\">\n"
            + "\n"
            + "\n"
            + "    <case id=\"oneTaskCaseWithForm\">\n"
            + "        <casePlanModel id=\"myPlanModel\" name=\"My CasePlanModel\" flowable:formKey=\"form1\" flowable:formFieldValidation=\"CASE_VALIDATE_VALUE\">\n"
            + "\n"
            + "            <planItem id=\"planItem1\" name=\"Task One\" definitionRef=\"theTask\" />\n"
            + "\n"
            + "            <humanTask id=\"theTask\" name=\"The Task\" flowable:formKey=\"form1\" flowable:formFieldValidation=\"TASK_VALIDATE_VALUE\">\n"
            + "                <extensionElements>\n"
            + "                    <flowable:taskListener event=\"create\" class=\"org.flowable.cmmn.test.validate.SideEffectTaskListener\"></flowable:taskListener>\n"
            + "                    <flowable:taskListener event=\"complete\" class=\"org.flowable.cmmn.test.validate.SideEffectTaskListener\"></flowable:taskListener>\n"
            + "                </extensionElements>\n"
            + "            </humanTask>\n"
            + "\n"
            + "        </casePlanModel>\n"
            + "    </case>\n"
            + "</definitions>\n";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    protected FormEngineConfigurationApi formEngineConfiguration;

    @Mock
    protected FormService formService;

    @Mock
    protected FormRepositoryService formRepositoryService;

    @Mock
    protected FormFieldHandler formFieldHandler;

    protected FormFieldHandler originalFormFieldHandler;

    @Before
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void initialize() {
        originalFormFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
        cmmnEngineConfiguration.setFormFieldHandler(formFieldHandler);
        cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();

        Map engineConfigurations = cmmnEngineConfiguration.getEngineConfigurations();
        engineConfigurations.put(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration);

        cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();

        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                        .replace("CASE_VALIDATE_VALUE", "true")
                        .replace("TASK_VALIDATE_VALUE", "true")
                )
                .deploy();
        SideEffectTaskListener.reset();
    }

    @After
    public void cleanDeployments() {
        cmmnEngineConfiguration.setFormFieldHandler(originalFormFieldHandler);
        cmmnEngineConfiguration.getEngineConfigurations().remove(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
        CmmnRepositoryService cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
        cmmnRepositoryService.createDeploymentQuery().list().forEach(
                cmmnDeployment -> cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true)
        );

        cmmnHistoryService.createHistoricTaskLogEntryQuery().list().forEach(
                historicTaskLogEntry -> cmmnHistoryService.deleteHistoricTaskLogEntry(historicTaskLogEntry.getLogNumber())
        );
    }

    @Test
    public void startCaseWithForm() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .singleResult();
        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("form1", caseDefinition.getDeploymentId()))
                .thenReturn(formInfo);

        doThrow(new RuntimeException("Validation failed"))
                .when(formService)
                .validateFormFields(null, "planModel", null, caseDefinition.getId(), ScopeTypes.CMMN, 
                        formInfo, Collections.singletonMap("variable", "VariableValue"));

        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startFormVariables(Collections.singletonMap("variable", "VariableValue"))
                .startWithForm())
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithFormAndCheckTaskLogEntries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        task.setName("newName");
        task.setPriority(0);
        cmmnTaskService.saveTask(task);
        cmmnTaskService.setAssignee(task.getId(), "newAssignee");
        cmmnTaskService.setOwner(task.getId(), "newOwner");
        cmmnTaskService.setDueDate(task.getId(), new Date());
        cmmnTaskService.addUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
        cmmnTaskService.addGroupIdentityLink(task.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnTaskService.deleteUserIdentityLink(task.getId(), "testUser", IdentityLinkType.PARTICIPANT);
        cmmnTaskService.deleteGroupIdentityLink(task.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        Map<String, Object> completeVariables = Collections.singletonMap("doNotThrowException", "");
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "humanTask", caseInstance.getId(), 
                caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, completeVariables, "__COMPLETE"))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));

        doNothing()
                .when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "humanTask", caseInstance.getId(), 
                        caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, completeVariables);

        cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", completeVariables);

        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count()).isEqualTo(11);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_CREATED.name()).count())
                .isEqualTo(1);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_NAME_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_PRIORITY_CHANGED.name())
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_ASSIGNEE_CHANGED.name())
                .count()).isEqualTo(1);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED.name()).count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED.name())
                .count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.name())
                .count()).isEqualTo(2);
        assertThat(
                cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_REMOVED.name())
                        .count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_COMPLETED.name()).count())
                .isEqualTo(1);
    }

    @Test
    public void startCaseAsyncWithForm() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .singleResult();
        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("form1", caseDefinition.getDeploymentId()))
                .thenReturn(formInfo);

        doThrow(new RuntimeException("Validation failed"))
                .when(formService)
                .validateFormFields(null, "planModel", null, caseDefinition.getId(), ScopeTypes.CMMN,
                        formInfo, Collections.singletonMap("variable", "VariableValue"));

        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startFormVariables(Collections.singletonMap("variable", "VariableValue"))
                .startWithForm())
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void startCaseWithFormWithDisabledValidationOnEngineLevel() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .singleResult();
        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("form1", caseDefinition.getDeploymentId()))
                .thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(null, "planModel", null, caseDefinition.getId(), ScopeTypes.CMMN,
                formInfo, Collections.singletonMap("variable", "VariableValue"), null))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));

        cmmnEngineConfiguration.setFormFieldValidationEnabled(false);
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCaseWithForm")
                    .startFormVariables(Collections.singletonMap("variable", "VariableValue"))
                    .startWithForm();

            assertThat(caseInstance).isNotNull();
            assertThat(caseInstance.getCaseVariables())
                    .containsOnly(entry("completeVar2", "Testing"));
            assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        } finally {
            cmmnEngineConfiguration.setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void startCaseWithFormWithoutVariables() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .singleResult();
        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("form1", caseDefinition.getDeploymentId()))
                .thenReturn(formInfo);
        doThrow(new RuntimeException("Validation failed"))
                .when(formService)
                .validateFormFields(null, "planModel", null, caseDefinition.getId(), ScopeTypes.CMMN, formInfo, null);

        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startWithForm())
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeCaseTaskWithFormWithValidationDisabledOnConfigLevel() {
        cmmnEngineConfiguration.setFormFieldValidationEnabled(false);
        try {
            when(formEngineConfiguration.getFormService()).thenReturn(formService);
            when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

            FormInfo formInfo = new FormInfo();
            Map<String, Object> completeVariables = Collections.singletonMap("var", "value");
            when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);

            CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCaseWithForm")
                    .start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
            assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
            SideEffectTaskListener.reset();
            
            when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "humanTask", caze.getId(), 
                    caze.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, completeVariables, "__COMPLETE"))
                    .thenReturn(Collections.singletonMap("var2", "value2"));

            cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", completeVariables);
            assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);

            verify(formService).saveFormInstanceWithScopeId(completeVariables, formInfo, task.getId(), caze.getId(), ScopeTypes.CMMN,
                    caze.getCaseDefinitionId(), caze.getTenantId(), "__COMPLETE");

            verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, task.getId(), null, caze.getId(), ScopeTypes.CMMN,
                    Collections.singletonMap("var2", "value2"), caze.getTenantId());
        } finally {
            cmmnEngineConfiguration.setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void completeCaseTaskWithForm() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();
        
        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "humanTask", caze.getId(), 
                        caze.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("var", "value"));

        assertThatThrownBy(
                () -> cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", Collections.singletonMap("var", "value")))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeCaseTaskWithFormWithoutVariables() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);
        
        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "humanTask", caze.getId(), 
                        caze.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null);

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevel() {
        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                        .replace("CASE_VALIDATE_VALUE", "false")
                        .replace("TASK_VALIDATE_VALUE", "false")
                )
                .deploy();
        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        Map<String, Object> completeVariables = Collections.singletonMap("completeVar", "test");
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "humanTask", caze.getId(), 
                caze.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, completeVariables, "__COMPLETE"))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));

        cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", completeVariables);
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);

        verify(formService).saveFormInstanceWithScopeId(completeVariables, formInfo, task.getId(), caze.getId(), ScopeTypes.CMMN,
                caze.getCaseDefinitionId(), caze.getTenantId(), "__COMPLETE");

        verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, task.getId(), null, caze.getId(), ScopeTypes.CMMN,
                Collections.singletonMap("completeVar2", "Testing"), caze.getTenantId());
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelExpression() {
        CmmnDeployment deployment = cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                        .replace("CASE_VALIDATE_VALUE", "${true}")
                        .replace("TASK_VALIDATE_VALUE", "${allowValidation}")
                )
                .deploy();

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);
        
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCaseWithForm").latestVersion().singleResult();

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelByKeyAndParentDeploymentId("form1", deployment.getParentDeploymentId()))
                .thenReturn(formInfo);
        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields(null, "planModel", null, caseDefinition.getId(), ScopeTypes.CMMN, 
                        formInfo, Collections.singletonMap("allowValidation", true));

        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startFormVariables(Collections.singletonMap("allowValidation", true))
                .startWithForm())
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();

        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .variables(Collections.singletonMap("allowValidation", true))
                .start();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();

        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        doThrow(new RuntimeException("validation failed for task"))
                .when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "humanTask", caze.getId(), 
                        caze.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null);

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("validation failed for task");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelBadExpression() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);

        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                        .replace("CASE_VALIDATE_VALUE", "true")
                        .replace("TASK_VALIDATE_VALUE", "${BAD_EXPRESSION}")
                )
                .deploy();

        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("Unknown property used in expression: ${BAD_EXPRESSION}");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithValidationOnModelLevelStringExpression() {
        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                        .replace("CASE_VALIDATE_VALUE", "true")
                        .replace("TASK_VALIDATE_VALUE", "${true}")
                )
                .deploy();

        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        
        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "humanTask", caze.getId(), 
                        caze.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null);

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithoutValidationOnMissingModelLevel() {
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);

        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
        .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                .replace("flowable:formFieldValidation=\"CASE_VALIDATE_VALUE\"", "")
                .replace("flowable:formFieldValidation=\"TASK_VALIDATE_VALUE\"", "")
        )
        .deploy();

        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();
        
        FormInfo formInfo = new FormInfo();
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        doThrow(new RuntimeException("validation failed"))
                .when(formService)
                .validateFormFields(task.getTaskDefinitionKey(), "humanTask", caze.getId(), 
                        caze.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null);

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), "formDefId", "__COMPLETE", null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("validation failed");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

}

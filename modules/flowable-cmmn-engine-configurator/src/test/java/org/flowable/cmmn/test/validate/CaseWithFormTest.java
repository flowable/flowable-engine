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

import java.util.Collections;
import java.util.Date;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.test.AbstractProcessEngineIntegrationTest;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FlowableFormValidationException;
import org.flowable.form.engine.FormEngines;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

    protected FormRepositoryService formRepositoryService;

    @Before
    public void initialize() {
        cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();

        formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        formRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/simple.form")
                .deploy();
        cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();

        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                        .replace("CASE_VALIDATE_VALUE", "true")
                        .replace("TASK_VALIDATE_VALUE", "true")
                )
                .deploy();
        SideEffectTaskListener.reset();
        TestValidationFormEngineConfigurator.ThrowExceptionOnValidationFormService.activate();
    }

    @After
    public void cleanDeployments() {
        TestValidationFormEngineConfigurator.ThrowExceptionOnValidationFormService.deactivate();
        formRepositoryService.createDeploymentQuery().list().forEach(
                formDeployment -> formRepositoryService.deleteDeployment(formDeployment.getId(), true)
        );
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
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startFormVariables(Collections.singletonMap("variable", "VariableValue"))
                .startWithForm())
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithFormAndCheckTaskLogEntries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey(task.getFormKey()).singleResult();

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
        cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("doNotThrowException", ""));

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
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startFormVariables(Collections.singletonMap("variable", "VariableValue"))
                .startWithForm())
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void startCaseWithFormWithDisabledValidationOnEngineLevel() {
        cmmnEngineConfiguration.setFormFieldValidationEnabled(false);
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCaseWithForm")
                    .startFormVariables(Collections.singletonMap("variable", "VariableValue"))
                    .startWithForm();

            assertThat(caseInstance).isNotNull();
            assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        } finally {
            cmmnEngineConfiguration.setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void startCaseWithFormWithoutVariables() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startWithForm())
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeCaseTaskWithFormWithValidationDisabledOnConfigLevel() {
        cmmnEngineConfiguration.setFormFieldValidationEnabled(false);
        try {
            CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCaseWithForm")
                    .start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
            assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
            SideEffectTaskListener.reset();

            cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("var", "value"));
            assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        } finally {
            cmmnEngineConfiguration.setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void completeCaseTaskWithForm() {
        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();
        assertThatThrownBy(
                () -> cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("var", "value")))
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeCaseTaskWithFormWithoutVariables() {
        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();
        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", null))
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
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
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", null);
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelExpression() {
        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment()
                .addString("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn", ONE_TASK_CASE
                        .replace("CASE_VALIDATE_VALUE", "${true}")
                        .replace("TASK_VALIDATE_VALUE", "${allowValidation}")
                )
                .deploy();

        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .startFormVariables(Collections.singletonMap("allowValidation", true))
                .startWithForm())
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();

        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .variables(Collections.singletonMap("allowValidation", true))
                .start();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", null))
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithoutValidationOnModelLevelBadExpression() {
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
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", null))
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

        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCaseWithForm")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", null))
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

    @Test
    public void completeTaskWithoutValidationOnMissingModelLevel() {
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
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect()).isEqualTo(1);
        SideEffectTaskListener.reset();

        assertThatThrownBy(() -> cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", null))
                .isInstanceOf(FlowableFormValidationException.class)
                .hasMessageStartingWith("Validation failed by default");
        assertThat(SideEffectTaskListener.getSideEffect()).isZero();
    }

}

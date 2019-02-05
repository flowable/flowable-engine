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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FlowableFormValidationException;
import org.flowable.form.engine.FormEngines;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskLogEntryType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author martin.grofcik
 */
@RunWith(CmmnTestRunner.class)
public class CaseWithFormTest {
    protected static CmmnEngineConfiguration cmmnEngineConfiguration;
    protected static ProcessEngine processEngine;

    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnTaskService cmmnTaskService;
    protected FormRepositoryService formRepositoryService;
    protected CmmnHistoryService cmmnHistoryService;

    @BeforeClass
    public static void bootProcessEngine() {
        if (processEngine == null) {
            processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.process-cmmn-form.cfg.xml").buildProcessEngine();
            cmmnEngineConfiguration = (CmmnEngineConfiguration) processEngine.getProcessEngineConfiguration()
                .getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
            CmmnTestRunner.setCmmnEngineConfiguration(cmmnEngineConfiguration);
        }
    }

    @Before
    public void initialize() {
        cmmnTaskService = cmmnEngineConfiguration.getCmmnTaskService();
        cmmnRuntimeService = cmmnEngineConfiguration.getCmmnRuntimeService();

        formRepositoryService = FormEngines.getDefaultFormEngine().getFormRepositoryService();
        formRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/simple.form").
            deploy();
        cmmnHistoryService = cmmnEngineConfiguration.getCmmnHistoryService();

        cmmnEngineConfiguration.getCmmnRepositoryService().createDeployment().
            addClasspathResource("org/flowable/cmmn/test/oneTasksCaseWithForm.cmmn").
            deploy();
        SideEffectTaskListener.reset();
    }

    @After
    public void cleanDeployments() {
        formRepositoryService.createDeploymentQuery().list().forEach(
            formDeployment -> formRepositoryService.deleteDeployment(formDeployment.getId())
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
        try {
            cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCaseWithForm").
                startFormVariables(Collections.singletonMap("variable", "VariableValue")).
                startWithForm();
            fail("Validation exception expected");
        } catch (FlowableFormValidationException e) {
            assertThat("Validation failed by default", is(e.getMessage()));
        }
        assertThat(SideEffectTaskListener.getSideEffect(), is(0));
    }

    @Test
    public void completeTaskWithFormAndCheckTaskLogEntries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCaseWithForm").
            start();

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

        assertEquals(11l, cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).count());
        assertEquals(1l, cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_CREATED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_NAME_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_PRIORITY_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_ASSIGNEE_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED.name()).count());
        assertEquals(2l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.name()).count());
        assertEquals(2l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_REMOVED.name()).count());
        assertEquals(1l,
            cmmnHistoryService.createHistoricTaskLogEntryQuery().taskId(task.getId()).type(HistoricTaskLogEntryType.USER_TASK_COMPLETED.name()).count());
    }

    @Test
    public void startCaseAsyncWithForm() {
        try {
            cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCaseWithForm").
                startFormVariables(Collections.singletonMap("variable", "VariableValue")).
                startWithForm();
            fail("Validation exception expected");
        } catch (FlowableFormValidationException e) {
            assertThat("Validation failed by default", is(e.getMessage()));
        }
        assertThat(SideEffectTaskListener.getSideEffect(), is(0));
    }

    @Test
    public void startCaseWithFormWithDisabledValidationOnEngineLevel() {
        cmmnEngineConfiguration.setFormFieldValidationEnabled(false);
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCaseWithForm").
                startFormVariables(Collections.singletonMap("variable", "VariableValue")).
                startWithForm();

            assertThat(caseInstance, is(notNullValue()));
            assertThat(SideEffectTaskListener.getSideEffect(), is(1));
        } finally {
            cmmnEngineConfiguration.setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void startCaseWithFormWithoutVariables() {
        try {
            cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCaseWithForm").
                startWithForm();
            fail("Validation exception expected");
        } catch (FlowableFormValidationException e) {
            assertThat("Validation failed by default", is(e.getMessage()));
        }
        assertThat(SideEffectTaskListener.getSideEffect(), is(0));
    }

    @Test
    public void completeCaseTaskWithFormWithValidationDisabledOnConfigLevel() {
        cmmnEngineConfiguration.setFormFieldValidationEnabled(false);
        try {
            CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCaseWithForm").
                start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
            FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
            assertThat(SideEffectTaskListener.getSideEffect(), is(1));
            SideEffectTaskListener.reset();

            cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("var", "value"));
            assertThat(SideEffectTaskListener.getSideEffect(), is(1));
        } finally {
            cmmnEngineConfiguration.setFormFieldValidationEnabled(true);
        }
    }

    @Test
    public void completeCaseTaskWithForm() {
        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCaseWithForm").
            start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect(), is(1));
        SideEffectTaskListener.reset();

        try {
            cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", Collections.singletonMap("var", "value"));
            fail("Validation exception expected");
        } catch (FlowableFormValidationException e) {
            assertThat("Validation failed by default", is(e.getMessage()));
        }
        assertThat(SideEffectTaskListener.getSideEffect(), is(0));
    }

    @Test
    public void completeCaseTaskWithFormWithoutVariables() {
        CaseInstance caze = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCaseWithForm").
            start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caze.getId()).singleResult();
        FormDefinition formDefinition = formRepositoryService.createFormDefinitionQuery().formDefinitionKey("form1").singleResult();
        assertThat(SideEffectTaskListener.getSideEffect(), is(1));
        SideEffectTaskListener.reset();

        try {
            cmmnTaskService.completeTaskWithForm(task.getId(), formDefinition.getId(), "__COMPLETE", null);
            fail("Validation exception expected");
        } catch (FlowableFormValidationException e) {
            assertThat("Validation failed by default", is(e.getMessage()));
        }
        assertThat(SideEffectTaskListener.getSideEffect(), is(0));
    }

}

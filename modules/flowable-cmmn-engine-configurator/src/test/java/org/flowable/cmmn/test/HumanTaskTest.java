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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
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
 * @author Filip Hrisafov
 */
public class HumanTaskTest extends AbstractProcessEngineIntegrationTest {

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
    public void setup() {
        super.setupServices();
        originalFormFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
        cmmnEngineConfiguration.setFormFieldHandler(formFieldHandler);
        Map engineConfigurations = cmmnEngineConfiguration.getEngineConfigurations();
        engineConfigurations.put(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration);
    }

    @After
    public void tearDown() {
        cmmnEngineConfiguration.setFormFieldHandler(originalFormFieldHandler);
        cmmnEngineConfiguration.getEngineConfigurations().remove(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn")
    public void completeHumanTaskWithoutVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        assertThat(caseInstance).isNotNull();

        Task caseTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseTask).isNotNull();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(caseTask.getTaskDefinitionKey(), "humanTask", caseInstance.getId(),
                caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null, "__COMPLETE"))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));
        doNothing().when(formService)
                .validateFormFields(caseTask.getTaskDefinitionKey(), "humanTask", caseInstance.getId(),
                        caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null);

        cmmnTaskService
                .completeTaskWithForm(caseTask.getId(), "formDefId",
                        "__COMPLETE", null);

        CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(dbCaseInstance).isNull();

        verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, caseTask.getId(), null, caseInstance.getId(), ScopeTypes.CMMN,
                Collections.singletonMap("completeVar2", "Testing"), caseInstance.getTenantId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn")
    public void completeHumanTaskWithBpmnEngine() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        assertThat(caseInstance).isNotNull();

        Task caseTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseTask).isNotNull();
        
        assertThatThrownBy(() -> processEngineTaskService.complete(caseTask.getId())).isInstanceOf(FlowableException.class)
            .hasMessageContaining("created by the cmmn engine");
    
        assertThatThrownBy(() -> processEngineTaskService.completeTaskWithForm(caseTask.getId(), null, null, null)).isInstanceOf(FlowableException.class)
            .hasMessageContaining("created by the cmmn engine");
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormRepositoryService()).thenReturn(formRepositoryService);
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formRepositoryService.getFormModelById("formDefId")).thenReturn(formInfo);
        when(formService.getVariablesFromFormSubmission(caseTask.getTaskDefinitionKey(), "humanTask", caseInstance.getId(),
                caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null, "__COMPLETE"))
                .thenReturn(Collections.singletonMap("completeVar2", "Testing"));
        doNothing().when(formService)
                .validateFormFields(caseTask.getTaskDefinitionKey(), "humanTask", caseInstance.getId(),
                        caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, null);

        cmmnTaskService.completeTaskWithForm(caseTask.getId(), "formDefId", "__COMPLETE", null);

        CaseInstance dbCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(dbCaseInstance).isNull();

        verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, caseTask.getId(), null, caseInstance.getId(), ScopeTypes.CMMN,
                Collections.singletonMap("completeVar2", "Testing"), caseInstance.getTenantId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn")
    public void queryTasksByDeploymentId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        assertThat(caseInstance).isNotNull();

        String caseDefinitionDeploymentId = caseInstance.getCaseDefinitionDeploymentId();
        assertThat(caseDefinitionDeploymentId).isNotNull();

        Task caseTask = cmmnTaskService.createTaskQuery()
                .cmmnDeploymentId(caseDefinitionDeploymentId)
                .singleResult();
        assertThat(caseTask).isNotNull();

        caseTask = cmmnTaskService.createTaskQuery()
                .cmmnDeploymentId(caseDefinitionDeploymentId)
                .deploymentId("invalid")
                .singleResult();
        assertThat(caseTask).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricTaskInstance historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .cmmnDeploymentId(caseDefinitionDeploymentId)
                    .singleResult();
            assertThat(historicTask).isNotNull();

            historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .cmmnDeploymentId(caseDefinitionDeploymentId)
                    .deploymentId("invalid")
                    .singleResult();
            assertThat(historicTask).isNull();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/CaseTaskTest.testCaseTask.cmmn")
    public void queryTasksByDeploymentIdsIn() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        assertThat(caseInstance).isNotNull();

        String caseDefinitionDeploymentId = caseInstance.getCaseDefinitionDeploymentId();
        assertThat(caseDefinitionDeploymentId).isNotNull();

        Task caseTask = cmmnTaskService.createTaskQuery()
                .cmmnDeploymentIdIn(Arrays.asList(caseDefinitionDeploymentId, "invalid"))
                .singleResult();
        assertThat(caseTask).isNotNull();

        caseTask = cmmnTaskService.createTaskQuery()
                .cmmnDeploymentIdIn(Arrays.asList(caseDefinitionDeploymentId, "invalid"))
                .deploymentId("invalid")
                .singleResult();
        assertThat(caseTask).isNull();

        caseTask = cmmnTaskService.createTaskQuery()
                .cmmnDeploymentIdIn(Arrays.asList(caseDefinitionDeploymentId, "invalid"))
                .deploymentIdIn(Arrays.asList("invalid1", "invalid2"))
                .singleResult();
        assertThat(caseTask).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricTaskInstance historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .cmmnDeploymentIdIn(Arrays.asList(caseDefinitionDeploymentId, "invalid"))
                    .singleResult();
            assertThat(historicTask).isNotNull();

            historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .cmmnDeploymentIdIn(Arrays.asList(caseDefinitionDeploymentId, "invalid"))
                    .deploymentId("invalid")
                    .singleResult();
            assertThat(historicTask).isNull();

            historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .cmmnDeploymentIdIn(Arrays.asList(caseDefinitionDeploymentId, "invalid"))
                    .deploymentIdIn(Arrays.asList("invalid1", "invalid2"))
                    .singleResult();
            assertThat(historicTask).isNull();
        }
    }

}

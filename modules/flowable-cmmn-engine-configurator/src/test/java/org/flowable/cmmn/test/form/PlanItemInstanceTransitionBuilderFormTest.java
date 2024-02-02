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
package org.flowable.cmmn.test.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.AbstractProcessEngineIntegrationTest;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.form.api.FormEngineConfigurationApi;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormService;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

/**
 * @author Filip Hrisafov
 */
public class PlanItemInstanceTransitionBuilderFormTest extends AbstractProcessEngineIntegrationTest {

    protected String processDeploymentId;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    protected FormEngineConfigurationApi formEngineConfiguration;

    @Mock
    protected FormService formService;

    @Mock
    protected FormFieldHandler formFieldHandler;

    protected FormFieldHandler originalFormFieldHandler;

    @Before
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setUp() {
        originalFormFieldHandler = cmmnEngineConfiguration.getFormFieldHandler();
        cmmnEngineConfiguration.setFormFieldHandler(formFieldHandler);
        Map engineConfigurations = cmmnEngineConfiguration.getEngineConfigurations();
        engineConfigurations.put(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG, formEngineConfiguration);
    }

    @After
    public void tearDown() {
        cmmnEngineConfiguration.setFormFieldHandler(originalFormFieldHandler);
        cmmnEngineConfiguration.getEngineConfigurations().remove(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
        if (processDeploymentId != null) {
            this.processEngineRepositoryService.deleteDeployment(processDeploymentId, true);
        }
    }

    @Test
    @CmmnDeployment
    public void testTriggerWithFormVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        Task task = cmmnTaskService.createTaskQuery().taskName("A").singleResult();
        assertThat(task).isNotNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "humantask", caseInstance.getId(), 
                caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("intVar", "50"), "testOutcome"))
                .thenReturn(Map.of(
                        "intVar", 50L,
                        "form_transitionForm_outcome", "testOutcome"
                ));

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .variable("var1", 123)
                .variable("var2", 456)
                .formVariables(Collections.singletonMap("intVar", "50"), formInfo, "testOutcome")
                .trigger();

        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", 123),
                        entry("var2", 456),
                        entry("intVar", 50L),
                        entry("form_transitionForm_outcome", "testOutcome")
                );

        verify(formFieldHandler).handleFormFieldsOnSubmit(formInfo, null, null, caseInstance.getId(), ScopeTypes.CMMN,
                Map.of("intVar", 50L, "form_transitionForm_outcome", "testOutcome"),
                caseInstance.getTenantId());
    }

    @Test
    @CmmnDeployment
    public void testEnableWithFormVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        Task task = cmmnTaskService.createTaskQuery().taskName("A").singleResult();
        assertThat(task).isNotNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission("taskB", "humantask", caseInstance.getId(), 
                caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("intVar", "100"), "test"))
                .thenReturn(Map.of(
                        "intVar", 100L,
                        "form_transitionForm_outcome", "test"
                ));

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .variable("var1", "hello")
                .variable("var2", "world")
                .formVariables(Collections.singletonMap("intVar", "100"), formInfo, "test")
                .enable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "hello"),
                        entry("var2", "world"),
                        entry("intVar", 100L),
                        entry("form_transitionForm_outcome", "test")
                );
    }

    @Test
    @CmmnDeployment
    public void testDisableWithFormVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        Task task = cmmnTaskService.createTaskQuery().taskName("A").singleResult();
        assertThat(task).isNotNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission("taskB", "humantask", caseInstance.getId(), 
                caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("intVar", "150"), "transition"))
                .thenReturn(Map.of(
                        "intVar", 150L,
                        "form_transitionForm_outcome", "transition"
                ));

        // Need to enable before disabling
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).enable();

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .variable("var1", "test")
                .variable("var2", "anotherTest")
                .formVariables(Collections.singletonMap("intVar", "150"), formInfo, "transition")
                .disable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.DISABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "test"),
                        entry("var2", "anotherTest"),
                        entry("intVar", 150L),
                        entry("form_transitionForm_outcome", "transition")
                );
    }

    @Test
    @CmmnDeployment
    public void testTerminateWithFormVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        Task task = cmmnTaskService.createTaskQuery().taskName("A").singleResult();
        assertThat(task).isNotNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission(task.getTaskDefinitionKey(), "humantask", caseInstance.getId(), 
                caseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("intVar", "15"), "anotherTest"))
                .thenReturn(Map.of(
                        "intVar", 15L,
                        "form_transitionForm_outcome", "anotherTest"
                ));

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .variable("var1", "hello")
                .formVariables(Collections.singletonMap("intVar", "15"), formInfo, "anotherTest")
                .terminate();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").includeEnded().singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).containsOnly(
                entry("var1", "hello"),
                entry("intVar", 15L),
                entry("form_transitionForm_outcome", "anotherTest")
        );
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/form/PlanItemInstanceTransitionBuilderFormTest.testStartCaseTask.cmmn",
            "org/flowable/cmmn/test/form/PlanItemInstanceTransitionBuilderFormTest.childCase.cmmn"
    })
    public void testStartCaseTaskWithFormVariables() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildCase").start();
        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult();
        assertThat(childCaseInstance).isNull();
        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId())).isEmpty();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission("caseTask1", "casetask", parentCaseInstance.getId(), 
                parentCaseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("intVar", "135"), "child"))
                .thenReturn(Map.of(
                        "intVar", 135L,
                        "form_transitionForm_outcome", "child"
                ));

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variable("someVar", "someValue")
                .variable("otherVar", 123)
                .formVariables(Collections.singletonMap("intVar", "135"), formInfo, "child")
                .start();

        childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult();
        assertThat(childCaseInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId()))
                .containsOnly(
                        entry("someVar", "someValue"),
                        entry("otherVar", 123),
                        entry("intVar", 135L),
                        entry("form_transitionForm_outcome", "child")
                );
        assertThat(cmmnRuntimeService.getVariables(childCaseInstance.getId())).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/form/PlanItemInstanceTransitionBuilderFormTest.testStartCaseTask.cmmn",
            "org/flowable/cmmn/test/form/PlanItemInstanceTransitionBuilderFormTest.childCase.cmmn"
    })
    public void testStartCaseTaskWithChildTaskFormVariables() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId())).isEmpty();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission("caseTask1", "caseTask", parentCaseInstance.getId(), 
                parentCaseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("intVar", "500"), "childForm"))
                .thenReturn(Map.of(
                        "intVar", 500L,
                        "form_transitionForm_outcome", "childForm"
                ));

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variable("parentVar1", 123)
                .childTaskVariable("childVar1", 1)
                .childTaskVariable("childVar2", 2)
                .childTaskFormVariables(Collections.singletonMap("intVar", "500"), formInfo, "childForm")
                .start();

        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult();
        assertThat(childCaseInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId()))
                .containsOnly(
                        entry("parentVar1", 123)
                );
        assertThat(cmmnRuntimeService.getVariables(childCaseInstance.getId()))
                .containsOnly(
                        entry("childVar1", 1),
                        entry("childVar2", 2),
                        entry("intVar", 500L),
                        entry("form_transitionForm_outcome", "childForm")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/form/PlanItemInstanceTransitionBuilderFormTest.testStartProcessTask.cmmn")
    public void testStartProcessTaskWithFormVariables() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildProcess").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();
        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId())).isEmpty();

        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission("theProcess", "processtask", parentCaseInstance.getId(), 
                parentCaseInstance.getCaseDefinitionId(), ScopeTypes.CMMN, formInfo, Collections.singletonMap("intVar", "77"), "childProcess"))
                .thenReturn(Map.of(
                        "intVar", 77L,
                        "form_transitionForm_outcome", "childProcess"
                ));

        processDeploymentId = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy()
                .getId();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variable("someVar", "someValue")
                .variable("otherVar", 123)
                .formVariables(Collections.singletonMap("intVar", "77"), formInfo, "childProcess")
                .start();

        ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(childProcessInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId()))
                .containsOnly(
                        entry("someVar", "someValue"),
                        entry("otherVar", 123),
                        entry("intVar", 77L),
                        entry("form_transitionForm_outcome", "childProcess")
                );
        assertThat(processEngineRuntimeService.getVariables(childProcessInstance.getId())).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/form/PlanItemInstanceTransitionBuilderFormTest.testStartProcessTask.cmmn")
    public void testStartProcessTaskWithChildTaskFormVariables() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildProcess").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId())).isEmpty();

        processDeploymentId = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy()
                .getId();
        
        ProcessDefinition processDefinition = processEngineRepositoryService.createProcessDefinitionQuery().deploymentId(processDeploymentId).singleResult();
        
        FormInfo formInfo = new FormInfo();
        when(formEngineConfiguration.getFormService()).thenReturn(formService);
        when(formService.getVariablesFromFormSubmission("theStart", "startEvent", null, 
                processDefinition.getId(), ScopeTypes.BPMN, formInfo, Collections.singletonMap("intVar", "42"), "childProcessForm"))
                .thenReturn(Map.of(
                        "intVar", 42L,
                        "form_transitionForm_outcome", "childProcessForm"
                ));

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variable("parentVar1", 123)
                .childTaskVariable("childVar1", 1)
                .childTaskVariable("childVar2", 2)
                .childTaskFormVariables(Collections.singletonMap("intVar", "42"), formInfo, "childProcessForm")
                .start();

        ProcessInstance childProcessInstance = processEngineRuntimeService.createProcessInstanceQuery().singleResult();
        assertThat(childProcessInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId()))
                .containsOnly(
                        entry("parentVar1", 123)
                );
        assertThat(processEngineRuntimeService.getVariables(childProcessInstance.getId()))
                .containsOnly(
                        entry("childVar1", 1),
                        entry("childVar2", 2),
                        entry("intVar", 42L),
                        entry("form_transitionForm_outcome", "childProcessForm")
                );
    }

}
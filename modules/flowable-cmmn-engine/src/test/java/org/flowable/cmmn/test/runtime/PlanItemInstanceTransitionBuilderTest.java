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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.form.api.FormInfo;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceTransitionBuilderTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testTrigger() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNotNull();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .trigger();

        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNull();
    }

    @Test
    @CmmnDeployment
    public void testInvalidTrigger() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).trigger())
            .isInstanceOf(FlowableIllegalStateException.class);
    }

    @Test
    @CmmnDeployment
    public void testInvalidTriggerEventListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance eventListenerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
            .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(eventListenerPlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(eventListenerPlanItemInstance.getId()).trigger())
            .isInstanceOf(FlowableIllegalStateException.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testTrigger.cmmn")
    public void testInvalidTriggerWithChildTaskInfo() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).childTaskVariable("testVar", "Test").trigger())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).childTaskVariables(Collections.singletonMap("testVar", "Test")).trigger())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).childTaskFormVariables(Collections.singletonMap("testVar", "Test"), new FormInfo(), "test").trigger())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child form variables can only be set when starting a plan item instance");
    }

    @Test
    @CmmnDeployment
    public void testTriggerWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNotNull();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .variables(CollectionUtil.map("var1", 123, "var2", 456))
                .trigger();

        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", 123),
                        entry("var2", 456)
                );
    }

    @Test
    @CmmnDeployment
    public void testTriggerWithVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNotNull();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .variable("var1", 123)
                .variable("var2", 456)
                .trigger();

        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", 123),
                        entry("var2", 456)
                );
    }

    @Test
    @CmmnDeployment
    public void testTriggerWithTransientVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNotNull();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .transientVariables(CollectionUtil.map("sentryVar", true, "otherVar", 123456))
                .trigger();

        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        // Transient variable should have triggered the sentry
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testTriggerWithTransientVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNotNull();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .transientVariable("sentryVar", true)
                .transientVariable("otherVar", 123456)
                .trigger();

        assertThat(cmmnTaskService.createTaskQuery().taskName("A").singleResult()).isNull();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        // Transient variable should have triggered the sentry
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testEnable() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).enable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);
    }

    @Test
    @CmmnDeployment
    public void testInvalidEnable() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).enable())
            .isInstanceOf(FlowableIllegalStateException.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testEnable.cmmn")
    public void testInvalidEnableWithChildTaskInfo() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskVariable("testVar", "Test").enable())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskVariables(Collections.singletonMap("testVar", "Test")).enable())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskFormVariables(Collections.singletonMap("testVar", "Test"), new FormInfo(), "test").enable())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child form variables can only be set when starting a plan item instance");
    }

    @Test
    @CmmnDeployment
    public void testEnableWithVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .variable("var1", "hello")
                .variable("var2", "world")
                .enable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "hello"),
                        entry("var2", "world")
                );
    }

    @Test
    @CmmnDeployment
    public void testEnableWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .variables(CollectionUtil.map("var1", "hello", "var2", "world"))
                .enable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "hello"),
                        entry("var2", "world")
                );
    }

    @Test
    @CmmnDeployment
    public void testEnableWithLocalVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .localVariable("localVariable", "hello")
                .variable("caseInstanceVar", "world")
                .enable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("caseInstanceVar", "world")
                );
    }

    @Test
    @CmmnDeployment
    public void testEnableWithTransientVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .transientVariable("sentryVar", true)
                .transientVariable("otherVar", 123)
                .enable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testEnableWithTransientVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .transientVariables(CollectionUtil.map("sentryVar", true, "otherVar", 123))
                .variable("persistentVar", 456)
                .enable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).containsOnly(
                entry("persistentVar", 456)
        );
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testDisable() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        // Need to enable before disabling
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).enable();

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).disable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.DISABLED);
    }

    @Test
    @CmmnDeployment
    public void testInvalidDisable() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).disable())
            .isInstanceOf(FlowableIllegalStateException.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testDisable.cmmn")
    public void testInvalidDisableWithChildTaskInfo() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskVariable("testVar", "Test").disable())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskVariables(Collections.singletonMap("testVar", "Test")).disable())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskFormVariables(Collections.singletonMap("testVar", "Test"), new FormInfo(), "test").disable())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child form variables can only be set when starting a plan item instance");
    }

    @Test
    @CmmnDeployment
    public void testDisableWithVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        // Need to enable before disabling
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).enable();

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .variable("var1", "test")
                .disable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.DISABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).containsOnly(
                entry("var1", "test")
        );
    }

    @Test
    @CmmnDeployment
    public void testDisableWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        // Need to enable before disabling
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).enable();

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .variables(CollectionUtil.map("var1", "test", "var2", "anotherTest"))
                .disable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.DISABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "test"),
                        entry("var2", "anotherTest")
                );
    }

    @Test
    @CmmnDeployment
    public void testDisableWithTransientVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        PlanItemInstance planItemInstanceD = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("D").singleResult();
        assertThat(planItemInstanceD.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Need to enable before disabling
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).enable();

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .variables(CollectionUtil.map("var1", "test", "var2", "anotherTest"))
                .transientVariable("sentryVar", true)
                .disable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.DISABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "test"),
                        entry("var2", "anotherTest")
                );

        planItemInstanceD = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("D").singleResult();
        assertThat(planItemInstanceD.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }

    @Test
    @CmmnDeployment
    public void testDisableWithTransientVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        PlanItemInstance planItemInstanceD = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("D").singleResult();
        assertThat(planItemInstanceD.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Need to enable before disabling
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId()).enable();

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceB.getId())
                .transientVariables(CollectionUtil.map("sentryVar", true, "otherVar", "test"))
                .disable();

        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        assertThat(planItemInstanceB.getState()).isEqualTo(PlanItemInstanceState.DISABLED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        planItemInstanceD = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("D").singleResult();
        assertThat(planItemInstanceD.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }

    @Test
    @CmmnDeployment
    public void testTerminate() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).terminate();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").includeEnded().singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testTerminate.cmmn")
    public void testInvalidTerminateWithChildTaskInfo() {
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskVariable("testVar", "Test").terminate())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskVariables(Collections.singletonMap("testVar", "Test")).terminate())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child task variables can only be set when starting a plan item instance");

        assertThatThrownBy(() ->  cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId()).childTaskFormVariables(Collections.singletonMap("testVar", "Test"), new FormInfo(), "test").terminate())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Child form variables can only be set when starting a plan item instance");
    }

    @Test
    @CmmnDeployment
    public void testTerminateWithVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .variable("var1", "hello")
                .variable("var2", "world")
                .terminate();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").includeEnded().singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "hello"),
                        entry("var2", "world")
                );
    }

    @Test
    @CmmnDeployment
    public void testTerminateWithVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .variables(CollectionUtil.map("var1", "hello"))
                .terminate();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").includeEnded().singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).containsOnly(
                entry("var1", "hello")
        );
    }

    @Test
    @CmmnDeployment
    public void testTerminateWithTransientVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNull();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .variable("var1", "hello")
                .variable("var2", "world")
                .transientVariable("sentryVar", true)
                .terminate();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").includeEnded().singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId()))
                .containsOnly(
                        entry("var1", "hello"),
                        entry("var2", "world")
                );

        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testTerminateWithTransientVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTransitionBuilder").start();
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNull();

        PlanItemInstance planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(5);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstanceA.getId())
                .transientVariables(CollectionUtil.map("sentryVar", true))
                .terminate();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(4);

        planItemInstanceA = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").includeEnded().singleResult();
        assertThat(planItemInstanceA.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();
        assertThat(cmmnTaskService.createTaskQuery().taskName("D").singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testStartCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.childcase.cmmn"
    })
    public void testStartCaseTask() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId()).start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNotNull();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testStartCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.childcase.cmmn"
    })
    public void testStartCaseTaskWithVariable() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variable("someVar", "someValue")
                .start();

        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult();
        assertThat(childCaseInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId()))
                .containsOnly(
                        entry("someVar", "someValue")
                );
        assertThat(cmmnRuntimeService.getVariables(childCaseInstance.getId())).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testStartCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.childcase.cmmn"
    })
    public void testStartCaseTaskWithVariables() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variables(CollectionUtil.map("someVar", "someValue", "otherVar", 123))
                .start();

        CaseInstance childCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult();
        assertThat(childCaseInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariables(parentCaseInstance.getId()))
                .containsOnly(
                        entry("someVar", "someValue"),
                        entry("otherVar", 123)
                );
        assertThat(cmmnRuntimeService.getVariables(childCaseInstance.getId())).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testStartCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.childcase.cmmn"
    })
    public void testStartCaseTaskWithChildTaskVariable() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variable("parentVar1", 123)
                .childTaskVariable("childVar1", 1)
                .childTaskVariable("childVar2", 2)
                .childTaskVariable("childVar3", 3)
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
                        entry("childVar3", 3)
                );
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.testStartCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/PlanItemInstanceTransitionBuilderTest.childcase.cmmn"
    })
    public void testStartCaseTaskWithChildTaskVariables() {
        CaseInstance parentCaseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testStartChildCase").start();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(parentCaseInstance.getId()).singleResult()).isNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(parentCaseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ENABLED);

        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                .variable("parentVar1", 123)
                .childTaskVariables(CollectionUtil.map("childVar1", 1, "childVar2", 2))
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
                        entry("childVar2", 2)
                );
    }

    @Test
    public void testInvalidArguments() {
        assertThatThrownBy(() -> cmmnRuntimeService.createPlanItemInstanceTransitionBuilder("dummy").childTaskFormVariables(Collections.emptyMap(), null, "test"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("formInfo is null");

        assertThatThrownBy(() -> cmmnRuntimeService.createPlanItemInstanceTransitionBuilder("dummy").formVariables(Collections.emptyMap(), null, "test"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("formInfo is null");
    }
}
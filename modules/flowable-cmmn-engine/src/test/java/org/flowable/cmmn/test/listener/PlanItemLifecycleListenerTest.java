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
package org.flowable.cmmn.test.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class PlanItemLifecycleListenerTest extends CustomCmmnConfigurationFlowableTestCase {

    @Override
    protected String getEngineName() {
        return this.getClass().getName();
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        Map<Object, Object> beans = new HashMap<>();
        cmmnEngineConfiguration.setBeans(beans);

        beans.put("delegateListener", new TestDelegateTaskListener());
    }

    @After
    public void tearDown() throws Exception {
        StateCapturingStaticPlanItemInstanceLifecycleListener.reset();
    }

    @Test
    @CmmnDeployment
    public void testListeners() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testPlanItemLifecycleListeners").start();

        assertVariable(caseInstance, "classDelegateVariable", "Hello World");
        assertVariable(caseInstance, "variableFromDelegateExpression", "Hello World from delegate expression");
        assertVariable(caseInstance, "expressionVar", "planItemIsActive");

        assertVariable(caseInstance, "stageActive",true);
        assertVariable(caseInstance, "milestoneReached", true);
    }

    @Test
    @CmmnDeployment
    public void testEventListenerPlanItemLifecycleListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testEventListenerPlanItemLifecycleListener").start();

        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "available")).isTrue();
        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "completed")).isNull();
        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "terminated")).isNull();

        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        assertThat(StateCapturingStaticPlanItemInstanceLifecycleListener.planItemsInstanceStates.get(userEventListenerInstance.getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "available"),
                        tuple("available", "completed")
                );

        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "available")).isTrue();
        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "completed")).isTrue();
        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "terminated")).isNull();

        // Same, but terminate the case
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testEventListenerPlanItemLifecycleListener").start();
        userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        assertThat(StateCapturingStaticPlanItemInstanceLifecycleListener.planItemsInstanceStates.get(userEventListenerInstance.getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "available"),
                        tuple("available", "terminated")
                );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat((Boolean) cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("available").singleResult().getValue()).isTrue();
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("completed").singleResult()).isNull();
            assertThat((Boolean) cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("terminate").singleResult().getValue()).isTrue();
        }
    }

    @Test
    @CmmnDeployment
    public void testEventListenerWithRepetitionPlanItemLifecycleListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testEventListenerPlanItemLifecycleListener")
                .start();

        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());

        assertThat(StateCapturingStaticPlanItemInstanceLifecycleListener.planItemsInstanceStates.get(userEventListenerInstance.getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "available")
                );

        PlanItemInstance completedEventListenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionId("userEventListener1")
                .includeEnded()
                .planItemInstanceStateCompleted()
                .singleResult();
        assertThat(completedEventListenerInstance).isNotNull();
        assertThat(StateCapturingStaticPlanItemInstanceLifecycleListener.planItemsInstanceStates.get(completedEventListenerInstance.getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "available"),
                        tuple("available", "completed")
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/PlanItemLifecycleListenerDelegateExpressionThrowsException.cmmn")
    public void testListenerWithDelegateExpressionThrowsFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners")
                .transientVariable("bean", new PlanItemInstanceLifecycleListener() {

                    @Override
                    public String getSourceState() {
                        return null;
                    }

                    @Override
                    public String getTargetState() {
                        return null;
                    }

                    @Override
                    public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
                        throw new FlowableIllegalArgumentException("Message from listener");
                    }
                });
        assertThatThrownBy(builder::start)
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Message from listener");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/PlanItemLifecycleListenerDelegateExpressionThrowsException.cmmn")
    public void testListenerWithDelegateExpressionThrowsNonFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners")
                .transientVariable("bean", new PlanItemInstanceLifecycleListener() {

                    @Override
                    public String getSourceState() {
                        return null;
                    }

                    @Override
                    public String getTargetState() {
                        return null;
                    }

                    @Override
                    public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
                        throw new RuntimeException("Message from listener");
                    }
                });
        assertThatThrownBy(builder::start)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasNoCause()
                .hasMessage("Message from listener");
    }

    @Test
    @CmmnDeployment
    public void testListenerWithClassThrowsFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners");
        assertThatThrownBy(builder::start)
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in listener");
    }

    @Test
    @CmmnDeployment
    public void testListenerWithClassThrowsNonFlowableException() {
        CaseInstanceBuilder builder = cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners");
        assertThatThrownBy(builder::start)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in listener");
    }

    @Test
    @CmmnDeployment
    public void testListenerInvokedWithRightStates() {
        StateCapturingPlanItemInstanceLifecycleListener listener = new StateCapturingPlanItemInstanceLifecycleListener();

        cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners")
                .transientVariable("testListener", listener)
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService
                .createPlanItemInstanceQuery()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(1);

        assertThat(listener.planItemsInstanceStates.get(planItemInstances.get(0).getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "available"),
                        tuple("available", "active"),
                        tuple("active", "completed")
                );
    }

    @Test
    @CmmnDeployment
    public void testListenerInvokedWithRightStatesForRepetitionsWithCollection() {
        StateCapturingPlanItemInstanceLifecycleListener listener = new StateCapturingPlanItemInstanceLifecycleListener();
        List<String> items = Arrays.asList("1", "2");

        cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners")
                .transientVariable("testListener", listener)
                .transientVariable("items", items)
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService
                .createPlanItemInstanceQuery()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(3);
        // There are 3 plan items.
        // One is terminated (the one that was waiting for repetition)
        // the other 2 are completed

        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.TERMINATED
                );

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateCompleted()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances)
                .hasSize(2)
                .allSatisfy(planItemInstance -> {
                    assertThat(listener.planItemsInstanceStates.get(planItemInstance.getId()))
                            .extracting(Pair::getLeft, Pair::getRight)
                            .containsExactly(
                                    tuple(null, "available"),
                                    tuple("available", "active"),
                                    tuple("active", "completed")
                            );
                });

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateTerminated()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances)
                .hasSize(1)
                .singleElement()
                .satisfies(planItemInstance -> {
                    assertThat(listener.planItemsInstanceStates.get(planItemInstance.getId()))
                            .extracting(Pair::getLeft, Pair::getRight)
                            .containsExactly(
                                    tuple(null, "available"),
                                    tuple("available", "terminated")
                            );
                });
    }

    @Test
    @CmmnDeployment
    public void testListenerInvokedWithRightStatesForRepetitionsWithCollectionAsync() {
        JsonNode items = cmmnEngineConfiguration.getObjectMapper()
                .createArrayNode()
                .add("1")
                .add("2");

        cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners")
                .variable("items", items)
                .start();

        waitForJobExecutorToProcessAllJobs();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService
                .createPlanItemInstanceQuery()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(3);
        // There are 3 plan items.
        // One is terminated (the one that was waiting for repetition)
        // the other 2 are completed

        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.TERMINATED
                );

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateCompleted()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances)
                .hasSize(2)
                .allSatisfy(planItemInstance -> {
                    assertThat(StateCapturingStaticPlanItemInstanceLifecycleListener.planItemsInstanceStates.get(planItemInstance.getId()))
                            .extracting(Pair::getLeft, Pair::getRight)
                            .containsExactly(
                                    tuple(null, "available"),
                                    tuple("available", "async-active"),
                                    tuple("async-active", "active"),
                                    tuple("active", "completed")
                            );
                });

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateTerminated()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances)
                .hasSize(1)
                .singleElement()
                .satisfies(planItemInstance -> {
                    assertThat(StateCapturingStaticPlanItemInstanceLifecycleListener.planItemsInstanceStates.get(planItemInstance.getId()))
                            .extracting(Pair::getLeft, Pair::getRight)
                            .containsExactly(
                                    tuple(null, "available"),
                                    tuple("available", "terminated")
                            );
                });
    }

    @Test
    @CmmnDeployment
    public void testListenerInvokedWithRightStatesForRepetitionsWithSentry() {
        StateCapturingPlanItemInstanceLifecycleListener listener = new StateCapturingPlanItemInstanceLifecycleListener();

        cmmnRuntimeService
                .createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemLifecycleListeners")
                .transientVariable("testListener", listener)
                .transientVariable("maxIterations", 2)
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService
                .createPlanItemInstanceQuery()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(3);
        // There are 3 plan items.
        // One is terminated (the one that was waiting for repetition)
        // the other 2 are completed

        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.COMPLETED,
                        PlanItemInstanceState.WAITING_FOR_REPETITION
                );

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateCompleted()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(2);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .variableValueEquals("repetitionCounter", 1)
                .planItemInstanceStateCompleted()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .singleResult();
        assertThat(listener.planItemsInstanceStates.get(planItemInstance.getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "available"),
                        tuple("available", "active"),
                        tuple("active", "completed")
                );

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .variableValueEquals("repetitionCounter", 2)
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .planItemInstanceStateCompleted()
                .singleResult();
        assertThat(listener.planItemsInstanceStates.get(planItemInstance.getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "wait_repetition"),
                        tuple("wait_repetition", "active"),
                        tuple("active", "completed")
                );

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceStateWaitingForRepetition()
                .planItemDefinitionId("serviceTask1")
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(1);

        planItemInstance = planItemInstances.get(0);
        assertThat(listener.planItemsInstanceStates.get(planItemInstance.getId()))
                .extracting(Pair::getLeft, Pair::getRight)
                .containsExactly(
                        tuple(null, "wait_repetition")
                );
    }


    private void assertVariable(CaseInstance caseInstance, String varName, boolean value) {
        Boolean variable = (Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), varName);
        assertThat(variable).isEqualTo(value);
    }

    private void assertVariable(CaseInstance caseInstance, String varName, String value) {
        String variable = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), varName);
        assertThat(variable).isEqualTo(value);
    }

    static class TestDelegateTaskListener implements PlanItemInstanceLifecycleListener {

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
            planItemInstance.setVariable("variableFromDelegateExpression", "Hello World from delegate expression");
        }

    }

    public static class ThrowingFlowableExceptionPlanItemInstanceLifecycleListener implements PlanItemInstanceLifecycleListener {

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
            throw new FlowableIllegalArgumentException("Illegal argument in listener");

        }
    }

    public static class ThrowingNonFlowableExceptionPlanItemInstanceLifecycleListener implements PlanItemInstanceLifecycleListener {

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
            throw new RuntimeException("Illegal argument in listener");

        }
    }

    public static class StateCapturingPlanItemInstanceLifecycleListener implements PlanItemInstanceLifecycleListener {

        private final Map<String, List<Pair<String, String>>> planItemsInstanceStates = new HashMap<>();

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
            planItemsInstanceStates.computeIfAbsent(planItemInstance.getId(), key -> new ArrayList<>()).add(Pair.of(oldState, newState));
        }
    }

    public static class StateCapturingStaticPlanItemInstanceLifecycleListener implements PlanItemInstanceLifecycleListener {

        private static final Map<String, List<Pair<String, String>>> planItemsInstanceStates = new HashMap<>();

        @Override
        public String getSourceState() {
            return null;
        }

        @Override
        public String getTargetState() {
            return null;
        }

        @Override
        public void stateChanged(DelegatePlanItemInstance planItemInstance, String oldState, String newState) {
            planItemsInstanceStates.computeIfAbsent(planItemInstance.getId(), key -> new ArrayList<>()).add(Pair.of(oldState, newState));
        }

        public static void reset() {
            planItemsInstanceStates.clear();
        }
    }


}

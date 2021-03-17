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

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

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

        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "available")).isTrue();
        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "completed")).isTrue();
        assertThat((Boolean) cmmnRuntimeService.getVariable(caseInstance.getId(), "terminated")).isNull();

        // Same, but terminate the case
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testEventListenerPlanItemLifecycleListener").start();
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat((Boolean) cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("available").singleResult().getValue()).isTrue();
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("completed").singleResult()).isNull();
            assertThat((Boolean) cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("terminate").singleResult().getValue()).isTrue();
        }
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

}

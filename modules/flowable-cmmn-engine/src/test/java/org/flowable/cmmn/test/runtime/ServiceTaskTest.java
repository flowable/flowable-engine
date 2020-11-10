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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.behavior.CmmnTriggerableActivityBehavior;
import org.flowable.cmmn.engine.impl.delegate.CmmnDelegateHelper;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.test.delegate.TestJavaDelegateThrowsException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ServiceTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testJavaServiceTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("test", "test2")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate")).isEqualTo("executed");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "test")).isEqualTo("test2");

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("javaDelegate")
                .singleResult().getValue()).isEqualTo("executed");
        }
    }

    @Test
    @CmmnDeployment
    public void testJavaServiceTaskFields() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("test", "test")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "testValue")).isEqualTo("test");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "testExpression")).isEqualTo(true);

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testValue")
                .singleResult().getValue()).isEqualTo("test");
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testExpression")
                .singleResult().getValue()).isEqualTo(true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testJavaServiceTaskThrowsException.cmmn")
    public void testJavaServiceTaskThrowsFlowableException() {
        TestJavaDelegateThrowsException.setExceptionSupplier(() -> new FlowableIllegalArgumentException("test exception"));
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");

        TestJavaDelegateThrowsException.resetExceptionSupplier();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testJavaServiceTaskThrowsException.cmmn")
    public void testJavaServiceTaskThrowsNonFlowableException() {
        TestJavaDelegateThrowsException.setExceptionSupplier(() -> new IllegalArgumentException("test exception"));
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start())
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");
        TestJavaDelegateThrowsException.resetExceptionSupplier();
    }

    @Test
    @CmmnDeployment
    public void testResultVariableName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("test", "test")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "beanResponse")).isEqualTo("hello test");

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("beanResponse")
                .singleResult().getValue()).isEqualTo("hello test");
        }
    }
    
    @Test
    @CmmnDeployment
    public void testDefinitionExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "resultVersion")).isEqualTo("1");
        assertThat(cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "resultVersion").getTypeName()).isEqualTo("string");

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("resultVersion")
                    .singleResult();
            
            assertThat(historicVariableInstance.getValue()).isEqualTo("1");
            assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo("string");
        }
    }

    @Test
    @CmmnDeployment
    public void testDelegateExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("test", "test2")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "javaDelegate")).isEqualTo("executed");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "test")).isEqualTo("test2");

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("javaDelegate")
                .singleResult().getValue()).isEqualTo("executed");
        }
    }

    @Test
    @CmmnDeployment
    public void testDelegateExpressionFields() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("test", "test")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "testValue")).isEqualTo("test");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "testExpression")).isEqualTo(true);

        // Triggering the task should start the child case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testValue")
                .singleResult().getValue()).isEqualTo("test");
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testExpression")
                .singleResult().getValue()).isEqualTo(true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testDelegateExpressionThrowsException.cmmn")
    public void testDelegateExpressionThrowsFlowableException() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("testDelegateBeanThrowsException", new PlanItemJavaDelegate() {

                    @Override
                    public void execute(DelegatePlanItemInstance planItemInstance) {
                        throw new FlowableIllegalArgumentException("test exception");
                    }
                })
                .start())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testDelegateExpressionThrowsException.cmmn")
    public void testDelegateExpressionThrowsNonFlowableException() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("testDelegateBeanThrowsException", new PlanItemJavaDelegate() {

                    @Override
                    public void execute(DelegatePlanItemInstance planItemInstance) {
                        throw new IllegalArgumentException("test exception");
                    }
                })
                .start())
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testDelegateExpressionCmmnTriggerableActivityBehaviorThrowsException.cmmn")
    public void testDelegateExpressionCmmnTriggerableActivityBehaviorThrowsFlowableExceptionOnTrigger() {
        CmmnTriggerableActivityBehavior triggerableActivityBehavior = new CmmnTriggerableActivityBehavior() {

            @Override
            public void trigger(DelegatePlanItemInstance planItemInstance) {
                throw new FlowableIllegalArgumentException("test exception");
            }

            @Override
            public void execute(DelegatePlanItemInstance delegatePlanItemInstance) {
                // Do nothing, wait state
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("testDelegateBeanThrowsException", triggerableActivityBehavior)
                .start();

        // The service task here acts like a wait state.
        // When the case instance is started, it will wait and be in state ACTIVE.

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        // When triggered, the plan item will complete
        assertThatThrownBy(() -> {
            cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                    .transientVariable("testDelegateBeanThrowsException", triggerableActivityBehavior)
                    .trigger();
        })
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testDelegateExpressionCmmnTriggerableActivityBehaviorThrowsException.cmmn")
    public void testDelegateExpressionCmmnTriggerableActivityBehaviorThrowsNonFlowableExceptionOnTrigger() {
        CmmnTriggerableActivityBehavior triggerableActivityBehavior = new CmmnTriggerableActivityBehavior() {

            @Override
            public void trigger(DelegatePlanItemInstance planItemInstance) {
                throw new IllegalArgumentException("test exception");
            }

            @Override
            public void execute(DelegatePlanItemInstance delegatePlanItemInstance) {
                // Do nothing, wait state
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("testDelegateBeanThrowsException", triggerableActivityBehavior)
                .start();

        // The service task here acts like a wait state.
        // When the case instance is started, it will wait and be in state ACTIVE.

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        // When triggered, the plan item will complete
        assertThatThrownBy(() -> {
            cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId())
                    .transientVariable("testDelegateBeanThrowsException", triggerableActivityBehavior)
                    .trigger();
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");
    }



    @Test
    @CmmnDeployment
    public void testGetCmmnModelWithDelegateHelper() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        assertThat(TestJavaDelegate01.cmmnModel).isNotNull();
        assertThat(TestJavaDelegate01.cmmnElement)
                .asInstanceOf(InstanceOfAssertFactories.type(PlanItem.class))
                .extracting(PlanItem::getName)
                .isEqualTo("Task One");
    }

    @Test
    @CmmnDeployment
    public void testCreateFieldExpressionWithDelegateHelper() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        Number variable = (Number) cmmnRuntimeService.getVariable(caseInstance.getId(), "delegateVariable");
        assertThat(variable).isEqualTo(2L);
    }

    @Test
    @CmmnDeployment
    public void testCreateFieldExpressionForLifecycleListenerWithDelegateHelper() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        Number variable = (Number) cmmnRuntimeService.getVariable(caseInstance.getId(), "listenerVar");
        assertThat(variable).isEqualTo(99L);
    }

    @Test
    @CmmnDeployment
    public void testStoreTransientVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("myCase")
            .start();

        Object transientResult = cmmnRuntimeService.getVariable(caseInstance.getId(), "transientResult");
        Object persistentResult = cmmnRuntimeService.getVariable(caseInstance.getId(), "persistentResult");

        assertThat(transientResult).isNull();
        assertThat(persistentResult).isEqualTo("Result is: test");
    }

    @Test
    @CmmnDeployment
    public void testCmmnTriggerableActivityBehavior() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("myCase")
            .start();

        // The service task here acts like a wait state.
        // When the case instance is started, it will wait and be in state ACTIVE.

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        // When triggered, the plan item will complete
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(planItemInstance.getId()).trigger();

        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).singleResult().getState())
                .isEqualTo(PlanItemInstanceState.COMPLETED);
        }

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testExpressionThrowsException.cmmn")
    public void testExpressionThrowsFlowableException() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("testBean", new Object() {

                    public void invoke() {
                        throw new FlowableIllegalArgumentException("test exception");
                    }
                })
                .start())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Error while evaluating expression: ${testBean.invoke()}")
                .getCause()
                .isInstanceOf(ELException.class)
                .getCause()
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testExpressionThrowsException.cmmn")
    public void testExpressionThrowsNonFlowableException() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("testBean", new Object() {

                    public void invoke() {
                        throw new IllegalArgumentException("test exception");
                    }
                })
                .start())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Error while evaluating expression: ${testBean.invoke()}")
                .getCause()
                .isInstanceOf(ELException.class)
                .getCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("test exception");
    }

    public static class TestJavaDelegate01 implements PlanItemJavaDelegate {

        public static CmmnModel cmmnModel;
        public static CmmnElement cmmnElement;

        @Override
        public void execute(DelegatePlanItemInstance planItemInstance) {
            cmmnModel = CmmnDelegateHelper.getCmmnModel(planItemInstance);
            cmmnElement = CmmnDelegateHelper.getCmmnElement(planItemInstance);
        }

    }

    public static class TestJavaDelegate02 implements PlanItemJavaDelegate {

        @Override
        public void execute(DelegatePlanItemInstance planItemInstance) {
            Expression delegateFieldExpression = CmmnDelegateHelper.getFieldExpression(planItemInstance, "delegateField");
            planItemInstance.setVariable("delegateVariable", delegateFieldExpression.getValue(planItemInstance));

        }

    }

    public static class TestJavaDelegate03 implements CmmnTriggerableActivityBehavior {

        @Override
        public void execute(DelegatePlanItemInstance delegatePlanItemInstance) {
            // Do nothing, wait state
        }

        @Override
        public void trigger(DelegatePlanItemInstance planItemInstance) {
            CommandContextUtil.getAgenda().planCompletePlanItemInstanceOperation((PlanItemInstanceEntity) planItemInstance);
        }

    }

    public static class TestLifecycleListener01 implements PlanItemInstanceLifecycleListener {

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
            Expression delegateField = CmmnDelegateHelper.getFieldExpression(planItemInstance, "delegateField");
            planItemInstance.setVariable("listenerVar", delegateField.getValue(planItemInstance));
        }

    }

}

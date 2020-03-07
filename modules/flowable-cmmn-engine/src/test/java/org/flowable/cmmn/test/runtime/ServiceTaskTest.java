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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.delegate.CmmnDelegateHelper;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.common.engine.api.delegate.Expression;
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
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);

        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("javaDelegate")
                .singleResult().getValue()).isEqualTo("executed");
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
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);

        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testValue")
                .singleResult().getValue()).isEqualTo("test");
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testExpression")
                .singleResult().getValue()).isEqualTo(true);
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
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);

        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("beanResponse")
                .singleResult().getValue()).isEqualTo("hello test");
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
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);

        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("javaDelegate")
                .singleResult().getValue()).isEqualTo("executed");
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
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);

        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testValue")
                .singleResult().getValue()).isEqualTo("test");
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .variableName("testExpression")
                .singleResult().getValue()).isEqualTo(true);
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

}

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

package org.flowable.cmmn.test.el;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class CmmnExpressionManagerTest extends FlowableCmmnTestCase {

    @Test
    public void testExpressionEvaluationWithoutCaseContext() {
        Expression expression = this.cmmnEngineConfiguration.getExpressionManager().createExpression("#{1 == 1}");
        Object value = expression.getValue(new NoExecutionVariableScope());
        assertThat(value).isEqualTo(true);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testIntJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", cmmnEngineConfiguration.getObjectMapper().createObjectNode().put("minIntVar", Integer.MIN_VALUE));
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        Expression expression = this.cmmnEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minIntVar}");
        Object value = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext ->
                expression.getValue(
                        (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables()
                                .singleResult()));

        assertThat(value).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testShortJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", cmmnEngineConfiguration.getObjectMapper().createObjectNode().put("minShortVar", Short.MIN_VALUE));
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        Expression expression = this.cmmnEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minShortVar}");
        Object value = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext ->
                expression.getValue(
                        (CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables()
                                .singleResult()));

        assertThat(value).isEqualTo((int) Short.MIN_VALUE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testFloatJsonVariableSerialization() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mapVariable", cmmnEngineConfiguration.getObjectMapper().createObjectNode().put("minFloatVar", Float.valueOf((float) -1.5)));
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(vars)
                .start();

        Expression expression = this.cmmnEngineConfiguration.getExpressionManager().createExpression("#{mapVariable.minFloatVar}");
        Object value = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext ->
                expression
                        .getValue((CaseInstanceEntity) cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables()
                                .singleResult()));

        assertThat(value).isEqualTo(-1.5d);
    }

    @Test
    @CmmnDeployment
    public void testMethodExpressions() {
        // Case contains 2 service tasks. one containing a method with no params, the other
        // contains a method with 2 params. When the case completes without exception, test passed.
        Map<String, Object> vars = new HashMap<>();
        vars.put("aString", "abcdefgh");
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("methodExpressionCase")
                .variables(vars)
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("methodExpressionCase").count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstanceAvailable() {
        Map<String, Object> vars = new HashMap<>();

        vars.put("myVar", new PlanItemInstanceTestVariable());
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testPlanItemInstanceAvailableCase")
                .variables(vars)
                .start();

        // Check of the testMethod has been called with the current planItemInstance
        String value = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "testVar");
        assertThat(value).isEqualTo("myValue");
    }

    @Test
    @CmmnDeployment
    public void testAuthenticatedUserIdAvailable() {
        try {
            // Setup authentication
            Authentication.setAuthenticatedUserId("filip");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testAuthenticatedUserIdAvailableCase")
                    .start();

            // Check if the variable that has been set in service-task is the
            // authenticated user
            String value = (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "theUser");
            assertThat(value).isEqualTo("filip");
        } finally {
            // Cleanup
            Authentication.setAuthenticatedUserId(null);
        }
    }
}

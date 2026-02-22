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
package org.flowable.dmn.engine.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Test;

/**
 * Tests that original exception messages are preserved through the DMN expression
 * evaluation chain when a custom bean throws an exception.
 */
class ExpressionBeanExceptionTest extends BaseFlowableDmnTest {

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/bean_expression_output_throwing_property.dmn")
    void beanPropertyAccessShouldPreserveOriginalExceptionMessage() {
        // When a bean property getter throws a custom exception during DMN output
        // expression evaluation, the original exception message should be preserved
        // in the audit container's exception message so callers can differentiate
        // between different types of errors (e.g. bad user input vs server error).
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", 1)
                .variable("testBean", new TestBean())
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getExceptionMessage()).isEqualTo("Threshold value cannot be null.");
        assertThat(result.getException())
                .isNotNull()
                .rootCause()
                .isInstanceOf(CustomBeanException.class)
                .hasMessage("Threshold value cannot be null.");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/bean_expression_output_throwing_method.dmn")
    void beanMethodInvocationShouldPreserveOriginalExceptionMessage() {
        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", 1)
                .variable("testBean", new TestBean())
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getExceptionMessage()).isEqualTo("Invalid input: test");
        assertThat(result.getException())
                .isNotNull()
                .rootCause()
                .isInstanceOf(CustomBeanException.class)
                .hasMessage("Invalid input: test");
    }

    public static class TestBean {

        public String getThrowingValue() {
            throw new CustomBeanException("Threshold value cannot be null.");
        }

        public String validate(String input) {
            throw new CustomBeanException("Invalid input: " + input);
        }
    }

    public static class CustomBeanException extends RuntimeException {

        public CustomBeanException(String message) {
            super(message);
        }
    }
}

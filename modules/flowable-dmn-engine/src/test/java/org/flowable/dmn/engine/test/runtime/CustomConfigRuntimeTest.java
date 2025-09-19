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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnConfigurationResource;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Yvo Swillens
 */
public class CustomConfigRuntimeTest {

    protected static final String ENGINE_CONFIG_1 = "custom1.flowable.dmn.cfg.xml";
    protected static final String ENGINE_CONFIG_2 = "custom2.flowable.dmn.cfg.xml";


    @Nested
    @DmnConfigurationResource(ENGINE_CONFIG_1)
    class CustomFunction extends BaseFlowableDmnTest {

        @Test
        @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/post_custom_expression_function_expression_1.dmn")
        public void postCustomExpressionFunction() {
            DmnDecisionService ruleService = dmnEngine.getDmnDecisionService();

            LocalDate localDate = LocalDate.parse("2015-09-18");
            Date input1 = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                    .decisionKey("decision")
                    .variable("input1", input1)
                    .executeWithSingleResult();

            assertThat(result).containsEntry("output1", "test2");
        }

    }

    @Nested
    @DmnConfigurationResource(ENGINE_CONFIG_2)
    class CustomFunctionMissingDefault extends BaseFlowableDmnTest {

        @Test
        @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/post_custom_expression_function_expression_1.dmn")
        public void customExpressionFunctionMissingDefaultFunction() {
            DmnDecisionService ruleService = dmnEngine.getDmnDecisionService();

            LocalDate localDate = LocalDate.parse("2015-09-18");
            Date input1 = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

            DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                    .decisionKey("decision")
                    .variable("input1", input1)
                    .executeWithAuditTrail();

            assertThat(result.getDecisionResult()).isEmpty();
            assertThat(result.isFailed()).isTrue();
        }

    }
}

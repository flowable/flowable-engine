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

import java.util.Map;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Yvo Swillens
 */
public class CustomConfigRuntimeTest {

    protected static final String ENGINE_CONFIG_1 = "custom1.flowable.dmn.cfg.xml";
    protected static final String ENGINE_CONFIG_2 = "custom2.flowable.dmn.cfg.xml";

    @Rule
    public FlowableDmnRule flowableRule1 = new FlowableDmnRule(ENGINE_CONFIG_1);

    @Rule
    public FlowableDmnRule flowableRule2 = new FlowableDmnRule(ENGINE_CONFIG_2);

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/post_custom_expression_function_expression_1.dmn")
    public void postCustomExpressionFunction() {

        DmnEngine dmnEngine = flowableRule1.getDmnEngine();
        DmnDecisionService ruleService = dmnEngine.getDmnDecisionService();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", localDate.toDate())
                .executeWithSingleResult();

        assertThat(result).containsEntry("output1", "test2");
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/deployment/post_custom_expression_function_expression_1.dmn")
    public void customExpressionFunctionMissingDefaultFunction() {

        DmnEngine dmnEngine = flowableRule2.getDmnEngine();
        DmnDecisionService ruleService = dmnEngine.getDmnDecisionService();

        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate localDate = dateTimeFormatter.parseLocalDate("2015-09-18");

        DecisionExecutionAuditContainer result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variable("input1", localDate.toDate())
                .executeWithAuditTrail();

        assertThat(result.getDecisionResult()).isEmpty();
        assertThat(result.isFailed()).isTrue();
    }
}

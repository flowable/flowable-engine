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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class CollectionsContainsTest {

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    public ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN.dmn")
    public void testContainsTrue() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd");

        List inputVariable1 = Arrays.asList("test1", "test2", "test3");
        List inputVariable2 = Arrays.asList("test1", "test2", "test3");
        List inputVariable3 = Arrays.asList("test1", "test2");
        List inputVariable4 = Arrays.asList(5L, 10L, 20L, 50L);
        List inputVariable5 = Arrays.asList("test3", "test5");
        List inputVariable6 = Arrays.asList("tes,t6", "te,st5");
        List inputVariable7 = Arrays.asList(BigInteger.valueOf(100), BigInteger.valueOf(101));
        List inputVariable8 = Arrays.asList("100", "101");
        LocalDate date1 = dtf.parseLocalDate("2021-02-02");

        ArrayNode arrayNode1 = objectMapper.createArrayNode().add("test1").add("test2").add("test3");
        ArrayNode arrayNode2 = objectMapper.createArrayNode().add(5L).add(10L).add(20L).add(50L);
        ArrayNode arrayNode3 = objectMapper.createArrayNode().add(5.5D).add(10.5D).add(20.5D).add(50.5D);
        ArrayNode arrayNode4 = objectMapper.createArrayNode().add(5.5555F).add(10.5555F).add(20.5555F).add(50.5555F);
        ArrayNode arrayNode5 = objectMapper.createArrayNode().add(5.5555F).add(10.5555F);
        ObjectNode nestedArrayNode1 = objectMapper.createObjectNode().putPOJO("property1", arrayNode1);

        processVariablesInput.put("collection1", inputVariable1);
        processVariablesInput.put("collection2", inputVariable2);
        processVariablesInput.put("collection3", inputVariable3);
        processVariablesInput.put("collection4", inputVariable4);
        processVariablesInput.put("collection5", inputVariable5);
        processVariablesInput.put("collection6", inputVariable6);
        processVariablesInput.put("collection7", inputVariable7);
        processVariablesInput.put("collection8", inputVariable8);
        processVariablesInput.put("arrayNode1", arrayNode1);
        processVariablesInput.put("arrayNode2", arrayNode2);
        processVariablesInput.put("arrayNode3", arrayNode3);
        processVariablesInput.put("arrayNode4", arrayNode4);
        processVariablesInput.put("arrayNode5", arrayNode5);
        processVariablesInput.put("nestedArrayNode1", nestedArrayNode1);
        processVariablesInput.put("bigInteger1", BigInteger.valueOf(101));
        processVariablesInput.put("date1", date1.toDate());

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getRuleExecutions().get(1).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(2).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(3).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(4).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(7).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(8).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(11).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(13).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(14).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(15).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(16).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(17).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(18).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(19).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(20).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(21).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(22).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(24).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(25).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(26).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(27).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(28).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(29).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(30).isValid()).isTrue();
        assertThat(result.getRuleExecutions().get(31).isValid()).isTrue();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN.dmn")
    public void testContainsFalse() {
        Map<String, Object> processVariablesInput = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd");

        List inputVariable1 = Arrays.asList("test1", "test2", "test3");
        List inputVariable2 = Arrays.asList("test1", "test2", "test3");
        List inputVariable3 = Arrays.asList("test1", "test2");
        List inputVariable4 = Arrays.asList(5L, 10L, 20L, 50L);
        List inputVariable5 = Arrays.asList("test3", "test5");
        List inputVariable6 = Arrays.asList("tes,t6", "te,st5");
        List inputVariable7 = Arrays.asList(BigInteger.valueOf(100), BigInteger.valueOf(101));
        List inputVariable8 = Arrays.asList("100", "101");

        ArrayNode arrayNode1 = objectMapper.createArrayNode().add("test1").add("test2").add("test3");
        ArrayNode arrayNode2 = objectMapper.createArrayNode().add(5L).add(10L).add(20L).add(50L);
        ArrayNode arrayNode3 = objectMapper.createArrayNode().add(5.5D).add(10.5D).add(20.5D).add(50.5D);
        ArrayNode arrayNode4 = objectMapper.createArrayNode().add(5.5555F).add(10.5555F).add(20.5555F).add(50.5555F);
        ArrayNode arrayNode5 = objectMapper.createArrayNode().add(5.5555F).add(10.5555F);
        ObjectNode nestedArrayNode1 = objectMapper.createObjectNode().putPOJO("property1", arrayNode1);
        LocalDate date1 = dtf.parseLocalDate("2021-02-02");

        processVariablesInput.put("collection1", inputVariable1);
        processVariablesInput.put("collection2", inputVariable2);
        processVariablesInput.put("collection3", inputVariable3);
        processVariablesInput.put("collection4", inputVariable4);
        processVariablesInput.put("collection5", inputVariable5);
        processVariablesInput.put("collection6", inputVariable6);
        processVariablesInput.put("collection7", inputVariable7);
        processVariablesInput.put("collection8", inputVariable8);
        processVariablesInput.put("arrayNode1", arrayNode1);
        processVariablesInput.put("arrayNode2", arrayNode2);
        processVariablesInput.put("arrayNode3", arrayNode3);
        processVariablesInput.put("arrayNode4", arrayNode4);
        processVariablesInput.put("arrayNode5", arrayNode5);
        processVariablesInput.put("nestedArrayNode1", nestedArrayNode1);
        processVariablesInput.put("bigInteger1", BigInteger.valueOf(101));
        processVariablesInput.put("date1", date1);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();
        assertThat(result.getRuleExecutions().get(5).isValid()).isFalse();
        assertThat(result.getRuleExecutions().get(6).isValid()).isFalse();
        assertThat(result.getRuleExecutions().get(9).isValid()).isFalse();
        assertThat(result.getRuleExecutions().get(10).isValid()).isFalse();
        assertThat(result.getRuleExecutions().get(12).isValid()).isFalse();
    }

    @Test
    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/contains_IN_types.dmn")
    public void testContainsTypeCheck() {
        Map<String, Object> processVariablesInput = new HashMap<>();

        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate date1 = dtf.parseLocalDate("2017-12-25");
        LocalDate date2 = dtf.parseLocalDate("2018-12-25");

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.LocalDate date3 = java.time.LocalDate.parse("2017-12-25", formatter);

        List<String> collectionString = Arrays.asList("test1", "test2", "test3");
        List<Boolean> collectionBoolean = Arrays.asList(Boolean.TRUE, Boolean.FALSE);
        List<Date> collectionDate = Arrays.asList(date1.toDate(), date2.toDate());
        List<LocalDate> collectionLocalDate = Arrays.asList(date1, date2);
        List<Integer> collectionInteger = Arrays.asList(5, 10, 20, 50);
        List<Long> collectionLong = Arrays.asList(5L, 10L, 20L, 50L);
        List<Float> collectionFloat = Arrays.asList(5F, 10F, 20F, 50F);
        List<Double> collectionDouble = Arrays.asList(5D, 10D, 20D, 50D);

        processVariablesInput.put("collectionString", collectionString);
        processVariablesInput.put("collectionBoolean", collectionBoolean);
        processVariablesInput.put("collectionDate", collectionDate);
        processVariablesInput.put("collectionLocalDate", collectionLocalDate);
        processVariablesInput.put("collectionInteger", collectionInteger);
        processVariablesInput.put("collectionLong", collectionLong);
        processVariablesInput.put("collectionFloat", collectionFloat);
        processVariablesInput.put("collectionDouble", collectionDouble);
        processVariablesInput.put("stringLocalDate", "2017-12-25");
        processVariablesInput.put("jodaLocalDate", date1);
        processVariablesInput.put("javaLocalDate", date3);

        DmnEngine dmnEngine = flowableDmnRule.getDmnEngine();
        DmnDecisionService dmnRuleService = dmnEngine.getDmnDecisionService();

        DecisionExecutionAuditContainer result = dmnRuleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .variables(processVariablesInput)
                .executeWithAuditTrail();

        assertThat(result.isFailed()).isFalse();

        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(0).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(1).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(2).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(3).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(4).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(5).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(6).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(1).getConditionResults().get(7).getResult()).isEqualTo(true);

        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(0).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(1).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(2).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(3).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(4).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(5).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(6).getResult()).isEqualTo(true);
        assertThat(result.getRuleExecutions().get(2).getConditionResults().get(7).getResult()).isEqualTo(true);
    }
}

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
package org.flowable.dmn.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.api.DecisionServiceExecutionAuditContainer;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Saratz
 */
class HistoryDisabledTest extends BaseFlowableDmnTest {

    @Test
    @DmnDeployment
    public void testExecuteDecision() {
        List<Map<String, Object>> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .disableHistory()
                .executeDecision();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsExactly(
                entry("outputVariable1", "gt 10"),
                entry("outputVariable2", "result2")
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecuteDecisionService() {
        Map<String, List<Map<String, Object>>> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("expandedDecisionService")
                .variable("input1", "test1")
                .variable("input2", "test2")
                .variable("input3", "test3")
                .variable("input4", "test4")
                .disableHistory()
                .executeDecisionService();

        assertThat(result).hasSize(2);
        List<Map<String, Object>> decision1 = result.get("decision1");
        assertThat(decision1).hasSize(3);
        IntStream.range(0, 3).forEach(i ->
                assertThat(decision1.get(i)).containsExactly(entry("output1", "NOT EMPTY " + (i + 1)))
        );

        List<Map<String, Object>> decision2 = result.get("decision2");
        assertThat(decision2).hasSize(3);
        IntStream.range(0, 3).forEach(i ->
                assertThat(decision2.get(i)).containsExactly(entry("output2", "NOT EMPTY " + (i + 1)))
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecuteWithSingleResult() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .disableHistory()
                .executeWithSingleResult();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(
                entry("outputVariable1", "gt 10"),
                entry("outputVariable2", "result2")
        );
        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecuteDecisionWithSingleResult() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .disableHistory()
                .executeDecisionWithSingleResult();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(
                entry("outputVariable1", "gt 10"),
                entry("outputVariable2", "result2")
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecuteDecisionServiceWithSingleResult() {
        Map<String, Object> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("expandedDecisionService")
                .variable("input1", "test1")
                .variable("input2", "test2")
                .variable("input3", "test3")
                .variable("input4", "test4")
                .disableHistory()
                .executeDecisionServiceWithSingleResult();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(
                entry("output1", "NOT EMPTY 1"),
                entry("output2", "NOT EMPTY 1")
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecuteWithAuditTrail() {
        DecisionExecutionAuditContainer auditContainer = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .disableHistory()
                .executeWithAuditTrail();

        assertThat(auditContainer).isNotNull();
        assertThat(auditContainer.getStartTime()).isNotNull();
        assertThat(auditContainer.getEndTime()).isNotNull();
        assertThat(auditContainer.getInputVariables().get("inputVariable1")).isEqualTo(11);
        assertThat(auditContainer.getDecisionResult()).hasSize(1);
        assertThat(auditContainer.getDecisionResult().get(0)).containsExactly(
                entry("outputVariable1", "gt 10"),
                entry("outputVariable2", "result2")
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecuteDecisionWithAuditTrail() {
        DecisionExecutionAuditContainer auditContainer = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .disableHistory()
                .executeDecisionWithAuditTrail();

        assertThat(auditContainer).isNotNull();
        assertThat(auditContainer.getStartTime()).isNotNull();
        assertThat(auditContainer.getEndTime()).isNotNull();
        assertThat(auditContainer.getInputVariables().get("inputVariable1")).isEqualTo(11);
        assertThat(auditContainer.getDecisionResult()).hasSize(1);
        assertThat(auditContainer.getDecisionResult().get(0)).containsExactly(
                entry("outputVariable1", "gt 10"),
                entry("outputVariable2", "result2")
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecuteDecisionServiceWithAuditTrail() {
        DecisionServiceExecutionAuditContainer auditContainer = ruleService.createExecuteDecisionBuilder()
                .decisionKey("expandedDecisionService")
                .variable("input1", "test1")
                .variable("input2", "test2")
                .variable("input3", "test3")
                .variable("input4", "test4")
                .disableHistory()
                .executeDecisionServiceWithAuditTrail();

        assertThat(auditContainer).isNotNull();
        assertThat(auditContainer.getStartTime()).isNotNull();
        assertThat(auditContainer.getEndTime()).isNotNull();
        assertThat(auditContainer.getInputVariables()).containsExactlyInAnyOrderEntriesOf(
                Map.of("input1", "test1",
                        "input2", "test2",
                        "input3", "test3",
                        "input4", "test4")
        );

        Map<String, List<Map<String, Object>>> result = auditContainer.getDecisionServiceResult();
        assertThat(result).hasSize(2);
        assertThat(result).hasSize(2);
        List<Map<String, Object>> decision1 = result.get("decision1");
        assertThat(decision1).hasSize(3);
        IntStream.range(0, 3).forEach(i ->
                assertThat(decision1.get(i)).containsExactly(entry("output1", "NOT EMPTY " + (i + 1)))
        );

        List<Map<String, Object>> decision2 = result.get("decision2");
        assertThat(decision2).hasSize(3);
        IntStream.range(0, 3).forEach(i ->
                assertThat(decision2.get(i)).containsExactly(entry("output2", "NOT EMPTY " + (i + 1)))
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }

    @Test
    @DmnDeployment
    public void testExecute() {
        List<Map<String, Object>> result = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .disableHistory()
                .execute();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsExactly(
                entry("outputVariable1", "gt 10"),
                entry("outputVariable2", "result2")
        );

        assertThat(historyService.createHistoricDecisionExecutionQuery().count()).isEqualTo(0);
    }
}
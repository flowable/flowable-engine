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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.impl.test.PluggableFlowableDmnTestCase;
import org.flowable.dmn.engine.test.DmnDeployment;

import com.fasterxml.jackson.databind.JsonNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Tijs Rademakers
 */
public class HistoryTest extends PluggableFlowableDmnTestCase {

    @DmnDeployment
    public void testFirstHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();

        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        assertThat(decisionExecution.getDecisionDefinitionId()).isNotNull();
        assertThat(decisionExecution.getDeploymentId()).isNotNull();
        assertThat(decisionExecution.isFailed()).isFalse();
        assertThat(decisionExecution.getStartTime()).isNotNull();
        assertThat(decisionExecution.getEndTime()).isNotNull();
        assertThat(decisionExecution.getExecutionJson()).isNotNull();

        JsonNode executionNode = dmnEngineConfiguration.getObjectMapper().readTree(decisionExecution.getExecutionJson());
        assertThatJson(executionNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    decisionKey: 'decision1',"
                        + "    decisionName: 'Full Decision',"
                        + "    hitPolicy: 'FIRST'"
                        + "}");

        JsonNode inputVariables = executionNode.get("inputVariables");
        assertThat(inputVariables.isObject()).isTrue();
        assertThatJson(inputVariables)
                .isEqualTo("{"
                        + "    inputVariable1: 11"
                        + "}");

        JsonNode inputVariableTypes = executionNode.get("inputVariableTypes");
        assertThat(inputVariableTypes.isObject()).isTrue();
        assertThatJson(inputVariableTypes)
                .isEqualTo("{"
                        + "    inputVariable1: 'number'"
                        + "}");

        JsonNode decisionResultArray = executionNode.get("decisionResult");
        assertThat(decisionResultArray.isArray()).isTrue();
        assertThatJson(decisionResultArray)
                .isEqualTo("["
                        + "  {"
                        + "    outputVariable1: 'gt 10',"
                        + "    outputVariable2: 'result2'"
                        + "  }"
                        + "]");

        JsonNode decisionResultTypes = executionNode.get("decisionResultTypes");
        assertThat(decisionResultTypes.isObject()).isTrue();
        assertThatJson(decisionResultTypes)
                .isEqualTo("{"
                        + "    outputVariable1: 'string',"
                        + "    outputVariable2: 'string'"
                        + "}");

        JsonNode ruleExecutions = executionNode.get("ruleExecutions");

        assertThat(ruleExecutions.isObject()).isTrue();
        assertThatJson(ruleExecutions)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    1: {"
                        + "         valid: false"
                        + "        },"
                        + "    2: {"
                        + "         valid: true"
                        + "       }"
                        + "}");
    }
    
    @DmnDeployment
    public void testOutputOrderHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .executeWithAuditTrail();

        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        assertThat(decisionExecution.getDecisionDefinitionId()).isNotNull();
        assertThat(decisionExecution.getDeploymentId()).isNotNull();
        assertThat(decisionExecution.isFailed()).isFalse();
        assertThat(decisionExecution.getStartTime()).isNotNull();
        assertThat(decisionExecution.getEndTime()).isNotNull();
        assertThat(decisionExecution.getExecutionJson()).isNotNull();

        JsonNode executionNode = dmnEngineConfiguration.getObjectMapper().readTree(decisionExecution.getExecutionJson());
        assertThatJson(executionNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    decisionKey: 'decision1',"
                        + "    decisionName: 'Full Decision',"
                        + "    hitPolicy: 'OUTPUT ORDER'"
                        + "}");

        JsonNode inputVariables = executionNode.get("inputVariables");
        assertThat(inputVariables.isObject()).isTrue();
        assertThatJson(inputVariables)
                .isEqualTo("{"
                        + "    inputVariable1: 5"
                        + "}");

        JsonNode inputVariableTypes = executionNode.get("inputVariableTypes");
        assertThat(inputVariableTypes.isObject()).isTrue();
        assertThatJson(inputVariableTypes)
                .isEqualTo("{"
                        + "    inputVariable1: 'number'"
                        + "}");

        JsonNode decisionResultArray = executionNode.get("decisionResult");
        assertThat(decisionResultArray.isArray()).isTrue();
        assertThatJson(decisionResultArray)
                .isEqualTo("["
                        + "  {"
                        + "    outputVariable1: 'OUTPUT2'"
                        + "  },"
                        + "  {"
                        + "    outputVariable1: 'OUTPUT3'"
                        + "  },"
                        + "  {"
                        + "    outputVariable1: 'OUTPUT1'"
                        + "  }"
                        + "]");

        JsonNode decisionResultTypes = executionNode.get("decisionResultTypes");
        assertThat(decisionResultTypes.isObject()).isTrue();
        assertThatJson(decisionResultTypes)
                .isEqualTo("{"
                        + "    outputVariable1: 'string'"
                        + "}");

        JsonNode ruleExecutions = executionNode.get("ruleExecutions");
        assertThat(ruleExecutions.isObject()).isTrue();
        assertThatJson(ruleExecutions)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    1: {"
                        + "         valid: true"
                        + "        },"
                        + "    2: {"
                        + "         valid: true"
                        + "       },"
                        + "    3: {"
                        + "         valid: true"
                        + "       }"
                        + "}");
    }
    
    @DmnDeployment
    public void testPriorityHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .instanceId("myInstanceId")
                .executionId("myExecutionId")
                .activityId("myActivityId")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").list()).hasSize(1);
        assertThat(historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").instanceId("myInstanceId").list()).hasSize(1);
        assertThat(historyService.createHistoricDecisionExecutionQuery().executionId("myInstanceId2").list()).isEmpty();
        assertThat(historyService.createHistoricDecisionExecutionQuery().executionId("myExecutionId").list()).hasSize(1);
        assertThat(historyService.createHistoricDecisionExecutionQuery().executionId("myExecutionId2").list()).isEmpty();
        assertThat(historyService.createHistoricDecisionExecutionQuery().activityId("myActivityId").list()).hasSize(1);
        assertThat(historyService.createHistoricDecisionExecutionQuery().activityId("myActivityId2").list()).isEmpty();

        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        assertThat(decisionExecution.getDecisionDefinitionId()).isNotNull();
        assertThat(decisionExecution.getDeploymentId()).isNotNull();
        assertThat(decisionExecution.getInstanceId()).isEqualTo("myInstanceId");
        assertThat(decisionExecution.getExecutionId()).isEqualTo("myExecutionId");
        assertThat(decisionExecution.getActivityId()).isEqualTo("myActivityId");
        assertThat(decisionExecution.isFailed()).isFalse();
        assertThat(decisionExecution.getStartTime()).isNotNull();
        assertThat(decisionExecution.getEndTime()).isNotNull();
        assertThat(decisionExecution.getExecutionJson()).isNotNull();

        JsonNode executionNode = dmnEngineConfiguration.getObjectMapper().readTree(decisionExecution.getExecutionJson());
        assertThatJson(executionNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    decisionKey: 'decision1',"
                        + "    decisionName: 'Full Decision',"
                        + "    hitPolicy: 'PRIORITY'"
                        + "}");

        JsonNode inputVariables = executionNode.get("inputVariables");
        assertThat(inputVariables.isObject()).isTrue();
        assertThatJson(inputVariables)
                .isEqualTo("{"
                        + "    inputVariable1: 5"
                        + "}");

        JsonNode inputVariableTypes = executionNode.get("inputVariableTypes");
        assertThat(inputVariableTypes.isObject()).isTrue();
        assertThatJson(inputVariableTypes)
                .isEqualTo("{"
                        + "    inputVariable1: 'number'"
                        + "}");

        JsonNode decisionResultArray = executionNode.get("decisionResult");
        assertThat(decisionResultArray.isArray()).isTrue();
        assertThatJson(decisionResultArray)
                .isEqualTo("["
                        + "  {"
                        + "    outputVariable1: 'OUTPUT2'"
                        + "  }"
                        + "]");

        JsonNode decisionResultTypes = executionNode.get("decisionResultTypes");
        assertThat(decisionResultTypes.isObject()).isTrue();
        assertThatJson(decisionResultTypes)
                .isEqualTo("{"
                        + "    outputVariable1: 'string'"
                        + "}");

        JsonNode ruleExecutions = executionNode.get("ruleExecutions");
        assertThat(ruleExecutions.isObject()).isTrue();
        assertThatJson(ruleExecutions)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "    1: {"
                        + "         valid: true"
                        + "        },"
                        + "    2: {"
                        + "         valid: true"
                        + "       },"
                        + "    3: {"
                        + "         valid: true"
                        + "       }"
                        + "}");
    }
    
    @DmnDeployment
    public void testHistoricDecisionQueryOrdering() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();

        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        String firstDecisionExecutionId = decisionExecution.getId();

        dmnEngineConfiguration.getClock().setCurrentTime(new Date(new Date().getTime() + 2000L));

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();

        List<DmnHistoricDecisionExecution> historicExecutions = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").list();
        assertThat(historicExecutions).hasSize(2);

        String secondDecisionExcecutionId = null;
        for (DmnHistoricDecisionExecution historicDecisionExecution : historicExecutions) {
            if (!historicDecisionExecution.getId().equals(firstDecisionExecutionId)) {
                secondDecisionExcecutionId = historicDecisionExecution.getId();
            }
        }

        assertThat(secondDecisionExcecutionId).isNotNull();

        historicExecutions = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").orderByStartTime().asc().list();
        assertThat(historicExecutions)
                .extracting(DmnHistoricDecisionExecution::getId)
                .containsExactly(firstDecisionExecutionId, secondDecisionExcecutionId);

        historicExecutions = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").orderByStartTime().desc().list();
        assertThat(historicExecutions)
                .extracting(DmnHistoricDecisionExecution::getId)
                .containsExactly(secondDecisionExcecutionId, firstDecisionExecutionId);
    }
    
    @DmnDeployment
    public void testHistoricDecisionQueryOrderingAndPaging() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();

        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        String firstDecisionExecutionId = decisionExecution.getId();

        dmnEngineConfiguration.getClock().setCurrentTime(new Date(new Date().getTime() + 2000L));

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();

        List<DmnHistoricDecisionExecution> historicExecutions = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").list();
        assertThat(historicExecutions).hasSize(2);

        String secondDecisionExcecutionId = null;
        for (DmnHistoricDecisionExecution historicDecisionExecution : historicExecutions) {
            if (!historicDecisionExecution.getId().equals(firstDecisionExecutionId)) {
                secondDecisionExcecutionId = historicDecisionExecution.getId();
            }
        }

        assertThat(secondDecisionExcecutionId).isNotNull();

        historicExecutions = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").orderByStartTime().asc().listPage(0, 10);
        assertThat(historicExecutions)
                .extracting(DmnHistoricDecisionExecution::getId)
                .containsExactly(firstDecisionExecutionId, secondDecisionExcecutionId);

        historicExecutions = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").orderByStartTime().desc().listPage(0, 10);
        assertThat(historicExecutions)
                .extracting(DmnHistoricDecisionExecution::getId)
                .containsExactly(secondDecisionExcecutionId, firstDecisionExecutionId);
    }

    @DmnDeployment
    public void testHistoricDecisionService() throws Exception {
        ruleService.createExecuteDecisionBuilder()
            .decisionKey("expandedDecisionService")
            .variable("input1", "test1")
            .variable("input2", "test2")
            .variable("input3", "test3")
            .variable("input4", "test4")
            .executeWithAuditTrail();

        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("expandedDecisionService").singleResult();
        assertThat(decisionExecution.getDecisionDefinitionId()).isNotNull();
        assertThat(decisionExecution.getDeploymentId()).isNotNull();
        assertThat(decisionExecution.isFailed()).isFalse();
        assertThat(decisionExecution.getStartTime()).isNotNull();
        assertThat(decisionExecution.getEndTime()).isNotNull();
        assertThat(decisionExecution.getExecutionJson()).isNotNull();

        JsonNode executionNode = dmnEngineConfiguration.getObjectMapper().readTree(decisionExecution.getExecutionJson());
        assertThat(executionNode.get("decisionKey").asText()).isEqualTo("expandedDecisionService");

        JsonNode decisionServiceResult =  executionNode.get("decisionServiceResult");
        assertThat(decisionServiceResult.get("decision1").isArray()).isTrue();
        assertThat(decisionServiceResult.get("decision2").isArray()).isTrue();

        assertThat(decisionServiceResult.get("decision1")).hasSize(3);
        assertThat(decisionServiceResult.get("decision2")).hasSize(3);

        JsonNode decisionResultArray = executionNode.get("decisionResult");
        assertThat(decisionResultArray.isArray()).isTrue();
        assertThat(decisionResultArray).isEmpty();

        JsonNode ruleExecutions = executionNode.get("childDecisionExecutions");
        assertThat(ruleExecutions.isObject()).isTrue();
        assertThat(ruleExecutions.has("decision4")).isTrue();
        assertThat(ruleExecutions.has("decision3")).isTrue();
        assertThat(ruleExecutions.has("decision1")).isTrue();
        assertThat(ruleExecutions.has("decision2")).isTrue();
    }
}

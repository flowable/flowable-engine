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

import java.util.Arrays;
import java.util.HashSet;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.impl.test.PluggableFlowableDmnTestCase;
import org.flowable.dmn.engine.test.DmnDeployment;

/**
 * @author Filip Hrisafov
 */
public class HistoryDataDeleteTest extends PluggableFlowableDmnTestCase {

    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn")
    public void testDeleteById() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3");

        DmnHistoricDecisionExecution execution = historyService.createHistoricDecisionExecutionQuery().activityId("execution-1").singleResult();

        historyService.createHistoricDecisionExecutionQuery().id(execution.getId()).delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-2", "execution-3");

        execution = historyService.createHistoricDecisionExecutionQuery().activityId("execution-3").singleResult();

        historyService.createHistoricDecisionExecutionQuery().id(execution.getId()).delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-2");

        historyService.createHistoricDecisionExecutionQuery().id("unknown").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-2");
    }

    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn")
    public void testDeleteByIds() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3");

        DmnHistoricDecisionExecution execution1 = historyService.createHistoricDecisionExecutionQuery().activityId("execution-1").singleResult();
        DmnHistoricDecisionExecution execution2 = historyService.createHistoricDecisionExecutionQuery().activityId("execution-2").singleResult();

        historyService.createHistoricDecisionExecutionQuery().ids(new HashSet<>(Arrays.asList(execution1.getId(), execution2.getId()))).delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-3");

        historyService.createHistoricDecisionExecutionQuery().ids(new HashSet<>(Arrays.asList("unknown1", "unknown2"))).delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-3");
    }

    @DmnDeployment(resources = {
            "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn",
            "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple2.dmn"
    })
    public void testDeleteByDecisionDefinitionId() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision2")
                .activityId("execution-4")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision2")
                .activityId("execution-5")
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3", "execution-4", "execution-5");

        DmnDecision decision = repositoryService.createDecisionQuery().decisionKey("decision").singleResult();

        historyService.createHistoricDecisionExecutionQuery().decisionDefinitionId(decision.getId()).delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-4", "execution-5");

        historyService.createHistoricDecisionExecutionQuery().decisionDefinitionId("unknown").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-4", "execution-5");
    }

    @DmnDeployment(resources = {
            "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn",
            "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple2.dmn"
    })
    public void testDeleteByDecisionKey() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision2")
                .activityId("execution-4")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision2")
                .activityId("execution-5")
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3", "execution-4", "execution-5");

        historyService.createHistoricDecisionExecutionQuery().decisionKey("decision").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-4", "execution-5");

        historyService.createHistoricDecisionExecutionQuery().decisionKey("unknown").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-4", "execution-5");
    }

    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn")
    public void testDeleteByInstanceIdAndScopeType() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .instanceId("proc-1")
                .scopeType(ScopeTypes.BPMN)
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .instanceId("proc-1")
                .scopeType(ScopeTypes.BPMN)
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .instanceId("case-1")
                .scopeType(ScopeTypes.CMMN)
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-4")
                .instanceId("proc-2")
                .scopeType(ScopeTypes.BPMN)
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3", "execution-4");

        historyService.createHistoricDecisionExecutionQuery().instanceId("proc-1").scopeType(ScopeTypes.CMMN).delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3", "execution-4");

        historyService.createHistoricDecisionExecutionQuery().instanceId("proc-1").scopeType(ScopeTypes.BPMN).delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-3", "execution-4");
    }

    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn")
    public void testDeleteByInstanceIdAndWithoutScopeType() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .instanceId("proc-1")
                .scopeType(ScopeTypes.BPMN)
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .instanceId("proc-1")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .instanceId("case-1")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-4")
                .instanceId("proc-2")
                .scopeType(ScopeTypes.BPMN)
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3", "execution-4");

        historyService.createHistoricDecisionExecutionQuery().instanceId("proc-1").withoutScopeType().delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-3", "execution-4");
    }

    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn")
    public void testDeleteByActivityId() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3");

        historyService.createHistoricDecisionExecutionQuery().activityId("execution-2").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-3");

        historyService.createHistoricDecisionExecutionQuery().activityId("unknown").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-3");
    }

    @DmnDeployment(resources = "org/flowable/dmn/engine/test/runtime/HistoryDataDeleteTest.simple.dmn")
    public void testDeleteByTenantId() {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-1")
                .tenantId("tenantA")
                .fallbackToDefaultTenant()
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-2")
                .tenantId("tenantA")
                .fallbackToDefaultTenant()
                .executeWithSingleResult();

        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision")
                .activityId("execution-3")
                .tenantId("tenantB")
                .fallbackToDefaultTenant()
                .executeWithSingleResult();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-1", "execution-2", "execution-3");

        historyService.createHistoricDecisionExecutionQuery().tenantId("tenantA").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-3");

        historyService.createHistoricDecisionExecutionQuery().tenantId("unknown").delete();

        assertThat(historyService.createHistoricDecisionExecutionQuery().list())
                .extracting(DmnHistoricDecisionExecution::getActivityId)
                .containsExactlyInAnyOrder("execution-3");
    }
}

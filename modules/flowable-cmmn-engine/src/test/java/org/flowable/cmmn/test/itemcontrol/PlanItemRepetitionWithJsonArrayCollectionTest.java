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
package org.flowable.cmmn.test.itemcontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ArrayNode;

/**
 * @author Filip Hrisafov
 */
public class PlanItemRepetitionWithJsonArrayCollectionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void repetitionWithStringItems() {
        ArrayNode collection = cmmnEngineConfiguration.getObjectMapper().createArrayNode()
                .add("A")
                .add("B");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repetitionWithJsonArray")
                .variable("myCollection", collection)
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(2);
        assertPlanItemInstanceState(planItemInstances, "Task (A)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task (B)", ACTIVE);

        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "item")).isInstanceOf(String.class);
        }

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(Task::getName, Task::getAssignee)
                .containsExactlyInAnyOrder(
                        tuple("Task (A)", "A"),
                        tuple("Task (B)", "B")
                );
    }
}

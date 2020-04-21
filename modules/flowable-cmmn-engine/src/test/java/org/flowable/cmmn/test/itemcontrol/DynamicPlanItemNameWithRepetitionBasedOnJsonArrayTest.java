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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Testing dynamic, expression based plan item name with local, collection based variables as well as case based ones.
 *
 * @author Micha Kiener
 */
public class DynamicPlanItemNameWithRepetitionBasedOnJsonArrayTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testDynamicNameWithRepetitionCollection() {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.addObject().put("name", "A").put("foo", "a");
        arrayNode.addObject().put("name", "B").put("foo", "b");
        arrayNode.addObject().put("name", "C").put("foo", "c");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("dynamicPlanItemNameTestOnJsonArray")
                .variable("myCollection", arrayNode)
                .variable("foo", "FooValue")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task (A / 0 - FooValue)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task (B / 1 - FooValue)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task (C / 2 - FooValue)", ACTIVE);
    }

    @Test
    @CmmnDeployment
    public void testDynamicNameWithRepetitionCollectionNoFallbackExpression() {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.addObject().put("name", "A").put("bar", "a");
        arrayNode.addObject().put("name", "B").put("bar", "b");
        arrayNode.addObject().put("name", "C").put("bar", "c");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("dynamicPlanItemNameTestOnJsonArray")
                .variable("myCollection", arrayNode)
                .variable("foo", "FooValue")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task (A / 0 - FooValue)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task (B / 1 - FooValue)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task (C / 2 - FooValue)", ACTIVE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/DynamicPlanItemNameWithRepetitionBasedOnJsonArrayTest.testDynamicNameWithRepetitionCollectionNoFallbackExpression.cmmn")
    public void testDynamicNameWithRepetitionCollectionNoFallbackExpressionWithAvailablePlanItem() {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        arrayNode.addObject().put("name", "A").put("bar", "a");
        arrayNode.addObject().put("name", "B").put("bar", "b");
        arrayNode.addObject().put("name", "C").put("bar", "c");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("dynamicPlanItemNameTestOnJsonArray")
                .variable("foo", "FooValue")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(1);

        // as we don't have the collection yet available, the name must use the expression as fallback without any exception, as the item / itemIndex local
        // variables are not available on the available plan item instance
        assertPlanItemInstanceState(planItemInstances, "Task (${item.name} / ${itemIndex} - ${foo})", AVAILABLE);

        // now kick-off the repetition by setting the collection variable
        cmmnRuntimeService.setVariable(caseInstance.getId(), "myCollection", arrayNode);

        planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(3);

        assertPlanItemInstanceState(planItemInstances, "Task (A / 0 - FooValue)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task (B / 1 - FooValue)", ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Task (C / 2 - FooValue)", ACTIVE);
    }
}

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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Dennis Federico
 */
public class CompletionNeutralConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/completionNeutralAtPlanItem.cmmn")
    public void completionNeutralDefinedAtPlanItem(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        List<PlanItem> planItems = planModel.getPlanItems();
        assertThat(planItems)
                .hasSize(4)
                .extracting(
                        planItem -> planItem.getItemControl(),
                        planItem -> planItem.getItemControl().getCompletionNeutralRule(),
                        planItem -> planItem.getItemControl().getCompletionNeutralRule().getCondition())
                .doesNotContainNull();
        planItems.forEach(planItem -> {
            assertThat(planItem.getItemControl().getCompletionNeutralRule().getCondition()).isEqualTo("${" + planItem.getId() + "}");
        });

        Stage stageOne = (Stage) cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionInStageOrDownwards("stageOne");
        List<PlanItem> planItems1 = stageOne.getPlanItems();

        assertThat(planItems1)
                .hasSize(1)
                .extracting(
                        planItem -> planItem.getItemControl(),
                        planItem -> planItem.getItemControl().getCompletionNeutralRule())
                .doesNotContainNull();
        PlanItem planItem = planItems1.get(0);
        assertThat(planItem.getItemControl().getCompletionNeutralRule().getCondition()).isNull();

        List<ExtensionElement> extensionElements = planItem.getExtensionElements().get("planItemTest");
        assertThat(extensionElements)
                .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                .containsExactly(tuple("planItemTest", "hello"));
    }

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/completionNeutralAtPlanItemDefinition.cmmn")
    public void completionNeutralDefinedAtPlanItemDefinition(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertThat(planItemDefinitions).hasSize(4);
        planItemDefinitions.forEach(definition -> {
            assertThat(definition.getDefaultControl()).isNotNull();
            assertThat(definition.getDefaultControl().getCompletionNeutralRule()).isNotNull();
            assertThat(definition.getDefaultControl().getCompletionNeutralRule().getCondition()).isEqualTo("${" + definition.getId() + "}");
        });

        PlanItemDefinition planItemDef = cmmnModel.findPlanItemDefinition("taskTwo");
        List<ExtensionElement> extensionElements = planItemDef.getExtensionElements().get("taskTest");
        assertThat(extensionElements)
                .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                .containsExactly(tuple("taskTest", "hello"));
    }
}

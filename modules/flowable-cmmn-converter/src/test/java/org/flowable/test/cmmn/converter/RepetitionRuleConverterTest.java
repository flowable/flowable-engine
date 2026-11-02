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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
class RepetitionRuleConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/repetitionRuleVariableAggregations.cmmn")
    void variableAggregations(CmmnModel cmmnModel) {

        PlanItem planItem2 = cmmnModel.getPrimaryCase()
                .getPlanModel()
                .getPlanItem("planItem2");

        assertThat(planItem2).isNotNull();
        assertThat(planItem2.getPlanItemDefinition().getId()).isEqualTo("humanTask2");

        RepetitionRule repetitionRule = planItem2.getItemControl().getRepetitionRule();
        assertThat(repetitionRule).isNotNull();
        assertThat(repetitionRule.getRepetitionCounterVariableName()).isEqualTo("repetitionCounter");
        assertThat(repetitionRule.getCollectionVariableName()).isEqualTo("myCollection");
        assertThat(repetitionRule.getElementVariableName()).isEqualTo("item");
        assertThat(repetitionRule.getElementIndexVariableName()).isEqualTo("itemIndex");

        ArrayList<VariableAggregationDefinition> aggregations = new ArrayList<>(repetitionRule.getAggregations().getAggregations());
        assertThat(aggregations)
                .extracting(VariableAggregationDefinition::getTarget, VariableAggregationDefinition::getTargetExpression,
                        VariableAggregationDefinition::getImplementationType, VariableAggregationDefinition::getImplementation)
                .containsExactly(
                        tuple("reviews", null, null, null),
                        tuple(null, "${targetVar}", null, null),
                        tuple("reviews", null, ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, "${customVariableAggregator}"),
                        tuple("reviews", null, ImplementationType.IMPLEMENTATION_TYPE_CLASS, "com.example.flowable.CustomVariableAggregator")
                );

        assertThat(aggregations.get(0).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("taskAssignee", null, "userId", null),
                        tuple("approved", null, null, null),
                        tuple(null, "${score * 2}", null, "${targetVar}")
                );

        assertThat(aggregations.get(1).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("taskAssignee", null, "userId", null),
                        tuple("approved", null, null, null)
                );

        assertThat(aggregations.get(2).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("approved", null, null, null)
                );

        assertThat(aggregations.get(3).getDefinitions())
                .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getSourceExpression,
                        VariableAggregationDefinition.Variable::getTarget, VariableAggregationDefinition.Variable::getTargetExpression)
                .containsExactly(
                        tuple("description", null, null, null)
                );
    }

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/repetitionRuleWithCustomExtensionElements.cmmn")
    void customExtensionElements(CmmnModel cmmnModel) {

        Stage mainPlanModel = cmmnModel.getPrimaryCase()
                .getPlanModel();

        PlanItem planItem = mainPlanModel.getPlanItem("repetitionRuleWithExtensionElements");
        PlanItemControl itemControl = planItem.getItemControl();
        assertThat(itemControl).isNotNull();
        RepetitionRule rule = itemControl.getRepetitionRule();
        assertThat(rule).isNotNull();
        List<ExtensionElement> testEntryExtensions = rule.getExtensionElements().get("testEntry");
        assertThat(testEntryExtensions)
                .extracting(ExtensionElement::getElementText)
                .containsExactly("Test Entry");

        List<ExtensionElement> nestedTestExtensions = rule.getExtensionElements().get("nestedTest");
        assertThat(nestedTestExtensions)
                .hasSize(1)
                .first()
                .satisfies(extensionElement -> {
                    assertThat(extensionElement.getAttributeValue(null, "name"))
                            .isEqualTo("Test");
                    assertThat(extensionElement.getChildElements().get("nestedValue"))
                            .extracting(ExtensionElement::getElementText)
                            .containsExactly("Test Value");
                });

    }

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/repetitionRuleVariableAggregationsAndCustomExtensionElements.cmmn")
    void aggregationsAndCustomExtensionElements(CmmnModel cmmnModel) {
        PlanItem planItem = cmmnModel.getPrimaryCase()
                .getPlanModel()
                .getPlanItem("planItem1");

        PlanItemControl itemControl = planItem.getItemControl();
        assertThat(itemControl).isNotNull();
        RepetitionRule repetitionRule = itemControl.getRepetitionRule();
        assertThat(repetitionRule).isNotNull();
        assertThat(repetitionRule.getExtensionElements())
                .containsOnlyKeys("testEntry", "nestedTest");

        Collection<VariableAggregationDefinition> aggregations = repetitionRule.getAggregations().getAggregations();
        assertThat(aggregations)
                .extracting(VariableAggregationDefinition::getTarget)
                .containsExactly("reviews");

        assertThat(aggregations)
                .first()
                .satisfies(aggregation -> {
                    assertThat(aggregation.getDefinitions())
                            .extracting(VariableAggregationDefinition.Variable::getSource, VariableAggregationDefinition.Variable::getTarget)
                            .containsExactly(
                                    tuple("taskAssignee", "userId"),
                                    tuple("approved", null)
                            );
                });

        List<ExtensionElement> testEntryExtensions = repetitionRule.getExtensionElements().get("testEntry");
        assertThat(testEntryExtensions)
                .extracting(ExtensionElement::getElementText)
                .containsExactly("Test Entry");

        List<ExtensionElement> nestedTestExtensions = repetitionRule.getExtensionElements().get("nestedTest");
        assertThat(nestedTestExtensions)
                .hasSize(1)
                .first()
                .satisfies(extensionElement -> {
                    assertThat(extensionElement.getAttributeValue(null, "name"))
                            .isEqualTo("Test");
                    assertThat(extensionElement.getChildElements().get("nestedValue"))
                            .extracting(ExtensionElement::getElementText)
                            .containsExactly("Test Value");
                });

    }

}

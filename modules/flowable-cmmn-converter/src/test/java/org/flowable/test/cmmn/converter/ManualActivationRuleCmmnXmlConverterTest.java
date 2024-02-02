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

import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.ManualActivationRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Filip Hrisafov
 */
class ManualActivationRuleCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/manualActivationRuleWithExtensionElements.cmmn")
    void customExtensionElements(CmmnModel model) {
        Stage mainPlanModel = model.getPrimaryCase()
                .getPlanModel();

        PlanItem planItem = mainPlanModel.getPlanItem("manualActivationRuleWithExtensionElements");
        PlanItemControl itemControl = planItem.getItemControl();
        assertThat(itemControl).isNotNull();
        ManualActivationRule rule = itemControl.getManualActivationRule();
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

}

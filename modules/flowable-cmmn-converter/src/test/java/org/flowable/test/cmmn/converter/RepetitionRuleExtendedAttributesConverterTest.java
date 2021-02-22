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
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * Testing to read and write the extended repetition rule attributes.
 *
 * @author Micha Kiener
 */
public class RepetitionRuleExtendedAttributesConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/repetitionRuleExtension.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getCases()).hasSize(1);

        Map<String, CaseElement> caseElements = cmmnModel.getCases().get(0).getAllCaseElements();

        assertRepetitionRuleAttributes(caseElements, "Task A", null,
            null, null, null);

        assertRepetitionRuleAttributes(caseElements, "Task B", null,
            null, null, 1);

        assertRepetitionRuleAttributes(caseElements, "Task C", null,
            null, null, RepetitionRule.MAX_INSTANCE_COUNT_UNLIMITED);

        assertRepetitionRuleAttributes(caseElements, "Task D", null,
            null, null, null);

        assertRepetitionRuleAttributes(caseElements, "Task E", "entriesForTaskE",
            "item", "itemIndex", null);
    }

    protected void assertRepetitionRuleAttributes(Map<String, CaseElement> caseElements, String planItemName,
        String collectionVariableName, String elementVariableName, String elementIndexVariableName, Integer maxInstanceCount) {
        List<CaseElement> planItems = caseElements.values().stream()
            .filter(caseElement -> caseElement instanceof PlanItem && planItemName.equals(caseElement.getName()))
            .collect(Collectors.toList());

        assertThat(planItems).as("No plan item found with name " + planItemName).isNotEmpty();

        assertThat(planItems).as("More than one plan item found with name " + planItemName + ", make sure it is unique for testing purposes").hasSize(1);

        RepetitionRule repetitionRule = ((PlanItem) planItems.get(0)).getItemControl().getRepetitionRule();
        assertThat(repetitionRule).as("no repetition rule found for plan item with name '" + planItemName + "'").isNotNull();

        assertThat(repetitionRule.getCollectionVariableName()).isEqualTo(collectionVariableName);
        assertThat(repetitionRule.getElementVariableName()).isEqualTo(elementVariableName);
        assertThat(repetitionRule.getElementIndexVariableName()).isEqualTo(elementIndexVariableName);
        assertThat(repetitionRule.getMaxInstanceCount()).isEqualTo(maxInstanceCount);
    }
}

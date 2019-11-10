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

import static org.flowable.cmmn.model.RepetitionRule.MAX_INSTANCE_COUNT_ONE;
import static org.flowable.cmmn.model.RepetitionRule.MAX_INSTANCE_COUNT_UNLIMITED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.RepetitionRule;
import org.junit.Test;

/**
 * Testing to read and write the extended repetition rule attributes.
 *
 * @author Micha Kiener
 */
public class RepetitionRuleExtendedAttributesConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/repetitionRuleExtension.cmmn";

    @Test
    public void convertXMLToModel() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        validateModel(cmmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateModel(parsedModel);
    }

    public void validateModel(CmmnModel cmmnModel) {
        assertNotNull(cmmnModel);
        assertNotNull(cmmnModel.getCases());
        assertEquals(1, cmmnModel.getCases().size());

        Map<String, CaseElement> caseElements = cmmnModel.getCases().get(0).getAllCaseElements();

        assertRepetitionRuleAttributes(caseElements, "Task A", null,
            null, null, null);

        assertRepetitionRuleAttributes(caseElements, "Task B", null,
            null, null, MAX_INSTANCE_COUNT_ONE);

        assertRepetitionRuleAttributes(caseElements, "Task C", null,
            null, null, MAX_INSTANCE_COUNT_UNLIMITED);

        assertRepetitionRuleAttributes(caseElements, "Task D", null,
            null, null, null);

        assertRepetitionRuleAttributes(caseElements, "Task E", "entriesForTaskE",
            "item", "itemIndex", null);
    }

    protected void assertRepetitionRuleAttributes(Map<String, CaseElement> caseElements, String planItemName,
        String collectionVariableName, String elementVariableName, String elementIndexVariableName, String maxInstanceCount) {
        List<CaseElement> planItems = caseElements.values().stream()
            .filter(caseElement -> caseElement instanceof PlanItem && planItemName.equals(caseElement.getName()))
            .collect(Collectors.toList());

        if (planItems.size() == 0) {
            fail("No plan item found with name " + planItemName);
        }

        if (planItems.size() > 1) {
            fail("More than one plan item found with name " + planItemName + ", make sure it is unique for testing purposes");
        }

        RepetitionRule repetitionRule = ((PlanItem) planItems.get(0)).getItemControl().getRepetitionRule();
        assertNotNull("no repetition rule found for plan item with name '" + planItemName + "'", repetitionRule);

        assertEquals(collectionVariableName, repetitionRule.getCollectionVariableName());
        assertEquals(elementVariableName, repetitionRule.getElementVariableName());
        assertEquals(elementIndexVariableName, repetitionRule.getElementIndexVariableName());
        assertEquals(maxInstanceCount, repetitionRule.getMaxInstanceCount());
    }
}

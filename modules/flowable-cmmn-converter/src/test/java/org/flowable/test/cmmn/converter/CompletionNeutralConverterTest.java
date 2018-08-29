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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.function.Consumer;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

/**
 * @author Dennis Federico
 */
public class CompletionNeutralConverterTest extends AbstractConverterTest {

    @Test
    public void completionNeutralDefinedAtPlanItem() throws Exception {
        String cmmnResource = "org/flowable/test/cmmn/converter/completionNeutralAtPlanItem.cmmn";
        Consumer<CmmnModel> modelValidator = cmmnModel -> {
            assertNotNull(cmmnModel);
            Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
            List<PlanItem> planItems = planModel.getPlanItems();
            assertEquals(4, planItems.size());
            planItems.forEach(planItem -> {
                assertNotNull(planItem.getItemControl());
                assertNotNull(planItem.getItemControl().getCompletionNeutralRule());
                assertNotNull(planItem.getItemControl().getCompletionNeutralRule().getCondition());
                assertEquals("${" + planItem.getId() + "}", planItem.getItemControl().getCompletionNeutralRule().getCondition());
            });

            Stage stageOne = cmmnModel.getPrimaryCase().findStage("stageOne");
            List<PlanItem> planItems1 = stageOne.getPlanItems();
            assertEquals(1, planItems1.size());
            PlanItem planItem = planItems1.get(0);
            assertNotNull(planItem.getItemControl());
            assertNotNull(planItem.getItemControl().getCompletionNeutralRule());
            assertNull(planItem.getItemControl().getCompletionNeutralRule().getCondition());
            
            assertEquals(1, planItem.getExtensionElements().size());
            List<ExtensionElement> extensionElements = planItem.getExtensionElements().get("planItemTest");
            assertEquals(1, extensionElements.size());
            ExtensionElement extensionElement = extensionElements.get(0);
            assertEquals("planItemTest", extensionElement.getName());
            assertEquals("hello", extensionElement.getElementText());
        };

        validateModel(cmmnResource, modelValidator);
    }

    @Test
    public void completionNeutralDefinedAtPlanItemDefinition() throws Exception {
        String cmmnResource = "org/flowable/test/cmmn/converter/completionNeutralAtPlanItemDefinition.cmmn";
        Consumer<CmmnModel> modelValidator = cmmnModel -> {
            assertNotNull(cmmnModel);
            Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
            List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
            assertEquals(4, planItemDefinitions.size());
            planItemDefinitions.forEach(definition -> {
                assertNotNull(definition.getDefaultControl());
                assertNotNull(definition.getDefaultControl().getCompletionNeutralRule());
                assertNotNull(definition.getDefaultControl().getCompletionNeutralRule().getCondition());
                assertEquals("${" + definition.getId() + "}", definition.getDefaultControl().getCompletionNeutralRule().getCondition());
            });
            
            PlanItemDefinition planItemDef = cmmnModel.findPlanItemDefinition("taskTwo");
            assertEquals(1, planItemDef.getExtensionElements().size());
            List<ExtensionElement> extensionElements = planItemDef.getExtensionElements().get("taskTest");
            assertEquals(1, extensionElements.size());
            ExtensionElement extensionElement = extensionElements.get(0);
            assertEquals("taskTest", extensionElement.getName());
            assertEquals("hello", extensionElement.getElementText());
        };

        validateModel(cmmnResource, modelValidator);
    }

    private void validateModel(String cmmnResource, Consumer<CmmnModel> modelValidator) throws Exception {
        CmmnModel cmmnModel = readXMLFile(cmmnResource);
        modelValidator.accept(cmmnModel);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        modelValidator.accept(parsedModel);
    }

}

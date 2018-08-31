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

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class SimpleExtensionElementsCmmnXmlConverterTest extends AbstractConverterTest {

    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/simple_extensionelements.cmmn";

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
        assertEquals(1, cmmnModel.getCases().size());

        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertEquals("myCase", caze.getId());

        // Plan model
        Stage planModel = caze.getPlanModel();
        assertNotNull(planModel);
        assertEquals("myPlanModel", planModel.getId());
        assertEquals("My CasePlanModel", planModel.getName());
        assertEquals("formKey", planModel.getFormKey());

        Task task = (Task) planModel.findPlanItemDefinition("taskA");
        assertEquals(1, task.getExtensionElements().size());
        List<ExtensionElement> extensionElements = task.getExtensionElements().get("taskTest");
        assertEquals(1, extensionElements.size());
        ExtensionElement extensionElement = extensionElements.get(0);
        assertEquals("taskTest", extensionElement.getName());
        assertEquals("hello", extensionElement.getElementText());
        
        Milestone milestone = (Milestone) planModel.findPlanItemDefinition("mileStoneOne");
        assertEquals(1, milestone.getExtensionElements().size());
        extensionElements = milestone.getExtensionElements().get("milestoneTest");
        assertEquals(1, extensionElements.size());
        extensionElement = extensionElements.get(0);
        assertEquals("milestoneTest", extensionElement.getName());
        assertEquals("hello2", extensionElement.getElementText());
        
        PlanItem planItem = planModel.findPlanItemInPlanFragmentOrDownwards("planItemTaskA");
        assertEquals(1, planItem.getExtensionElements().size());
        extensionElements = planItem.getExtensionElements().get("test");
        assertEquals(1, extensionElements.size());
        extensionElement = extensionElements.get(0);
        assertEquals("test", extensionElement.getName());
        assertEquals("test", extensionElement.getElementText());
    }

}

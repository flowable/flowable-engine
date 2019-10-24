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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class CasePageTaskCmmnXmlConverterTest extends AbstractConverterTest {
    
    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/case-page-task.cmmn";
    
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
        assertEquals("casePageCase", caze.getId());
        
        Stage planModel = caze.getPlanModel();
        
        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertEquals(2, planItemDefinitions.size());
        assertEquals(2, planModel.findPlanItemDefinitionsOfType(CasePageTask.class, false).size());
        
        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertEquals(2, planItems.size());
        
        PlanItem planItemTaskA = cmmnModel.findPlanItem("planItemTaskA");
        PlanItemDefinition planItemDefinition = planItemTaskA.getPlanItemDefinition();
        assertEquals(0, planItemTaskA.getEntryCriteria().size());
        assertTrue(planItemDefinition instanceof CasePageTask);
        CasePageTask taskA = (CasePageTask) planItemDefinition;
        assertEquals(CasePageTask.TYPE, taskA.getType());
        assertEquals("A", taskA.getName());
        assertEquals("Label 1", taskA.getLabel());
        assertEquals("Icon 1", taskA.getIcon());
        assertNotNull(taskA.getDefaultControl());
        assertNotNull(taskA.getDefaultControl().getParentCompletionRule());
        assertEquals(ParentCompletionRule.ALWAYS_IGNORE, taskA.getDefaultControl().getParentCompletionRule().getType());

        PlanItem planItemTaskB = cmmnModel.findPlanItem("planItemTaskB");
        planItemDefinition = planItemTaskB.getPlanItemDefinition();
        assertEquals(1, planItemTaskB.getEntryCriteria().size());
        assertTrue(planItemDefinition instanceof CasePageTask);
        CasePageTask taskB = (CasePageTask) planItemDefinition;
        assertEquals(CasePageTask.TYPE, taskB.getType());
        assertEquals("B", taskB.getName());
        assertNotNull(taskB.getDefaultControl());
        assertNotNull(taskB.getDefaultControl().getParentCompletionRule());
        assertEquals(ParentCompletionRule.ALWAYS_IGNORE, taskB.getDefaultControl().getParentCompletionRule().getType());
        
        assertEquals(1, taskB.getExtensionElements().size());
        List<ExtensionElement> extensionElements = taskB.getExtensionElements().get("index");
        assertEquals(1, extensionElements.size());
        ExtensionElement extensionElement = extensionElements.get(0);
        assertEquals("index", extensionElement.getName());
        assertEquals("0", extensionElement.getElementText());
    }

}

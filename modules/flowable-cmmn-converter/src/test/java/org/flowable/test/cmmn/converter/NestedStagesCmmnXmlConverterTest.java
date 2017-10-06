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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class NestedStagesCmmnXmlConverterTest extends AbstractConverterTest {
    
    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/nested-stages.cmmn";
    
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
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertEquals(2, planModel.getPlanItems().size());
        
        Stage nestedStage = null;
        for (PlanItem planItem : planModel.getPlanItems()) {
            assertNotNull(planItem.getPlanItemDefinition());
            if (planItem.getPlanItemDefinition() instanceof Stage) {
                nestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertNotNull(nestedStage);
        assertEquals("Nested Stage", nestedStage.getName());
        
        // Nested stage has 3 plan items, and one of them refereces the rootTook from the plan model
        assertEquals(3, nestedStage.getPlanItems().size());
        Stage nestedNestedStage = null;
        for (PlanItem planItem : nestedStage.getPlanItems()) {
            assertNotNull(planItem.getPlanItemDefinition());
            if (planItem.getPlanItemDefinition()  instanceof Stage) {
                nestedNestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertNotNull(nestedNestedStage);
        assertEquals("Nested Stage 2", nestedNestedStage.getName());
        assertEquals(1, nestedNestedStage.getPlanItems().size());
        assertEquals("rootTask", nestedNestedStage.getPlanItems().get(0).getPlanItemDefinition().getId());
    }

}

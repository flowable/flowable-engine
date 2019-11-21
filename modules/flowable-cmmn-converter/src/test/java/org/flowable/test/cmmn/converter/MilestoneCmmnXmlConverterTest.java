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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class MilestoneCmmnXmlConverterTest extends AbstractConverterTest {
    
    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/milestone.cmmn";
    
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
        List<Milestone> milestones = planModel.findPlanItemDefinitionsOfType(Milestone.class, false);
        
        assertEquals(1, milestones.size());
        Milestone milestone = milestones.get(0);
        
        assertEquals("Milestone 1", milestone.getName());
        assertEquals(Integer.valueOf(5), milestone.getDisplayOrder());
        assertEquals("false", milestone.getIncludeInStageOverview());
        
        Stage nestedStage = planModel.findPlanItemDefinitionsOfType(Stage.class, false).get(0);
        assertNotNull(nestedStage);
        assertEquals("Nested Stage", nestedStage.getName());
        
        assertEquals(1, nestedStage.getPlanItems().size());
        milestones = nestedStage.findPlanItemDefinitionsOfType(Milestone.class, false);
        
        assertEquals(1, milestones.size());
        milestone = milestones.get(0);
        
        assertEquals("Milestone 2", milestone.getName());
        assertEquals(Integer.valueOf(3), milestone.getDisplayOrder());
        assertEquals("true", milestone.getIncludeInStageOverview());
    }

}

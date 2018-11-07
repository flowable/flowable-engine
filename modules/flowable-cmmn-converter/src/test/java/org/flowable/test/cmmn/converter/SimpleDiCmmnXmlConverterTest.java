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
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class SimpleDiCmmnXmlConverterTest extends AbstractConverterTest {
    
    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/simpledi.cmmn";
    
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
        
        PlanItem planItem = cmmnModel.findPlanItem("planItem1");
        assertNotNull(planItem);
        assertEquals("task1", planItem.getDefinitionRef());
        
        planItem = cmmnModel.findPlanItem("planItem2");
        assertNotNull(planItem);
        assertEquals("task2", planItem.getDefinitionRef());
        
        GraphicInfo graphicInfo = cmmnModel.getGraphicInfo("myPlanModel");
        assertNotNull(graphicInfo);
        assertEquals(1.0, graphicInfo.getX(), 0.1);
        assertEquals(19.0, graphicInfo.getY(), 0.1);
        assertEquals(400.0, graphicInfo.getWidth(), 0.1);
        assertEquals(300.0, graphicInfo.getHeight(), 0.1);
        
        graphicInfo = cmmnModel.getGraphicInfo("planItem1");
        assertNotNull(graphicInfo);
        assertEquals(70.0, graphicInfo.getX(), 0.1);
        assertEquals(65.0, graphicInfo.getY(), 0.1);
        assertEquals(80.0, graphicInfo.getWidth(), 0.1);
        assertEquals(60.0, graphicInfo.getHeight(), 0.1);
        
        graphicInfo = cmmnModel.getGraphicInfo("planItem2");
        assertNotNull(graphicInfo);
        assertEquals(250.0, graphicInfo.getX(), 0.1);
        assertEquals(100.0, graphicInfo.getY(), 0.1);
        assertEquals(80.0, graphicInfo.getWidth(), 0.1);
        assertEquals(60.0, graphicInfo.getHeight(), 0.1);
        
        graphicInfo = cmmnModel.getGraphicInfo("criterion1");
        assertNotNull(graphicInfo);
        assertEquals(268.0, graphicInfo.getX(), 0.1);
        assertEquals(216.0, graphicInfo.getY(), 0.1);
        assertEquals(20.0, graphicInfo.getWidth(), 0.1);
        assertEquals(28.0, graphicInfo.getHeight(), 0.1);
        
        List<GraphicInfo> waypoints = cmmnModel.getFlowLocationGraphicInfo("CMMNEdge_onPart1");
        assertEquals(4, waypoints.size());
        assertEquals(170.0, waypoints.get(0).getX(), 0.1);
        assertEquals(95.0, waypoints.get(0).getY(), 0.1);
        assertEquals(220.0, waypoints.get(1).getX(), 0.1);
        assertEquals(95.0, waypoints.get(1).getY(), 0.1);
        assertEquals(220.0, waypoints.get(2).getX(), 0.1);
        assertEquals(130.0, waypoints.get(2).getY(), 0.1);
        assertEquals(250.0, waypoints.get(3).getX(), 0.1);
        assertEquals(130.0, waypoints.get(3).getY(), 0.1);
    }

}

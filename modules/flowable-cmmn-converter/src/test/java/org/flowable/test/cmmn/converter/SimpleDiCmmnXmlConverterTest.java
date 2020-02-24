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
import static org.assertj.core.data.Offset.offset;

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
        assertThat(planModel.getPlanItems()).hasSize(2);
        
        PlanItem planItem = cmmnModel.findPlanItem("planItem1");
        assertThat(planItem).isNotNull();
        assertThat(planItem.getDefinitionRef()).isEqualTo("task1");
        
        planItem = cmmnModel.findPlanItem("planItem2");
        assertThat(planItem).isNotNull();
        assertThat(planItem.getDefinitionRef()).isEqualTo("task2");
        
        GraphicInfo graphicInfo = cmmnModel.getGraphicInfo("myPlanModel");
        assertThat(graphicInfo).isNotNull();
        assertThat(graphicInfo.getX()).isCloseTo(1.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(19.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(400.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(300.0, offset(0.1));
        
        graphicInfo = cmmnModel.getGraphicInfo("planItem1");
        assertThat(graphicInfo).isNotNull();
        assertThat(graphicInfo.getX()).isCloseTo(70.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(65.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(80.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(60.0, offset(0.1));
        
        graphicInfo = cmmnModel.getGraphicInfo("planItem2");
        assertThat(graphicInfo).isNotNull();
        assertThat(graphicInfo.getX()).isCloseTo(250.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(100.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(80.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(60.0, offset(0.1));
        
        graphicInfo = cmmnModel.getGraphicInfo("criterion1");
        assertThat(graphicInfo).isNotNull();
        assertThat(graphicInfo.getX()).isCloseTo(268.0, offset(0.1));
        assertThat(graphicInfo.getY()).isCloseTo(216.0, offset(0.1));
        assertThat(graphicInfo.getWidth()).isCloseTo(20.0, offset(0.1));
        assertThat(graphicInfo.getHeight()).isCloseTo(28.0, offset(0.1));
        
        List<GraphicInfo> waypoints = cmmnModel.getFlowLocationGraphicInfo("CMMNEdge_onPart1");
        assertThat(waypoints).hasSize(4);
        assertThat(waypoints.get(0).getX()).isCloseTo(170.0, offset(0.1));
        assertThat(waypoints.get(0).getY()).isCloseTo(95.0, offset(0.1));
        assertThat(waypoints.get(1).getX()).isCloseTo(220.0, offset(0.1));
        assertThat(waypoints.get(1).getY()).isCloseTo(95.0, offset(0.1));
        assertThat(waypoints.get(2).getX()).isCloseTo(220.0, offset(0.1));
        assertThat(waypoints.get(2).getY()).isCloseTo(130.0, offset(0.1));
        assertThat(waypoints.get(3).getX()).isCloseTo(250.0, offset(0.1));
        assertThat(waypoints.get(3).getY()).isCloseTo(130.0, offset(0.1));
    }

}

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

import org.flowable.cmmn.model.CmmnDiEdge;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class SimpleDiCmmnXmlConverterTest {
    
    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/simpledi.cmmn")
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
        assertThat(graphicInfo.getX()).isEqualTo(1.0);
        assertThat(graphicInfo.getY()).isEqualTo(19.0);
        assertThat(graphicInfo.getWidth()).isEqualTo(400.0);
        assertThat(graphicInfo.getHeight()).isEqualTo(300.0);
        
        graphicInfo = cmmnModel.getGraphicInfo("planItem1");
        assertThat(graphicInfo).isNotNull();
        assertThat(graphicInfo.getX()).isEqualTo(70.0);
        assertThat(graphicInfo.getY()).isEqualTo(65.0);
        assertThat(graphicInfo.getWidth()).isEqualTo(80.0);
        assertThat(graphicInfo.getHeight()).isEqualTo(60.0);
        
        graphicInfo = cmmnModel.getGraphicInfo("planItem2");
        assertThat(graphicInfo).isNotNull();
        assertThat(graphicInfo.getX()).isEqualTo(250.0);
        assertThat(graphicInfo.getY()).isEqualTo(100.0);
        assertThat(graphicInfo.getWidth()).isEqualTo(80.0);
        assertThat(graphicInfo.getHeight()).isEqualTo(60.0);
        
        graphicInfo = cmmnModel.getGraphicInfo("criterion1");
        assertThat(graphicInfo).isNotNull();
        assertThat(graphicInfo.getX()).isEqualTo(268.0);
        assertThat(graphicInfo.getY()).isEqualTo(216.0);
        assertThat(graphicInfo.getWidth()).isEqualTo(20.0);
        assertThat(graphicInfo.getHeight()).isEqualTo(28.0);
        
        List<GraphicInfo> waypoints = cmmnModel.getFlowLocationGraphicInfo("CMMNEdge_onPart1");
        assertThat(waypoints).hasSize(4);
        assertThat(waypoints.get(0).getX()).isEqualTo(170.0);
        assertThat(waypoints.get(0).getY()).isEqualTo(95.0);
        assertThat(waypoints.get(1).getX()).isEqualTo(220.0);
        assertThat(waypoints.get(1).getY()).isEqualTo(95.0);
        assertThat(waypoints.get(2).getX()).isEqualTo(220.0);
        assertThat(waypoints.get(2).getY()).isEqualTo(130.0);
        assertThat(waypoints.get(3).getX()).isEqualTo(250.0);
        assertThat(waypoints.get(3).getY()).isEqualTo(130.0);
        
        CmmnDiEdge edgeInfo = cmmnModel.getEdgeInfo("CMMNEdge_onPart1");
        assertThat(edgeInfo.getSourceDockerInfo().getX()).isEqualTo(50.0);
        assertThat(edgeInfo.getSourceDockerInfo().getY()).isEqualTo(10.0);
        assertThat(edgeInfo.getTargetDockerInfo().getX()).isEqualTo(50.0);
        assertThat(edgeInfo.getTargetDockerInfo().getY()).isEqualTo(40.0);
    }

}

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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

/**
 * Created by Pardo David on 16/01/2017.
 */
public class CollapsedSubProcessConverterTest extends AbstractConverterTest {
  
    private static final String START_EVENT = "sid-89C70A03-C51B-4185-AB85-B8476E7A4F0C";
    private static final String SEQUENCEFLOW_TO_COLLAPSEDSUBPROCESS = "sid-B80498C9-A45C-4D58-B4AA-5393A409ACAA";
    private static final String COLLAPSEDSUBPROCESS = "sid-C20D5023-C2B9-4102-AA17-7F16E49E47C1";
    private static final String IN_CSB_START_EVENT = "sid-D8198785-4F74-43A8-A4CD-AF383CEEBE04";
    private static final String IN_CSB_SEQUENCEFLOW_TO_USERTASK = "sid-C633903D-1169-42A4-933D-4D9AAB959792";
    private static final String IN_CSB_USERTASK = "sid-F64640C9-9585-4927-806B-8B0A03DB2B8B";
    private static final String IN_CSB_SEQUENCEFLOW_TO_END = "sid-C1EFE310-3B12-42DA-AEE6-5E442C2FEF19";

    @Test
    public void convertFromXmlToJava() throws Exception{
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
        validateGraphicInfo(bpmnModel);
    }

    @Test
    public void convertFromJavaToXml() throws Exception{
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
        bpmnModel = exportAndReadXMLFile(bpmnModel);
        validateModel(bpmnModel);
        validateGraphicInfo(bpmnModel);
    }

    private void validateModel(BpmnModel bpmnModel) {
        //temp vars
        GraphicInfo gi;
        List<GraphicInfo> flowLocationGraphicInfo;

        //validate parent
        gi = bpmnModel.getGraphicInfo(START_EVENT);
        assertThat(gi.getX()).isEqualTo(73.0);
        assertThat(gi.getY()).isEqualTo(96.0);
        assertThat(gi.getWidth()).isEqualTo(30.0);
        assertThat(gi.getHeight()).isEqualTo(30.0);
        assertThat(gi.getExpanded()).isNull();

        flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(SEQUENCEFLOW_TO_COLLAPSEDSUBPROCESS);

        gi = bpmnModel.getGraphicInfo(COLLAPSEDSUBPROCESS);
        assertThat(gi.getExpanded()).isFalse();

        //intersection points traversed from xml are full points it seems...
        assertThat(flowLocationGraphicInfo)
                .extracting(GraphicInfo::getX, GraphicInfo::getY)
                .containsExactly(
                        tuple(102.0, 111.0),
                        tuple(165.0, 112.0)
                );

        //validate graphic infos
        FlowElement flowElement = bpmnModel.getFlowElement(IN_CSB_START_EVENT);
        assertThat(flowElement).isInstanceOf(StartEvent.class);

        gi = bpmnModel.getGraphicInfo(IN_CSB_START_EVENT);
        assertThat(gi.getX()).isEqualTo(90.0);
        assertThat(gi.getY()).isEqualTo(135.0);
        assertThat(gi.getWidth()).isEqualTo(30.0);
        assertThat(gi.getHeight()).isEqualTo(30.0);

        flowElement = bpmnModel.getFlowElement(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
        assertThat(flowElement).isInstanceOf(SequenceFlow.class);
        assertThat(flowElement.getName()).isEqualTo("to ut");

        flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(IN_CSB_SEQUENCEFLOW_TO_USERTASK);
        assertThat(flowLocationGraphicInfo)
                .extracting(GraphicInfo::getX, GraphicInfo::getY)
                .containsExactly(
                        tuple(120.0, 150.0),
                        tuple(232.0, 150.0)
                );

        flowElement = bpmnModel.getFlowElement(IN_CSB_USERTASK);
        assertThat(flowElement).isInstanceOf(UserTask.class);
        assertThat(flowElement.getName()).isEqualTo("User task 1");

        gi = bpmnModel.getGraphicInfo(IN_CSB_USERTASK);
        assertThat(gi.getX()).isEqualTo(232.0);
        assertThat(gi.getY()).isEqualTo(110.0);
        assertThat(gi.getWidth()).isEqualTo(100.0);
        assertThat(gi.getHeight()).isEqualTo(80.0);

        flowElement = bpmnModel.getFlowElement(IN_CSB_SEQUENCEFLOW_TO_END);
        assertThat(flowElement).isInstanceOf(SequenceFlow.class);
        assertThat(flowElement.getName()).isEqualTo("to end");

        flowLocationGraphicInfo = bpmnModel.getFlowLocationGraphicInfo(IN_CSB_SEQUENCEFLOW_TO_END);
        assertThat(flowLocationGraphicInfo)
                .extracting(GraphicInfo::getX, GraphicInfo::getY)
                .containsExactly(
                        tuple(332.0, 150.0),
                        tuple(435.0, 150.0)
                );

    }

    @Override
    protected String getResource() {
        return "collapsed-subprocess.bpmn20.xml";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	private void validateGraphicInfo(BpmnModel model) throws Exception {
    	Process process = model.getMainProcess();
    	List<SubProcess> subList = process.findFlowElementsOfType(SubProcess.class);
    	Map<String, List<GraphicInfo>> flowLocationMap = model.getFlowLocationMap();
    	Map<String, GraphicInfo> locationMap = model.getLocationMap();

    	// BPMNDI data
        Map<String, Map> baseBpmnDI = parseBPMNDI(new BpmnXMLConverter().convertToXML(model));

        // verify sequence flows/edges
        assertThat(flowLocationMap).hasSize(6);

        // verify other elements/shapes
        assertThat(locationMap).hasSize(8);

        // verify BPMNDI data
        // should have 2 diagrams
        assertThat(baseBpmnDI).hasSize(2);

        // subprocess diagram
        assertThat(subList).hasSize(1);
        Map<String, List<GraphicInfo>> multiMainEdgeMap = (Map<String, List<GraphicInfo>>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_EDGE);
        Map<String, GraphicInfo> multiMainShapeMap = (Map<String, GraphicInfo>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_SHAPE);
        Map<String, List<GraphicInfo>> multiSubEdgeMap = (Map<String, List<GraphicInfo>>) baseBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_EDGE);
        Map<String, GraphicInfo> multiSubShapeMap = (Map<String, GraphicInfo>) baseBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_SHAPE);

        assertThat(multiMainEdgeMap).hasSize(3);
        assertThat(multiMainShapeMap).hasSize(4);
        assertThat(multiSubEdgeMap).hasSize(3);
        assertThat(multiSubShapeMap).hasSize(4);

        // verify annotations are in correct diagram
        assertThat(multiMainShapeMap).containsKey("textannotation1");
        assertThat(multiSubShapeMap).containsKey("textannotation2");
        assertThat(multiMainEdgeMap).containsKey("association1");
        assertThat(multiSubEdgeMap).containsKey("association2");

        // verify sequence flows/edges
        List<GraphicInfo> info;
        List<GraphicInfo> diInfo;
        for (String id : flowLocationMap.keySet()) {
            info = new ArrayList<>(flowLocationMap.get(id));
            diInfo = multiMainEdgeMap.get(id);
            // if not found in main process, must be in subprocess
            if (diInfo == null) {
                diInfo = multiSubEdgeMap.get(id);
            }
            assertThat(diInfo).hasSameSizeAs(info);
            compareCollections(info, diInfo);
        }

    	// verify other elements/shapes
        GraphicInfo shapeInfo;
		for (String id : locationMap.keySet()) {
			// compare graphic info for each element
			shapeInfo = multiMainShapeMap.get(id);
    		// if not found in main process, must be in subprocess
			if (shapeInfo == null) {
				shapeInfo = multiSubShapeMap.get(id);
			}
            assertThat(locationMap.get(id).equals(shapeInfo));
        }
    }

}

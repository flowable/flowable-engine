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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Artifact;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.TextAnnotation;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.junit.Test;

public class SubProcessMultiDiagramConverterTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
        validateGraphicInfo(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
        validateGraphicInfo(parsedModel);
//        deployProcess(parsedModel);
    }

    @Override
    protected String getResource() {
        return "subprocessmultidiagrammodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
    	Process process = model.getMainProcess();
    	Collection<Artifact> artifacts = process.getArtifacts();
        List<ValuedDataObject> dataObjects = process.getDataObjects();

    	// verify main process
        assertEquals(13, process.getFlowElements().size());
        assertEquals(2, artifacts.size());
        assertEquals(6, dataObjects.size());

        Artifact artifact = artifacts.iterator().next();
        assertNotNull(artifact);
        assertEquals("textannotation1", artifact.getId());
        assertTrue(artifact instanceof TextAnnotation);
        assertEquals("Test Annotation", ((TextAnnotation) artifact).getText());

        FlowElement flowElement = process.getFlowElement("start1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);

        flowElement = process.getFlowElement("userTask1");
        assertNotNull(flowElement);
        assertEquals("User task 1", flowElement.getName());
        assertTrue(flowElement instanceof UserTask);
        UserTask userTask = (UserTask) flowElement;
        assertEquals(1, userTask.getCandidateUsers().size());
        assertEquals(1, userTask.getCandidateGroups().size());
        
        flowElement = process.getFlowElement("subprocess1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SubProcess);

        // verify subprocess
        SubProcess subProcess = (SubProcess) flowElement;
        artifacts = subProcess.getArtifacts();
        dataObjects = subProcess.getDataObjects();

        assertEquals(11, subProcess.getFlowElements().size());
        assertEquals(2, artifacts.size());
        assertEquals(6, dataObjects.size());
        
        artifact = artifacts.iterator().next();
        assertNotNull(artifact);
        assertEquals("textannotation2", artifact.getId());
        assertTrue(artifact instanceof TextAnnotation);
        assertEquals("Sub Test Annotation", ((TextAnnotation) artifact).getText());

        flowElement = subProcess.getFlowElement("subStartEvent");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);

        flowElement = subProcess.getFlowElement("subUserTask1");
        assertNotNull(flowElement);
        assertEquals("User task 2", flowElement.getName());
        assertTrue(flowElement instanceof UserTask);
        userTask = (UserTask) flowElement;
        assertEquals(0, userTask.getCandidateUsers().size());
        assertEquals(0, userTask.getCandidateGroups().size());
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
        assertEquals(7, flowLocationMap.size());

    	// verify other elements/shapes
        assertEquals(9, locationMap.size());

        // verify BPMNDI data
    	// should have 2 diagrams
    	assertEquals(2, baseBpmnDI.size());

		// subprocess diagram
        assertEquals(1, subList.size());
    	Map<String, List<GraphicInfo>> multiMainEdgeMap = (Map<String, List<GraphicInfo>>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_EDGE);
    	Map<String, GraphicInfo> multiMainShapeMap = (Map<String, GraphicInfo>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_SHAPE);
    	Map<String, List<GraphicInfo>> multiSubEdgeMap = (Map<String, List<GraphicInfo>>) baseBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_EDGE);
        Map<String, GraphicInfo> multiSubShapeMap = (Map<String, GraphicInfo>) baseBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_SHAPE);

    	assertEquals(4, multiMainEdgeMap.size());
    	assertEquals(5, multiMainShapeMap.size());
    	assertEquals(3, multiSubEdgeMap.size());
    	assertEquals(4, multiSubShapeMap.size());

    	// verify annotations are in correct diagram
    	assertTrue(multiMainShapeMap.containsKey("textannotation1"));
    	assertTrue(multiSubShapeMap.containsKey("textannotation2"));
    	assertTrue(multiMainEdgeMap.containsKey("association1"));
    	assertTrue(multiSubEdgeMap.containsKey("association2"));

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
    		assertEquals(info.size(), diInfo.size());
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
        	assertTrue(locationMap.get(id).equals(shapeInfo));
        }
    }
}

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.converter.SubprocessXMLConverter;
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

    @Override
    protected BpmnModel readXMLFile() throws Exception {
        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream(getResource());
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(xmlStream, "UTF-8");
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        return new SubprocessXMLConverter().convertToBpmnModel(xtr);
    }

    @Override
    protected BpmnModel exportAndReadXMLFile(BpmnModel bpmnModel) throws Exception {
        byte[] xml = new SubprocessXMLConverter().convertToXML(bpmnModel);
        System.out.println("xml " + new String(xml, "UTF-8"));
        XMLInputFactory xif = XMLInputFactory.newInstance();
        InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(xml), "UTF-8");
        XMLStreamReader xtr = xif.createXMLStreamReader(in);
        return new SubprocessXMLConverter().convertToBpmnModel(xtr);
    }

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
        validateGraphicInfo(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
        validateGraphicInfo(parsedModel);
        deployProcess(parsedModel);
    }

    protected String getResource() {
        return "subprocessmultidiagrammodel.bpmn";
    }

    private void validateModel(BpmnModel model) {
    	Process process = model.getMainProcess();
    	Collection<Artifact> artifacts = process.getArtifacts();
        List<ValuedDataObject> dataObjects = process.getDataObjects();

    	// verify main process
        assertEquals(13, process.getFlowElements().size());
        assertEquals(1, artifacts.size());
        assertEquals(6, dataObjects.size());

        Artifact artifact = artifacts.iterator().next();
        assertNotNull(artifact);
        assertEquals("textannotation1", artifact.getId());
        assertTrue(artifact instanceof TextAnnotation);
        assertEquals("Text Annotation", ((TextAnnotation) artifact).getText());

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
        assertEquals(1, artifacts.size());
        assertEquals(6, dataObjects.size());
        
        artifact = artifacts.iterator().next();
        assertNotNull(artifact);
        assertEquals("textannotation2", artifact.getId());
        assertTrue(artifact instanceof TextAnnotation);
        assertEquals("Sub Text Annotation", ((TextAnnotation) artifact).getText());

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
    	Map<String, Map> multiBpmnDI = parseBPMNDI(new SubprocessXMLConverter().convertToXML(model));
    	Map<String, List<GraphicInfo>> baseEdgeMap = (Map<String, List<GraphicInfo>>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_EDGE);
    	Map<String, GraphicInfo> baseShapeMap = (Map<String, GraphicInfo>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_SHAPE);
    	
        // verify sequence flows/edges
        assertEquals(5, flowLocationMap.size());
        List<GraphicInfo> info, diInfo;
    	for (String id : flowLocationMap.keySet()) {
    		info = new ArrayList<GraphicInfo>(flowLocationMap.get(id));
    		diInfo = baseEdgeMap.get(id);

    		assertEquals(info.size(), diInfo.size());
    		compareCollections(info, diInfo);
    	}

    	// verify other elements/shapes
        assertEquals(9, locationMap.size());
        assertEquals(locationMap.size(), baseShapeMap.size());
		for (String id : locationMap.keySet()) {
			// compare graphic info for each element
        	assertTrue(locationMap.get(id).equals(baseShapeMap.get(id)));
        }
    	
    	// verify BPMNDI data
    	// should have 1 diagram
    	assertEquals(1, baseBpmnDI.size());

    	// should have 2 diagrams
    	assertEquals(2, multiBpmnDI.size());

		// subprocess diagram
        assertEquals(1, subList.size());
    	Map<String, List<GraphicInfo>> multiMainEdgeMap = (Map<String, List<GraphicInfo>>) multiBpmnDI.get(process.getId()).get(ELEMENT_DI_EDGE);
    	Map<String, GraphicInfo> multiMainShapeMap = (Map<String, GraphicInfo>) multiBpmnDI.get(process.getId()).get(ELEMENT_DI_SHAPE);
    	Map<String, List<GraphicInfo>> multiSubEdgeMap = (Map<String, List<GraphicInfo>>) multiBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_EDGE);;
    	Map<String, GraphicInfo> multiSubShapeMap = (Map<String, GraphicInfo>) multiBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_SHAPE);

    	assertEquals(3, multiMainEdgeMap.size());
    	assertEquals(5, multiMainShapeMap.size());
    	assertEquals(2, multiSubEdgeMap.size());
    	assertEquals(4, multiSubShapeMap.size());
    	
    	// verify annotations are in correct diagram
    	assertTrue(multiMainShapeMap.containsKey("textannotation1"));
    	assertTrue(multiSubShapeMap.containsKey("textannotation2"));

    	// verify sequence flows/edges
    	for (String id : flowLocationMap.keySet()) {
    		info = new ArrayList<GraphicInfo>(flowLocationMap.get(id));
    		diInfo = multiMainEdgeMap.get(id);
    		// if not found in main process, must be in subprocess
    		if (null == diInfo) {
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
			if (null == shapeInfo) {
				shapeInfo = multiSubShapeMap.get(id);
			}
        	assertTrue(locationMap.get(id).equals(shapeInfo));
        }
    }

    private void compareCollections(List<GraphicInfo> info, List<GraphicInfo> diInfo) {
		boolean foundMatch = false;
		for (GraphicInfo ginfo : info) {
			for (GraphicInfo gdInfo : diInfo) {
				// entries may not be in the same order
				if (foundMatch == true) {
					// found one match so reset and try next set of values
					info.remove(ginfo);
					foundMatch = false;
					continue;
				} else {
					assertTrue(ginfo.equals(gdInfo));
					foundMatch = true;
				}
			}
		}
    }
}

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
import org.junit.jupiter.api.Test;

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
        assertThat(process.getFlowElements()).hasSize(13);
        assertThat(artifacts).hasSize(2);
        assertThat(dataObjects).hasSize(6);

        Artifact artifact = artifacts.iterator().next();
        assertThat(artifact)
                .isInstanceOfSatisfying(TextAnnotation.class, art -> {
                    assertThat(art.getId()).isEqualTo("textannotation1");
                    assertThat(art.getText()).isEqualTo("Test Annotation");
                });

        FlowElement flowElement = process.getFlowElement("start1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(StartEvent.class, startEvent -> {
                    assertThat(startEvent.getId()).isEqualTo("start1");
                });

        flowElement = process.getFlowElement("userTask1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getName()).isEqualTo("User task 1");
                    assertThat(userTask.getCandidateUsers()).hasSize(1);
                    assertThat(userTask.getCandidateGroups()).hasSize(1);
                });

        flowElement = process.getFlowElement("subprocess1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SubProcess.class, subProcess -> {
                    assertThat(subProcess.getId()).isEqualTo("subprocess1");
                    assertThat(subProcess.getFlowElements()).hasSize(11);

                    // verify subprocess
                    assertThat(subProcess.getArtifacts()).hasSize(2);
                    assertThat(subProcess.getDataObjects()).hasSize(6);

                    assertThat(subProcess.getArtifacts().iterator().next())
                            .isInstanceOfSatisfying(TextAnnotation.class, art -> {
                                assertThat(art.getId()).isEqualTo("textannotation2");
                                assertThat(art.getText()).isEqualTo("Sub Test Annotation");
                            });

                    assertThat(subProcess.getFlowElement("subStartEvent"))
                            .isInstanceOfSatisfying(StartEvent.class, startEvent -> {
                                assertThat(startEvent.getId()).isEqualTo("subStartEvent");
                            });

                    assertThat(subProcess.getFlowElement("subUserTask1"))
                            .isInstanceOfSatisfying(UserTask.class, userTask -> {
                                assertThat(userTask.getName()).isEqualTo("User task 2");
                                assertThat(userTask.getCandidateUsers()).isEmpty();
                                assertThat(userTask.getCandidateGroups()).isEmpty();
                            });
                });
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
        assertThat(flowLocationMap).hasSize(7);

        // verify other elements/shapes
        assertThat(locationMap).hasSize(9);

        // verify BPMNDI data should have 2 diagrams
        assertThat(baseBpmnDI).hasSize(2);

        // subprocess diagram
        assertThat(subList).hasSize(1);
        Map<String, List<GraphicInfo>> multiMainEdgeMap = (Map<String, List<GraphicInfo>>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_EDGE);
        Map<String, GraphicInfo> multiMainShapeMap = (Map<String, GraphicInfo>) baseBpmnDI.get(process.getId()).get(ELEMENT_DI_SHAPE);
        Map<String, List<GraphicInfo>> multiSubEdgeMap = (Map<String, List<GraphicInfo>>) baseBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_EDGE);
        Map<String, GraphicInfo> multiSubShapeMap = (Map<String, GraphicInfo>) baseBpmnDI.get(subList.get(0).getId()).get(ELEMENT_DI_SHAPE);

        assertThat(multiMainEdgeMap).hasSize(4);
        assertThat(multiMainShapeMap).hasSize(5);
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
            assertThat(locationMap.get(id).equals(shapeInfo)).isTrue();
        }
    }
}

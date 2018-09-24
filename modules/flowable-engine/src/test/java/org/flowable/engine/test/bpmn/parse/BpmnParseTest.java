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

package org.flowable.engine.test.bpmn.parse;

import java.util.List;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.test.TestHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Joram Barrez
 */
public class BpmnParseTest extends PluggableFlowableTestCase {

    @Test
    public void testInvalidProcessDefinition() {
        try {
            String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testInvalidProcessDefinition");
            repositoryService.createDeployment().name(resource).addClasspathResource(resource).deploy();
            fail();
        } catch (XMLException e) {
            // expected exception
        }
    }

    @Test
    public void testParseWithBpmnNamespacePrefix() {
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/parse/BpmnParseTest.testParseWithBpmnNamespacePrefix.bpmn20.xml").deploy();
        assertEquals(1, repositoryService.createProcessDefinitionQuery().count());

        repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
    }

    @Test
    public void testParseWithMultipleDocumentation() {
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/parse/BpmnParseTest.testParseWithMultipleDocumentation.bpmn20.xml").deploy();
        assertEquals(1, repositoryService.createProcessDefinitionQuery().count());

        repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
    }

    @Test
    @Deployment
    public void testParseDiagramInterchangeElements() {

        // Graphical information is not yet exposed publicly, so we need to do some plumbing

        BpmnModel bpmnModel = repositoryService.getBpmnModel(repositoryService.createProcessDefinitionQuery().singleResult().getId());

        // Check if diagram has been created based on Diagram Interchange when it's not a headless instance
        List<String> resourceNames = repositoryService.getDeploymentResourceNames(repositoryService.createProcessDefinitionQuery().singleResult().getDeploymentId());
        assertEquals(2, resourceNames.size());

        assertActivityBounds(bpmnModel, "theStart", 70, 255, 30, 30);
        assertActivityBounds(bpmnModel, "task1", 176, 230, 100, 80);
        assertActivityBounds(bpmnModel, "gateway1", 340, 250, 40, 40);
        assertActivityBounds(bpmnModel, "task2", 445, 138, 100, 80);
        assertActivityBounds(bpmnModel, "gateway2", 620, 250, 40, 40);
        assertActivityBounds(bpmnModel, "task3", 453, 304, 100, 80);
        assertActivityBounds(bpmnModel, "theEnd", 713, 256, 28, 28);

        assertSequenceFlowWayPoints(bpmnModel, "flowStartToTask1", 100, 270, 176, 270);
        assertSequenceFlowWayPoints(bpmnModel, "flowTask1ToGateway1", 276, 270, 340, 270);
        assertSequenceFlowWayPoints(bpmnModel, "flowGateway1ToTask2", 360, 250, 360, 178, 445, 178);
        assertSequenceFlowWayPoints(bpmnModel, "flowGateway1ToTask3", 360, 290, 360, 344, 453, 344);
        assertSequenceFlowWayPoints(bpmnModel, "flowTask2ToGateway2", 545, 178, 640, 178, 640, 250);
        assertSequenceFlowWayPoints(bpmnModel, "flowTask3ToGateway2", 553, 344, 640, 344, 640, 290);
        assertSequenceFlowWayPoints(bpmnModel, "flowGateway2ToEnd", 660, 270, 713, 270);

    }

    @Test
    @Deployment
    public void testParseNamespaceInConditionExpressionType() {

        BpmnModel bpmnModel = repositoryService.getBpmnModel(repositoryService.createProcessDefinitionQuery().singleResult().getId());
        Process process = bpmnModel.getProcesses().get(0);
        assertNotNull(process);

        SequenceFlow sequenceFlow = (SequenceFlow) process.getFlowElement("SequenceFlow_3");
        assertEquals("#{approved}", sequenceFlow.getConditionExpression());

        sequenceFlow = (SequenceFlow) process.getFlowElement("SequenceFlow_4");
        assertEquals("#{!approved}", sequenceFlow.getConditionExpression());

    }

    @Test
    @Deployment
    public void testParseDiagramInterchangeElementsForUnknownModelElements() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("TestAnnotation").singleResult();
        BpmnModel model = repositoryService.getBpmnModel(processDefinition.getId());
        Process mainProcess = model.getMainProcess();
        assertEquals(0, mainProcess.getExtensionElements().size());
    }

    @Test
    public void testParseSwitchedSourceAndTargetRefsForAssociations() {
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/parse/BpmnParseTest.testParseSwitchedSourceAndTargetRefsForAssociations.bpmn20.xml").deploy();

        assertEquals(1, repositoryService.createProcessDefinitionQuery().count());

        repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
    }

    protected void assertActivityBounds(BpmnModel bpmnModel, String activityId, Integer x, Integer y, Integer width, Integer height) {
        assertEquals(x.doubleValue(), bpmnModel.getGraphicInfo(activityId).getX());
        assertEquals(y.doubleValue(), bpmnModel.getGraphicInfo(activityId).getY());
        assertEquals(width.doubleValue(), bpmnModel.getGraphicInfo(activityId).getWidth());
        assertEquals(height.doubleValue(), bpmnModel.getGraphicInfo(activityId).getHeight());
    }

    protected void assertSequenceFlowWayPoints(BpmnModel bpmnModel, String sequenceFlowId, Integer... waypoints) {
        List<GraphicInfo> graphicInfos = bpmnModel.getFlowLocationGraphicInfo(sequenceFlowId);
        assertEquals(waypoints.length / 2, graphicInfos.size());
        for (int i = 0; i < waypoints.length; i += 2) {
            Integer x = waypoints[i];
            Integer y = waypoints[i + 1];
            assertEquals(x.doubleValue(), graphicInfos.get(i / 2).getX());
            assertEquals(y.doubleValue(), graphicInfos.get(i / 2).getY());
        }
    }

}

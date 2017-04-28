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
package org.flowable.bpm.model.bpmn.builder.di;

import static org.flowable.bpm.model.bpmn.BpmnTestConstants.END_EVENT_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.SEQUENCE_FLOW_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.START_EVENT_ID;
import static org.flowable.bpm.model.bpmn.BpmnTestConstants.USER_TASK_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flowable.bpm.model.bpmn.BpmnModelBuilder;
import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.ProcessBuilder;
import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

public class DiGeneratorForSequenceFlowsTest {

    private BpmnModelInstance instance;

    @After
    public void validateModel()
        throws IOException {
        if (instance != null) {
            BpmnModelBuilder.validateModel(instance);
        }
    }

    @Test
    public void shouldGenerateEdgeForSequenceFlow() {

        ProcessBuilder builder = BpmnModelBuilder.createExecutableProcess();

        instance = builder
                .startEvent(START_EVENT_ID)
                .sequenceFlowId(SEQUENCE_FLOW_ID)
                .endEvent(END_EVENT_ID)
                .done();

        Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
        assertEquals(1, allEdges.size());

        assertBpmnEdgeExists(SEQUENCE_FLOW_ID);
    }

    @Test
    public void shouldGenerateEdgesForSequenceFlowsUsingGateway() {

        ProcessBuilder builder = BpmnModelBuilder.createExecutableProcess();

        instance = builder
                .startEvent(START_EVENT_ID)
                .sequenceFlowId("s1")
                .parallelGateway("gateway")
                .sequenceFlowId("s2")
                .endEvent("e1")
                .moveToLastGateway()
                .sequenceFlowId("s3")
                .endEvent("e2")
                .done();

        Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
        assertEquals(3, allEdges.size());

        assertBpmnEdgeExists("s1");
        assertBpmnEdgeExists("s2");
        assertBpmnEdgeExists("s3");
    }

    @Test
    public void shouldGenerateEdgesWhenUsingMoveToActivity() {

        ProcessBuilder builder = BpmnModelBuilder.createExecutableProcess();

        instance = builder
                .startEvent(START_EVENT_ID)
                .sequenceFlowId("s1")
                .exclusiveGateway()
                .sequenceFlowId("s2")
                .userTask(USER_TASK_ID)
                .sequenceFlowId("s3")
                .endEvent("e1")
                .moveToActivity(USER_TASK_ID)
                .sequenceFlowId("s4")
                .endEvent("e2")
                .done();

        Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
        assertEquals(4, allEdges.size());

        assertBpmnEdgeExists("s1");
        assertBpmnEdgeExists("s2");
        assertBpmnEdgeExists("s3");
        assertBpmnEdgeExists("s4");
    }

    @Test
    public void shouldGenerateEdgesWhenUsingMoveToNode() {

        ProcessBuilder builder = BpmnModelBuilder.createExecutableProcess();

        instance = builder
                .startEvent(START_EVENT_ID)
                .sequenceFlowId("s1")
                .exclusiveGateway()
                .sequenceFlowId("s2")
                .userTask(USER_TASK_ID)
                .sequenceFlowId("s3")
                .endEvent("e1")
                .moveToNode(USER_TASK_ID)
                .sequenceFlowId("s4")
                .endEvent("e2")
                .done();

        Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
        assertEquals(4, allEdges.size());

        assertBpmnEdgeExists("s1");
        assertBpmnEdgeExists("s2");
        assertBpmnEdgeExists("s3");
        assertBpmnEdgeExists("s4");
    }

    @Test
    public void shouldGenerateEdgesWhenUsingConnectTo() {

        ProcessBuilder builder = BpmnModelBuilder.createExecutableProcess();

        instance = builder
                .startEvent(START_EVENT_ID)
                .sequenceFlowId("s1")
                .exclusiveGateway("gateway")
                .sequenceFlowId("s2")
                .userTask(USER_TASK_ID)
                .sequenceFlowId("s3")
                .endEvent(END_EVENT_ID)
                .moveToNode(USER_TASK_ID)
                .sequenceFlowId("s4")
                .connectTo("gateway")
                .done();

        Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);
        assertEquals(4, allEdges.size());

        assertBpmnEdgeExists("s1");
        assertBpmnEdgeExists("s2");
        assertBpmnEdgeExists("s3");
        assertBpmnEdgeExists("s4");
    }

    protected BpmnEdge findBpmnEdge(String sequenceFlowId) {
        Collection<BpmnEdge> allEdges = instance.getModelElementsByType(BpmnEdge.class);

        for (BpmnEdge edge : allEdges) {
            if (edge.getBpmnElement().getId().equals(sequenceFlowId)) {
                return edge;
            }
        }
        return null;
    }

    protected void assertBpmnEdgeExists(String id) {
        BpmnEdge edge = findBpmnEdge(id);
        assertNotNull(edge);
    }
}

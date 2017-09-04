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
package org.flowable.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.junit.Test;

public class PoolConverterTest extends AbstractConverterTest {

    @Test
    public void convertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "test.poolmodel.json";
    }

    private void validateModel(BpmnModel model) {

        String idPool = "idPool";
        String idProcess = "poolProcess";

        assertEquals(1, model.getPools().size());

        Pool pool = model.getPool(idPool);
        assertEquals(idPool, pool.getId());
        assertEquals(idProcess, pool.getProcessRef());
        assertTrue(pool.isExecutable());

        Process process = model.getProcess(idPool);
        assertEquals(idProcess, process.getId());
        assertTrue(process.isExecutable());
        assertEquals(3, process.getLanes().size());

        Lane lane = process.getLanes().get(0);
        assertEquals("idLane1", lane.getId());
        assertEquals("Lane 1", lane.getName());
        assertEquals(7, lane.getFlowReferences().size());
        assertTrue(lane.getFlowReferences().contains("startevent"));
        assertTrue(lane.getFlowReferences().contains("usertask1"));
        assertTrue(lane.getFlowReferences().contains("usertask6"));
        assertTrue(lane.getFlowReferences().contains("endevent"));

        lane = process.getLanes().get(1);
        assertEquals("idLane2", lane.getId());
        assertEquals("Lane 2", lane.getName());
        assertEquals(4, lane.getFlowReferences().size());
        assertTrue(lane.getFlowReferences().contains("usertask2"));
        assertTrue(lane.getFlowReferences().contains("usertask5"));

        lane = process.getLanes().get(2);
        assertEquals("idLane3", lane.getId());
        assertEquals("Lane 3", lane.getName());
        assertEquals(4, lane.getFlowReferences().size());
        assertTrue(lane.getFlowReferences().contains("usertask3"));
        assertTrue(lane.getFlowReferences().contains("usertask4"));

        assertNotNull(process.getFlowElement("startevent", true));
        assertNotNull(process.getFlowElement("usertask1", true));
        assertNotNull(process.getFlowElement("usertask2", true));
        assertNotNull(process.getFlowElement("usertask3", true));
        assertNotNull(process.getFlowElement("usertask4", true));
        assertNotNull(process.getFlowElement("usertask5", true));
        assertNotNull(process.getFlowElement("usertask6", true));
        assertNotNull(process.getFlowElement("endevent", true));

        assertNotNull(process.getFlowElement("flow1", true));
        assertNotNull(process.getFlowElement("flow2", true));
        assertNotNull(process.getFlowElement("flow3", true));
        assertNotNull(process.getFlowElement("flow4", true));
        assertNotNull(process.getFlowElement("flow5", true));
        assertNotNull(process.getFlowElement("flow6", true));
        assertNotNull(process.getFlowElement("flow7", true));
    }
}

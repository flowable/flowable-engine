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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

public class SubProcessMultiDiagramConverterNoDITest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "subprocessmultidiagrammodel-noDI.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("start1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);
        assertEquals("start1", flowElement.getId());

        flowElement = model.getMainProcess().getFlowElement("userTask1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof UserTask);
        assertEquals("userTask1", flowElement.getId());
        UserTask userTask = (UserTask) flowElement;
        assertEquals(1, userTask.getCandidateUsers().size());
        assertEquals(1, userTask.getCandidateGroups().size());

        flowElement = model.getMainProcess().getFlowElement("subprocess1");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SubProcess);
        assertEquals("subprocess1", flowElement.getId());
        SubProcess subProcess = (SubProcess) flowElement;
        assertEquals(11, subProcess.getFlowElements().size());
    }
}

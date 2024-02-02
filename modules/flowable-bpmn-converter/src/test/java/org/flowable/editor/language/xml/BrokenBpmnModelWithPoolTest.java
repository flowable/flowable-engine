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
import static org.flowable.editor.language.xml.util.XmlTestUtils.exportAndReadXMLFile;

import java.util.Arrays;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.junit.jupiter.api.Test;

class BrokenBpmnModelWithPoolTest {

    @Test
    public void getMainProcessWithBrokenPool() {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId("myProcess");
        bpmnModel.addProcess(process);
        Pool brokenPool = new Pool();
        brokenPool.setProcessRef("wrongProcessRef");
        brokenPool.setId("cloudpool1");
        brokenPool.setExecutable(true);

        bpmnModel.setPools(Arrays.asList(brokenPool));

        StartEvent startEvent = new StartEvent();
        startEvent.setId("event1");
        EndEvent endEvent = new EndEvent();
        endEvent.setId("event2");

        SequenceFlow flow = new SequenceFlow("event1", "event2");

        process.addFlowElement(startEvent);
        process.addFlowElement(endEvent);
        process.addFlowElement(flow);

        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        Process mainProcess = parsedModel.getMainProcess();

        assertThat(mainProcess).isNotNull();
    }
    
    @Test
    public void getMainProcessWithTwoBrokenPools() {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId("myProcess");
        process.setExecutable(false);
        bpmnModel.addProcess(process);
        Pool brokenPool = new Pool();
        brokenPool.setProcessRef("wrongProcessRef");
        brokenPool.setId("cloudpool1");
        brokenPool.setExecutable(false);
        
        Process process2 = new Process();
        process2.setId("myProcess2");
        bpmnModel.addProcess(process2);
        Pool brokenPool2 = new Pool();
        brokenPool2.setProcessRef("wrongProcessRef2");
        brokenPool2.setId("cloudpool2");
        brokenPool2.setExecutable(true);

        bpmnModel.setPools(Arrays.asList(brokenPool, brokenPool2));
        
        StartEvent process1StartEvent = new StartEvent();
        process1StartEvent.setId("process1Event1");
        process.addFlowElement(process1StartEvent);

        StartEvent startEvent = new StartEvent();
        startEvent.setId("event1");
        EndEvent endEvent = new EndEvent();
        endEvent.setId("event2");

        SequenceFlow flow = new SequenceFlow("event1", "event2");

        process2.addFlowElement(startEvent);
        process2.addFlowElement(endEvent);
        process2.addFlowElement(flow);

        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        Process mainProcess = parsedModel.getMainProcess();

        assertThat(mainProcess).isNotNull();
        assertThat(mainProcess.getFlowElements()).hasSize(3);
    }
}

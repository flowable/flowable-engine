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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.junit.jupiter.api.Test;

/**
 * Verifies if the sequenceflows are correctly stored when a subprocess is inside
 * and collapsible subprocess.
 * <p>
 * Created by Pardo David on 1/03/2017.
 */
public class CollapsedSubWithSubSequenceFlowTest extends AbstractConverterTest {

    private static final String EXPANED_SUBPROCESS_IN_CP = "sid-65F96E4B-9E0D-462D-AFD4-3FFAD5F7F9B6";
    private static final String COLLAPSED_SUBPROCESS = "sid-44B96119-5A3B-4850-BDAC-2D4A2AECEA0A";

    @Test
    public void oneWay() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void twoWay() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    private void validateModel(BpmnModel model) {
        //It appears that the sequenceflows of the expanded subprocess are also stored as a child of the collapsed subprocess...
        //this lead to duplications when writing back to json...
        SubProcess collapsedSubprocess = (SubProcess) model.getFlowElement(COLLAPSED_SUBPROCESS);
        Collection<FlowElement> flowElements = collapsedSubprocess.getFlowElements();
        assertThat(flowElements).hasSize(5); //start - sf - sub - sf - end

        SubProcess subProcess = (SubProcess) model.getFlowElement(EXPANED_SUBPROCESS_IN_CP);
        assertThat(subProcess.getFlowElements()).hasSize(5); //start-sf-task-sf-end

    }

    @Override
    protected String getResource() {
        return "test.sequenceflow-in-collapsed-subprocess.json";
    }
}

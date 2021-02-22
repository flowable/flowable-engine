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
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.junit.jupiter.api.Test;

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

        assertThat(model.getPools())
                .extracting(Pool::getId, Pool::getProcessRef, Pool::isExecutable)
                .containsExactly(tuple(idPool, idProcess, true));

        Process process = model.getProcess(idPool);
        assertThat(process.getId()).isEqualTo(idProcess);
        assertThat(process.isExecutable()).isTrue();

        assertThat(process.getLanes())
                .extracting(Lane::getId, Lane::getName)
                .containsExactly(
                        tuple("idLane1", "Lane 1"),
                        tuple("idLane2", "Lane 2"),
                        tuple("idLane3", "Lane 3")
                );

        Lane lane = process.getLanes().get(0);
        assertThat(lane.getFlowReferences())
                .hasSize(7)
                .contains("startevent", "usertask1", "usertask6", "endevent");

        lane = process.getLanes().get(1);
        assertThat(lane.getFlowReferences())
                .hasSize(4)
                .contains("usertask2", "usertask5");

        lane = process.getLanes().get(2);
        assertThat(lane.getFlowReferences())
                .hasSize(4)
                .contains("usertask3", "usertask4");

        assertThat(process.getFlowElement("startevent", true)).isNotNull();
        assertThat(process.getFlowElement("usertask1", true)).isNotNull();
        assertThat(process.getFlowElement("usertask2", true)).isNotNull();
        assertThat(process.getFlowElement("usertask3", true)).isNotNull();
        assertThat(process.getFlowElement("usertask4", true)).isNotNull();
        assertThat(process.getFlowElement("usertask5", true)).isNotNull();
        assertThat(process.getFlowElement("usertask6", true)).isNotNull();
        assertThat(process.getFlowElement("endevent", true)).isNotNull();

        assertThat(process.getFlowElement("flow1", true)).isNotNull();
        assertThat(process.getFlowElement("flow2", true)).isNotNull();
        assertThat(process.getFlowElement("flow3", true)).isNotNull();
        assertThat(process.getFlowElement("flow4", true)).isNotNull();
        assertThat(process.getFlowElement("flow5", true)).isNotNull();
        assertThat(process.getFlowElement("flow6", true)).isNotNull();
        assertThat(process.getFlowElement("flow7", true)).isNotNull();
    }
}

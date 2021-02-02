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
import static org.assertj.core.api.Assertions.tuple;

import java.io.InputStream;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Pool;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;
import org.junit.jupiter.api.Test;

class PoolsConverterTest {

    @Test
    public void convertXMLToModel2() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("pool-with-extensions.bpmn")) {
            new BpmnXMLConverter().convertToBpmnModel(
                    new InputStreamSource(is), true, true, "UTF-8");
        }
    }

    @BpmnXmlConverterTest("pools.bpmn")
    void validateModel(BpmnModel model) {
        assertThat(model.getPools())
                .extracting(Pool::getId, Pool::getName)
                .containsExactly(tuple("pool1", "Pool"));
        Pool pool = model.getPools().get(0);
        Process process = model.getProcess(pool.getId());
        assertThat(process.getLanes())
                .extracting(Lane::getId, Lane::getName)
                .containsExactly(
                        tuple("lane1", "Lane 1"),
                        tuple("lane2", "Lane 2")
                );
        assertThat(process.getLanes().get(0).getFlowReferences()).hasSize(2);
        assertThat(process.getLanes().get(1).getFlowReferences()).hasSize(2);

        FlowElement flowElement = process.getFlowElement("flow1");
        assertThat(flowElement).isInstanceOf(SequenceFlow.class);
    }
}

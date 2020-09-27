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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.HttpServiceTask;
import org.junit.jupiter.api.Test;

public class HttpServiceTaskWithParallelInSameTransactionConverterTest extends AbstractConverterTest {

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
        return "httpServiceTaskWithParallelInSameTransactionModel.bpmn";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("serviceTask1");
        assertThat(flowElement).isInstanceOf(HttpServiceTask.class);
        HttpServiceTask httpServiceTask = (HttpServiceTask) flowElement;
        assertThat(httpServiceTask.getParallelInSameTransaction()).isTrue();

        flowElement = model.getMainProcess().getFlowElement("serviceTask2");
        assertThat(flowElement).isInstanceOf(HttpServiceTask.class);
        httpServiceTask = (HttpServiceTask) flowElement;
        assertThat(httpServiceTask.getParallelInSameTransaction()).isFalse();
    }
}

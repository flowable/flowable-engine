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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.UserTask;
import org.junit.jupiter.api.Test;

/**
 * @author Zheng Ji
 */
public class GatewayAsncAndExclusiveConverterTest extends AbstractConverterTest {

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
        return "test.gatewayasyncandexclusive.json";
    }

    private void validateModel(BpmnModel model) {

        Gateway gateway1 = (Gateway) model.getMainProcess().getFlowElement("p1", true);
        assertThat(gateway1.isExclusive()).isTrue();
        assertThat(gateway1.isAsynchronous()).isTrue();

        Gateway gateway2 = (Gateway) model.getMainProcess().getFlowElement("p2", true);
        assertThat(gateway2.isExclusive()).isFalse();
        assertThat(gateway2.isAsynchronous()).isTrue();

        UserTask userTask1 = (UserTask) model.getMainProcess().getFlowElement("shareniu-1", true);
        assertThat(userTask1.isExclusive()).isFalse();
        assertThat(userTask1.isAsynchronous()).isFalse();

        UserTask userTask2 = (UserTask) model.getMainProcess().getFlowElement("shareniu-2", true);
        assertThat(userTask2.isExclusive()).isTrue();
        assertThat(userTask2.isAsynchronous()).isTrue();
    }

}

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

import org.flowable.bpmn.model.*;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * @author Zheng Ji
 */
public class GatewayAsncAndExclusiveConverterTest extends AbstractConverterTest {

    @Test
    public void convertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "test.gatewayasyncandexclusive.json";
    }

    private void validateModel(BpmnModel model) {

        Gateway gateway1 = (Gateway) model.getMainProcess().getFlowElement("p1", true);
        assertTrue(gateway1.isExclusive());
        assertTrue(gateway1.isAsynchronous());

        Gateway gateway2 = (Gateway) model.getMainProcess().getFlowElement("p2", true);
        assertFalse(gateway2.isExclusive());
        assertTrue(gateway2.isAsynchronous());

        UserTask userTask1 = (UserTask) model.getMainProcess().getFlowElement("shareniu-1", true);
        assertFalse(userTask1.isExclusive());
        assertFalse(userTask1.isAsynchronous());

        UserTask userTask2 = (UserTask) model.getMainProcess().getFlowElement("shareniu-2", true);
        assertTrue(userTask2.isExclusive());
        assertTrue(userTask2.isAsynchronous());
    }

}

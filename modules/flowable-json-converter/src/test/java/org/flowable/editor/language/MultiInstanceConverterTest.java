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

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiInstanceConverterTest extends AbstractConverterTest {

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

    protected String getResource() {
        return "test.multiinstance.json";
    }

    private void validateModel(BpmnModel model) {
        Activity activity = (Activity) model.getFlowElement("multi-instance");
        assertEquals(activity.getLoopCharacteristics().isSequential(), true);
        assertEquals(activity.getLoopCharacteristics().getLoopCardinality(), "3");
        assertEquals(activity.getLoopCharacteristics().getElementVariable(), "instanceVar");
        assertEquals(activity.getLoopCharacteristics().getInputDataItem(), "collection");
        assertEquals(activity.getLoopCharacteristics().getElementIndexVariable(), "index");
        assertEquals(activity.getLoopCharacteristics().getCompletionCondition(), "completionCondition");
    }

}

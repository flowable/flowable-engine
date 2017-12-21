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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DataStore;
import org.flowable.bpmn.model.MessageFlow;
import org.flowable.bpmn.model.Pool;
import org.junit.Test;

public class MessageFlowConverterTest extends AbstractConverterTest {

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
        return "messageflow.bpmn";
    }

    private void validateModel(BpmnModel model) {
        assertEquals(1, model.getDataStores().size());
        DataStore dataStore = model.getDataStore("DATASTORE_1");
        assertNotNull(dataStore);
        assertEquals("DATASTORE_1", dataStore.getId());
        assertEquals("test", dataStore.getName());
        assertEquals("ITEM_1", dataStore.getItemSubjectRef());

        MessageFlow messageFlow = model.getMessageFlow("MESSAGEFLOW_1");
        assertNotNull(messageFlow);
        assertEquals("test 1", messageFlow.getName());
        assertEquals("task1", messageFlow.getSourceRef());
        assertEquals("task2", messageFlow.getTargetRef());

        messageFlow = model.getMessageFlow("MESSAGEFLOW_2");
        assertNotNull(messageFlow);
        assertEquals("test 2", messageFlow.getName());
        assertEquals("task2", messageFlow.getSourceRef());
        assertEquals("task3", messageFlow.getTargetRef());

        assertEquals(2, model.getPools().size());
        Pool pool = model.getPools().get(0);
        assertEquals("participant1", pool.getId());
        assertEquals("Participant 1", pool.getName());
        assertEquals("PROCESS_1", pool.getProcessRef());

        pool = model.getPools().get(1);
        assertEquals("participant2", pool.getId());
        assertEquals("Participant 2", pool.getName());
        assertEquals("PROCESS_2", pool.getProcessRef());
    }
}

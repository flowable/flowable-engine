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
import org.flowable.bpmn.model.DataStore;
import org.flowable.bpmn.model.DataStoreReference;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Pool;
import org.junit.Test;

public class DataStoreConverterTest extends AbstractConverterTest {

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
        return "datastore.bpmn";
    }

    private void validateModel(BpmnModel model) {
        assertEquals(1, model.getDataStores().size());
        DataStore dataStore = model.getDataStore("DataStore_1");
        assertNotNull(dataStore);
        assertEquals("DataStore_1", dataStore.getId());
        assertEquals("test", dataStore.getDataState());
        assertEquals("Test Database", dataStore.getName());
        assertEquals("test", dataStore.getItemSubjectRef());

        FlowElement refElement = model.getFlowElement("DataStoreReference_1");
        assertNotNull(refElement);
        assertTrue(refElement instanceof DataStoreReference);

        assertEquals(1, model.getPools().size());
        Pool pool = model.getPools().get(0);
        assertEquals("pool1", pool.getId());
    }
}

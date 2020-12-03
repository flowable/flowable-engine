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
import org.flowable.bpmn.model.DataStore;
import org.flowable.bpmn.model.DataStoreReference;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Pool;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class DataStoreConverterTest {

    @BpmnXmlConverterTest("datastore.bpmn")
    void validateModel(BpmnModel model) {
        assertThat(model.getDataStores())
                .containsOnlyKeys("DataStore_1");
        DataStore dataStore = model.getDataStore("DataStore_1");
        assertThat(dataStore).isNotNull();
        assertThat(dataStore.getId()).isEqualTo("DataStore_1");
        assertThat(dataStore.getDataState()).isEqualTo("test");
        assertThat(dataStore.getName()).isEqualTo("Test Database");
        assertThat(dataStore.getItemSubjectRef()).isEqualTo("test");

        FlowElement refElement = model.getFlowElement("DataStoreReference_1");
        assertThat(refElement).isInstanceOf(DataStoreReference.class);

        assertThat(model.getPools())
                .extracting(Pool::getId)
                .containsExactly("pool1");
    }
}

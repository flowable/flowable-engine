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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DataStore;
import org.flowable.bpmn.model.MessageFlow;
import org.flowable.bpmn.model.Pool;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

class MessageFlowConverterTest {

    @BpmnXmlConverterTest("messageflow.bpmn")
    void validateModel(BpmnModel model) {
        assertThat(model.getDataStores()).hasSize(1);
        DataStore dataStore = model.getDataStore("DATASTORE_1");
        assertThat(dataStore).isNotNull();
        assertThat(dataStore)
                .extracting(DataStore::getId, DataStore::getName, DataStore::getItemSubjectRef)
                .containsExactly("DATASTORE_1", "test", "ITEM_1");

        MessageFlow messageFlow = model.getMessageFlow("MESSAGEFLOW_1");
        assertThat(messageFlow).isNotNull();
        assertThat(messageFlow)
                .extracting(MessageFlow::getName, MessageFlow::getSourceRef, MessageFlow::getTargetRef)
                .containsExactly("test 1", "task1", "task2");

        messageFlow = model.getMessageFlow("MESSAGEFLOW_2");
        assertThat(messageFlow).isNotNull();
        assertThat(messageFlow)
                .extracting(MessageFlow::getName, MessageFlow::getSourceRef, MessageFlow::getTargetRef)
                .containsExactly("test 2", "task2", "task3");

        assertThat(model.getPools())
                .extracting(Pool::getId, Pool::getName, Pool::getProcessRef)
                .containsExactly(
                        tuple("participant1", "Participant 1", "PROCESS_1"),
                        tuple("participant2", "Participant 2", "PROCESS_2")
                );
    }
}

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
package org.flowable.bpm.model.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpm.model.bpmn.instance.DataStore;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataStoreTest {

    private static BpmnModelInstance modelInstance;

    @BeforeClass
    public static void parseModel() {
        modelInstance = BpmnModelBuilder.readModelFromStream(DataStoreTest.class.getResourceAsStream("DataStoreTest.bpmn20.xml"));
    }

    @Test
    public void getDataStore() {
        DataStore dataStore = modelInstance.getModelElementById("myDataStore");
        assertThat(dataStore).isNotNull();
        assertThat(dataStore.getName()).isEqualTo("My Data Store");
        assertThat(dataStore.getCapacity()).isEqualTo(23);
        assertThat(dataStore.isUnlimited()).isFalse();
    }

}

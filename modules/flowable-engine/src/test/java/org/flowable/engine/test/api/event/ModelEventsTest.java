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
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to models.
 *
 * @author Frederik Heremans
 */
public class ModelEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Test create, update and delete events of model entities.
     */
    @Test
    public void testModelEvents() throws Exception {
        Model model = null;
        try {
            model = repositoryService.newModel();
            model.setName("My model");
            model.setKey("key");
            repositoryService.saveModel(model);

            // Check create event
            assertThat(listener.getEventsReceived()).hasSize(2);
            assertThat(listener.getEventsReceived().get(0).getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
            assertThat(((Model) ((FlowableEntityEvent) listener.getEventsReceived().get(0)).getEntity()).getId()).isEqualTo(model.getId());

            assertThat(listener.getEventsReceived().get(1).getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
            assertThat(((Model) ((FlowableEntityEvent) listener.getEventsReceived().get(1)).getEntity()).getId()).isEqualTo(model.getId());
            listener.clearEventsReceived();

            // Update model
            model = repositoryService.getModel(model.getId());
            model.setName("Updated");
            repositoryService.saveModel(model);
            assertThat(listener.getEventsReceived())
                    .extracting(FlowableEvent::getType)
                    .containsExactly(FlowableEngineEventType.ENTITY_UPDATED);
            assertThat(((Model) ((FlowableEntityEvent) listener.getEventsReceived().get(0)).getEntity()).getId()).isEqualTo(model.getId());
            listener.clearEventsReceived();

            // Test additional update-methods (source and extra-source)
            repositoryService.addModelEditorSource(model.getId(), "test".getBytes());
            repositoryService.addModelEditorSourceExtra(model.getId(), "test extra".getBytes());
            assertThat(listener.getEventsReceived())
                    .extracting(FlowableEvent::getType)
                    .containsExactly(
                            FlowableEngineEventType.ENTITY_UPDATED,
                            FlowableEngineEventType.ENTITY_UPDATED);
            listener.clearEventsReceived();

            // Delete model events
            repositoryService.deleteModel(model.getId());
            assertThat(listener.getEventsReceived())
                    .extracting(FlowableEvent::getType)
                    .containsExactly(FlowableEngineEventType.ENTITY_DELETED);
            assertThat(((Model) ((FlowableEntityEvent) listener.getEventsReceived().get(0)).getEntity()).getId()).isEqualTo(model.getId());
            listener.clearEventsReceived();

        } finally {
            if (model != null && repositoryService.getModel(model.getId()) != null) {
                repositoryService.deleteModel(model.getId());
            }
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableEntityEventListener(Model.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}

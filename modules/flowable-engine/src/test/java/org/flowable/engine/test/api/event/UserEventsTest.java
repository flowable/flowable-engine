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

import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.idm.api.User;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to users.
 *
 * @author Frederik Heremans
 */
public class UserEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Test create, update and delete events of users.
     */
    @Test
    public void testUserEntityEvents() throws Exception {
        User user = null;
        try {
            user = identityService.newUser("fred");
            user.setFirstName("Frederik");
            user.setLastName("Heremans");
            identityService.saveUser(user);

            assertThat(listener.getEventsReceived()).hasSize(2);
            FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_CREATED);
            assertThat(event.getEntity()).isInstanceOf(User.class);
            User userFromEvent = (User) event.getEntity();
            assertThat(userFromEvent.getId()).isEqualTo("fred");

            event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_INITIALIZED);
            listener.clearEventsReceived();

            // Update user
            user.setFirstName("Anna");
            identityService.saveUser(user);
            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_UPDATED);
            assertThat(event.getEntity()).isInstanceOf(User.class);
            userFromEvent = (User) event.getEntity();
            assertThat(userFromEvent.getId()).isEqualTo("fred");
            assertThat(userFromEvent.getFirstName()).isEqualTo("Anna");
            listener.clearEventsReceived();

            // Delete user
            identityService.deleteUser(user.getId());

            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_DELETED);
            assertThat(event.getEntity()).isInstanceOf(User.class);
            userFromEvent = (User) event.getEntity();
            assertThat(userFromEvent.getId()).isEqualTo("fred");
            listener.clearEventsReceived();

        } finally {
            if (user != null && user.getId() != null) {
                identityService.deleteUser(user.getId());
            }
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableEntityEventListener(User.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}

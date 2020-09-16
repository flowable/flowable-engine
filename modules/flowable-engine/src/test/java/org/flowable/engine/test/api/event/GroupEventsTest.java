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
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.api.event.FlowableIdmMembershipEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to groups.
 *
 * @author Frederik Heremans
 */
public class GroupEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Test create, update and delete events of Groups.
     */
    @Test
    public void testGroupEntityEvents() throws Exception {
        Group group = null;
        try {
            group = identityService.newGroup("fred");
            group.setName("name");
            group.setType("type");
            identityService.saveGroup(group);

            assertThat(listener.getEventsReceived()).hasSize(2);
            FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_CREATED);
            assertThat(event.getEntity()).isInstanceOf(Group.class);
            Group groupFromEvent = (Group) event.getEntity();
            assertThat(groupFromEvent.getId()).isEqualTo("fred");

            event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_INITIALIZED);
            listener.clearEventsReceived();

            // Update Group
            group.setName("Another name");
            identityService.saveGroup(group);
            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_UPDATED);
            assertThat(event.getEntity()).isInstanceOf(Group.class);
            groupFromEvent = (Group) event.getEntity();
            assertThat(groupFromEvent.getId()).isEqualTo("fred");
            assertThat(groupFromEvent.getName()).isEqualTo("Another name");
            listener.clearEventsReceived();

            // Delete Group
            identityService.deleteGroup(group.getId());

            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.ENTITY_DELETED);
            assertThat(event.getEntity()).isInstanceOf(Group.class);
            groupFromEvent = (Group) event.getEntity();
            assertThat(groupFromEvent.getId()).isEqualTo("fred");
            listener.clearEventsReceived();

        } finally {
            if (group != null && group.getId() != null) {
                identityService.deleteGroup(group.getId());
            }
        }
    }

    /**
     * Test create, update and delete events of Groups.
     */
    @Test
    public void testGroupMembershipEvents() throws Exception {
        TestFlowableEventListener membershipListener = new TestFlowableEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(membershipListener);

        User user = null;
        Group group = null;
        try {
            user = identityService.newUser("kermit");
            identityService.saveUser(user);

            group = identityService.newGroup("sales");
            identityService.saveGroup(group);

            // Add membership
            membershipListener.clearEventsReceived();
            identityService.createMembership("kermit", "sales");
            assertThat(membershipListener.getEventsReceived()).hasSize(1);
            assertThat(membershipListener.getEventsReceived().get(0)).isInstanceOf(FlowableIdmMembershipEvent.class);
            FlowableIdmMembershipEvent event = (FlowableIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.MEMBERSHIP_CREATED);
            assertThat(event.getGroupId()).isEqualTo("sales");
            assertThat(event.getUserId()).isEqualTo("kermit");
            membershipListener.clearEventsReceived();

            // Delete membership
            identityService.deleteMembership("kermit", "sales");
            assertThat(membershipListener.getEventsReceived()).hasSize(1);
            assertThat(membershipListener.getEventsReceived().get(0)).isInstanceOf(FlowableIdmMembershipEvent.class);
            event = (FlowableIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.MEMBERSHIP_DELETED);
            assertThat(event.getGroupId()).isEqualTo("sales");
            assertThat(event.getUserId()).isEqualTo("kermit");
            membershipListener.clearEventsReceived();

            // Deleting group will dispatch an event, informing ALL memberships are deleted
            identityService.createMembership("kermit", "sales");
            membershipListener.clearEventsReceived();
            identityService.deleteGroup(group.getId());

            assertThat(membershipListener.getEventsReceived()).hasSize(2);
            assertThat(membershipListener.getEventsReceived().get(0)).isInstanceOf(FlowableIdmMembershipEvent.class);
            event = (FlowableIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableIdmEventType.MEMBERSHIPS_DELETED);
            assertThat(event.getGroupId()).isEqualTo("sales");
            assertThat(event.getUserId()).isNull();
            membershipListener.clearEventsReceived();
        } finally {
            processEngineConfiguration.getEventDispatcher().removeEventListener(membershipListener);
            if (user != null) {
                identityService.deleteUser(user.getId());
            }
            if (group != null) {
                identityService.deleteGroup(group.getId());
            }
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableEntityEventListener(Group.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}

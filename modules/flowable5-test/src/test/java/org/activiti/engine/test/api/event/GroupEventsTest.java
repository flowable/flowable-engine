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
package org.activiti.engine.test.api.event;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.api.event.FlowableIdmMembershipEvent;

/**
 * Test case for all {@link FlowableEvent}s related to groups.
 * 
 * @author Frederik Heremans
 */
public class GroupEventsTest extends PluggableFlowableTestCase {

    private TestFlowable6EntityEventListener listener;

    /**
     * Test create, update and delete events of Groups.
     */
    public void testGroupEntityEvents() throws Exception {
        Group group = null;
        try {
            group = identityService.newGroup("fred");
            group.setName("name");
            group.setType("type");
            identityService.saveGroup(group);

            assertEquals(2, listener.getEventsReceived().size());
            FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableIdmEventType.ENTITY_CREATED, event.getType());
            assertTrue(event.getEntity() instanceof Group);
            Group groupFromEvent = (Group) event.getEntity();
            assertEquals("fred", groupFromEvent.getId());

            event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
            assertEquals(FlowableIdmEventType.ENTITY_INITIALIZED, event.getType());
            listener.clearEventsReceived();

            // Update Group
            group.setName("Another name");
            identityService.saveGroup(group);
            assertEquals(1, listener.getEventsReceived().size());
            event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableIdmEventType.ENTITY_UPDATED, event.getType());
            assertTrue(event.getEntity() instanceof Group);
            groupFromEvent = (Group) event.getEntity();
            assertEquals("fred", groupFromEvent.getId());
            assertEquals("Another name", groupFromEvent.getName());
            listener.clearEventsReceived();

            // Delete Group
            identityService.deleteGroup(group.getId());

            assertEquals(1, listener.getEventsReceived().size());
            event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
            assertEquals(FlowableIdmEventType.ENTITY_DELETED, event.getType());
            assertTrue(event.getEntity() instanceof Group);
            groupFromEvent = (Group) event.getEntity();
            assertEquals("fred", groupFromEvent.getId());
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
    public void testGroupMembershipEvents() throws Exception {
        TestFlowable6EventListener membershipListener = new TestFlowable6EventListener();
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
            assertEquals(1, membershipListener.getEventsReceived().size());
            assertTrue(membershipListener.getEventsReceived().get(0) instanceof FlowableIdmMembershipEvent);
            FlowableIdmMembershipEvent event = (FlowableIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
            assertEquals(FlowableIdmEventType.MEMBERSHIP_CREATED, event.getType());
            assertEquals("sales", event.getGroupId());
            assertEquals("kermit", event.getUserId());
            membershipListener.clearEventsReceived();

            // Delete membership
            identityService.deleteMembership("kermit", "sales");
            assertEquals(1, membershipListener.getEventsReceived().size());
            assertTrue(membershipListener.getEventsReceived().get(0) instanceof FlowableIdmMembershipEvent);
            event = (FlowableIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
            assertEquals(FlowableIdmEventType.MEMBERSHIP_DELETED, event.getType());
            assertEquals("sales", event.getGroupId());
            assertEquals("kermit", event.getUserId());
            membershipListener.clearEventsReceived();

            // Deleting group will dispatch an event, informing ALL memberships are deleted
            identityService.createMembership("kermit", "sales");
            membershipListener.clearEventsReceived();
            identityService.deleteGroup(group.getId());

            assertEquals(2, membershipListener.getEventsReceived().size());
            assertTrue(membershipListener.getEventsReceived().get(0) instanceof FlowableIdmMembershipEvent);
            event = (FlowableIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
            assertEquals(FlowableIdmEventType.MEMBERSHIPS_DELETED, event.getType());
            assertEquals("sales", event.getGroupId());
            assertNull(event.getUserId());
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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        listener = new TestFlowable6EntityEventListener(Group.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}

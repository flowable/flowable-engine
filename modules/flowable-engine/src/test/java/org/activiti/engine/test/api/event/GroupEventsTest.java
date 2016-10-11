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

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.idm.api.Group;
import org.activiti.idm.api.User;
import org.activiti.idm.api.event.ActivitiIdmEntityEvent;
import org.activiti.idm.api.event.ActivitiIdmEventType;
import org.activiti.idm.api.event.ActivitiIdmMembershipEvent;

/**
 * Test case for all {@link ActivitiEvent}s related to groups.
 * 
 * @author Frederik Heremans
 */
public class GroupEventsTest extends PluggableActivitiTestCase {

  private TestActivitiIdmEntityEventListener listener;

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
      ActivitiIdmEntityEvent event = (ActivitiIdmEntityEvent) listener.getEventsReceived().get(0);
      assertEquals(ActivitiIdmEventType.ENTITY_CREATED, event.getType());
      assertTrue(event.getEntity() instanceof Group);
      Group groupFromEvent = (Group) event.getEntity();
      assertEquals("fred", groupFromEvent.getId());

      event = (ActivitiIdmEntityEvent) listener.getEventsReceived().get(1);
      assertEquals(ActivitiIdmEventType.ENTITY_INITIALIZED, event.getType());
      listener.clearEventsReceived();

      // Update Group
      group.setName("Another name");
      identityService.saveGroup(group);
      assertEquals(1, listener.getEventsReceived().size());
      event = (ActivitiIdmEntityEvent) listener.getEventsReceived().get(0);
      assertEquals(ActivitiIdmEventType.ENTITY_UPDATED, event.getType());
      assertTrue(event.getEntity() instanceof Group);
      groupFromEvent = (Group) event.getEntity();
      assertEquals("fred", groupFromEvent.getId());
      assertEquals("Another name", groupFromEvent.getName());
      listener.clearEventsReceived();

      // Delete Group
      identityService.deleteGroup(group.getId());

      assertEquals(1, listener.getEventsReceived().size());
      event = (ActivitiIdmEntityEvent) listener.getEventsReceived().get(0);
      assertEquals(ActivitiIdmEventType.ENTITY_DELETED, event.getType());
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
    TestActivitiIdmEventListener membershipListener = new TestActivitiIdmEventListener();
    processEngineConfiguration.getIdmEventDispatcher().addEventListener(membershipListener);

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
      assertTrue(membershipListener.getEventsReceived().get(0) instanceof ActivitiIdmMembershipEvent);
      ActivitiIdmMembershipEvent event = (ActivitiIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
      assertEquals(ActivitiIdmEventType.MEMBERSHIP_CREATED, event.getType());
      assertEquals("sales", event.getGroupId());
      assertEquals("kermit", event.getUserId());
      membershipListener.clearEventsReceived();

      // Delete membership
      identityService.deleteMembership("kermit", "sales");
      assertEquals(1, membershipListener.getEventsReceived().size());
      assertTrue(membershipListener.getEventsReceived().get(0) instanceof ActivitiIdmMembershipEvent);
      event = (ActivitiIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
      assertEquals(ActivitiIdmEventType.MEMBERSHIP_DELETED, event.getType());
      assertEquals("sales", event.getGroupId());
      assertEquals("kermit", event.getUserId());
      membershipListener.clearEventsReceived();

      // Deleting group will dispatch an event, informing ALL memberships are deleted
      identityService.createMembership("kermit", "sales");
      membershipListener.clearEventsReceived();
      identityService.deleteGroup(group.getId());

      assertEquals(2, membershipListener.getEventsReceived().size());
      assertTrue(membershipListener.getEventsReceived().get(0) instanceof ActivitiIdmMembershipEvent);
      event = (ActivitiIdmMembershipEvent) membershipListener.getEventsReceived().get(0);
      assertEquals(ActivitiIdmEventType.MEMBERSHIPS_DELETED, event.getType());
      assertEquals("sales", event.getGroupId());
      assertNull(event.getUserId());
      membershipListener.clearEventsReceived();
    } finally {
      processEngineConfiguration.getIdmEventDispatcher().removeEventListener(membershipListener);
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
    listener = new TestActivitiIdmEntityEventListener(Group.class);
    processEngineConfiguration.getIdmEventDispatcher().addEventListener(listener);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    if (listener != null) {
      processEngineConfiguration.getIdmEventDispatcher().removeEventListener(listener);
    }
  }
}

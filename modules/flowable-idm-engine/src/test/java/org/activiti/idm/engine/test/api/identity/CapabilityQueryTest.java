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
package org.activiti.idm.engine.test.api.identity;

import java.util.List;

import org.activiti.idm.api.Capability;
import org.activiti.idm.engine.impl.persistence.entity.CapabilityEntity;
import org.activiti.idm.engine.test.PluggableActivitiIdmTestCase;

/**
 * @author Joram Barrez
 */
public class CapabilityQueryTest extends PluggableActivitiIdmTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    createGroup("admins", null, "user");
    createGroup("sales", null, "user");
    createGroup("engineering", null, "user");

    idmIdentityService.saveUser(idmIdentityService.newUser("kermit"));
    idmIdentityService.saveUser(idmIdentityService.newUser("fozzie"));
    idmIdentityService.saveUser(idmIdentityService.newUser("mispiggy"));

    idmIdentityService.createMembership("kermit", "admins");
    idmIdentityService.createMembership("kermit", "sales");
    idmIdentityService.createMembership("kermit", "engineering");
    idmIdentityService.createMembership("fozzie", "sales");
    idmIdentityService.createMembership("mispiggy", "engineering");

    String adminCapability = "access admin application";
    idmIdentityService.createCapability(adminCapability, null, "admins");
    idmIdentityService.createCapability(adminCapability, "mispiggy", null);
    
    String modelerCapability = "access modeler application";
    idmIdentityService.createCapability(modelerCapability, null, "admins");
    idmIdentityService.createCapability(modelerCapability, null, "engineering");
    idmIdentityService.createCapability(modelerCapability, "kermit", null);
    
    String startProcessesCapability = "start processes";
    idmIdentityService.createCapability(startProcessesCapability, null, "sales");
  }
  
  @Override
  protected void tearDown() throws Exception {
    clearAllUsersAndGroups();
    super.tearDown();
  }
  
  public void testQueryAll() {
    List<Capability> capabilities = idmIdentityService.createCapabilityQuery().list();
    assertEquals(6, capabilities.size());
    assertEquals(6L, idmIdentityService.createCapabilityQuery().count());
    
    int nrOfUserCapabilities = 0;
    int nrOfGroupCapabilities = 0;
    for (Capability capability : capabilities) {
      assertNotNull(capability.getCapabilityName());
      if (capability.getUserId() != null) {
        nrOfUserCapabilities++;
      }
      if (capability.getGroupId() != null) {
        nrOfGroupCapabilities++;
      }
    }
    
    assertEquals(2, nrOfUserCapabilities);
    assertEquals(4, nrOfGroupCapabilities);
  }
  
  public void testQueryByCapabilityName() {
    List<Capability> capabilities = idmIdentityService.createCapabilityQuery().capabilityName("access admin application").list();
    assertEquals(2, capabilities.size());
    
    boolean groupFound = false;
    boolean userFound = false;
    for (Capability capability : capabilities) {
      if ("admins".equals(capability.getGroupId())) {
        groupFound = true;
      } 
      if ("mispiggy".equals(capability.getUserId())) {
        userFound = true;
      }
    }
    
    assertTrue(userFound);
    assertTrue(groupFound);
  }
  
 public void testQueryByInvalidCapabilityName() {
    assertEquals(0, idmIdentityService.createCapabilityQuery().capabilityName("does not exist").list().size());
  }
  
  public void testQueryByUserId() {
    List<Capability> capabilities = idmIdentityService.createCapabilityQuery().userId("kermit").list();
    assertEquals(1, capabilities.size());
    
    Capability capability = capabilities.get(0);
    assertEquals("access modeler application", capability.getCapabilityName());
  }
  
  public void testQueryByInvalidUserId() {
    assertEquals(0, idmIdentityService.createCapabilityQuery().userId("does not exist").list().size());
  }
  
  public void testQueryByGroupId() {
    List<Capability> capabilities = idmIdentityService.createCapabilityQuery().groupId("admins").list();
    assertEquals(2, capabilities.size());
  }
  
  public void testQueryByInvalidGroupId() {
    assertEquals(0, idmIdentityService.createCapabilityQuery().groupId("does not exist").list().size());
  }
  
  public void testNativeQuery() {
    assertEquals("ACT_ID_CAPABILITY", idmManagementService.getTableName(Capability.class));
    assertEquals("ACT_ID_CAPABILITY", idmManagementService.getTableName(CapabilityEntity.class));
    
    String tableName = idmManagementService.getTableName(CapabilityEntity.class);
    String baseQuerySql = "SELECT * FROM " + tableName + " where USER_ID_ = #{userId}";

    assertEquals(1, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).parameter("userId", "kermit").list().size());
  }
  
}

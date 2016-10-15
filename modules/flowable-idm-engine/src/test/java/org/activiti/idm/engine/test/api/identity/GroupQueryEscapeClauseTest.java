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

import org.activiti.idm.api.Group;
import org.activiti.idm.api.GroupQuery;
import org.activiti.idm.engine.test.ResourceActivitiIdmTestCase;

public class GroupQueryEscapeClauseTest extends ResourceActivitiIdmTestCase {
  
  public GroupQueryEscapeClauseTest() {
    super("escapeclause/activiti.idm.cfg.xml");
  }

  protected void setUp() throws Exception {
    super.setUp();

    createGroup("muppets", "muppets%", "user");
    createGroup("frogs", "frogs_", "user");
  }
  
  @Override
  protected void tearDown() throws Exception {
    idmIdentityService.deleteGroup("muppets");
    idmIdentityService.deleteGroup("frogs");
    super.tearDown();
  }
  
  public void testQueryByNameLike() {
    GroupQuery query = idmIdentityService.createGroupQuery().groupNameLike("%\\%%");
    assertEquals(1, query.list().size());
    assertEquals(1, query.count());
    assertEquals("muppets", query.singleResult().getId());
    
    query = idmIdentityService.createGroupQuery().groupNameLike("%\\_%");
    assertEquals(1, query.list().size());
    assertEquals(1, query.count());
    assertEquals("frogs", query.singleResult().getId());
  }
  
  private Group createGroup(String id, String name, String type) {
    Group group = idmIdentityService.newGroup(id);
    group.setName(name);
    group.setType(type);
    idmIdentityService.saveGroup(group);
    return group;
  }
}

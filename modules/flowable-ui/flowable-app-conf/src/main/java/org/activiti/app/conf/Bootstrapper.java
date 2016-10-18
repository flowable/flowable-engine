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
package org.activiti.app.conf;

import org.activiti.app.constant.GroupIds;
import org.activiti.app.constant.GroupTypes;
import org.activiti.engine.IdentityService;
import org.activiti.idm.api.Group;
import org.activiti.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Responsible for executing all action required after booting up the Spring container.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@Component
public class Bootstrapper implements ApplicationListener<ContextRefreshedEvent> {

  private final Logger log = LoggerFactory.getLogger(Bootstrapper.class);

  @Autowired
  private IdentityService identityService;

  @Autowired
  private Environment env;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event.getApplicationContext().getParent() == null) { // Using Spring MVC, there are multiple child contexts. We only care about the root

      // First create the default IDM entities
      createDefaultAdmin();
    }
  }

  protected void createDefaultAdmin() {
    if (identityService.createUserQuery().count() == 0) {
      log.info("No users found, initializing default entities");
      User user = initializeSuperUser();
      initializeSuperUserGroups(user);
    }
  }

  protected User initializeSuperUser() {
    String adminPassword = env.getRequiredProperty("admin.password");
    String adminLastname = env.getRequiredProperty("admin.lastname");
    String adminEmail = env.getRequiredProperty("admin.email");

    User admin = identityService.newUser(adminEmail);
    admin.setLastName(adminLastname);
    admin.setEmail(adminEmail);
    admin.setPassword(adminPassword);
    identityService.saveUser(admin);
    return admin;
  }

  protected void initializeSuperUserGroups(User superUser) {
    String superUserGroupName = env.getRequiredProperty("admin.group");
    Group group = identityService.newGroup(GroupIds.ROLE_ADMIN);
    group.setName(superUserGroupName);
    group.setType(GroupTypes.TYPE_SECURITY_ROLE);
    identityService.saveGroup(group);
    identityService.createMembership(superUser.getId(), group.getId());
  }
  
}
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
package org.flowable.app.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.app.security.DefaultPrivileges;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrapper.class);

    @Qualifier("defaultIdmIdentityService")
    @Autowired
    private IdmIdentityService identityService;

    @Autowired
    private Environment env;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) { // Using Spring MVC, there are multiple child contexts. We only care about the root

            if (!env.getProperty("ldap.enabled", Boolean.class, false)) {
                if (env.getProperty("idm.bootstrap.enabled", Boolean.class, true)){
                    // First create the default IDM entities
                    createDefaultAdmin();
                }
            } else {
                if (identityService.createPrivilegeQuery().count() == 0) {
                    String adminUserId = env.getRequiredProperty("admin.userid");
                    initializeDefaultPrivileges(adminUserId);
                }
            }
        }
    }

    protected void createDefaultAdmin() {
        if (identityService.createUserQuery().count() == 0) {
            LOGGER.info("No users found, initializing default entities");
            User user = initializeSuperUser();
            initializeDefaultPrivileges(user.getId());
        }
    }

    protected User initializeSuperUser() {
        String adminUserId = env.getRequiredProperty("admin.userid");
        String adminPassword = env.getRequiredProperty("admin.password");
        String adminFirstname = env.getRequiredProperty("admin.firstname");
        String adminLastname = env.getRequiredProperty("admin.lastname");
        String adminEmail = env.getProperty("admin.email");

        User admin = identityService.newUser(adminUserId);
        admin.setFirstName(adminFirstname);
        admin.setLastName(adminLastname);
        admin.setEmail(adminEmail);
        admin.setPassword(adminPassword);
        identityService.saveUser(admin);
        return admin;
    }

    protected void initializeDefaultPrivileges(String adminId) {
        List<Privilege> privileges = identityService.createPrivilegeQuery().list();
        Map<String, Privilege> privilegeMap = new HashMap<>();
        for (Privilege privilege : privileges) {
            privilegeMap.put(privilege.getName(), privilege);
        }
        
        Privilege idmAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_IDM, privilegeMap);
        identityService.addUserPrivilegeMapping(idmAppPrivilege.getId(), adminId);

        Privilege adminAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_ADMIN, privilegeMap);
        identityService.addUserPrivilegeMapping(adminAppPrivilege.getId(), adminId);

        Privilege modelerAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_MODELER, privilegeMap);
        identityService.addUserPrivilegeMapping(modelerAppPrivilege.getId(), adminId);

        Privilege taskAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_TASK, privilegeMap);
        identityService.addUserPrivilegeMapping(taskAppPrivilege.getId(), adminId);
    }
    
    protected Privilege findOrCreatePrivilege(String privilegeId, Map<String, Privilege> privilegeMap) {
        Privilege privilege = null;
        if (privilegeMap.containsKey(privilegeId)) {
            privilege = privilegeMap.get(privilegeId);
        } else {
            privilege = identityService.createPrivilege(privilegeId);
        }
        
        return privilege;
    }

}

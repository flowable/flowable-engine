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
package org.flowable.ui.idm.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.spring.boot.ldap.FlowableLdapProperties;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.idm.properties.FlowableIdmAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Responsible for executing all action required after booting up the Spring container.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@Component
public class Bootstrapper implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrapper.class);

    @Autowired
    private IdmIdentityService identityService;

    private FlowableLdapProperties ldapProperties;

    @Autowired
    private FlowableIdmAppProperties idmAppProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) { // Using Spring MVC, there are multiple child contexts. We only care about the root

            if (ldapProperties == null || !ldapProperties.isEnabled()) {
                if (idmAppProperties.isBootstrap()) {
                    // First create the default IDM entities
                    createDefaultAdminUserAndPrivileges();
                }
            
            } else {
                if (identityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_IDM).count() == 0) {
                    String adminUserId = idmAppProperties.getAdmin().getUserId();
                    if (StringUtils.isNotEmpty(adminUserId)) {
                        initializeDefaultPrivileges(adminUserId);
                    } else {
                        LOGGER.warn(
                            "No user found with IDM access. Set flowable.idp.app.admin.user-id to give at least one user access to the IDM application to configure privileges.");
                    }
                }
            }
        }
    }

    protected void createDefaultAdminUserAndPrivileges() {
        String adminUserId = idmAppProperties.getAdmin().getUserId();
        if (StringUtils.isNotEmpty(adminUserId)) {
            User adminUser = identityService.createUserQuery().userId(adminUserId).singleResult();
            if (adminUser == null) {
                LOGGER.info("No admin user found, initializing default entities");
                adminUser = initializeAdminUser();
            } 
            initializeDefaultPrivileges(adminUser.getId());
        }
       
    }

    protected User initializeAdminUser() {
        FlowableIdmAppProperties.Admin adminConfig = idmAppProperties.getAdmin();
        String adminUserId = adminConfig.getUserId();
        Assert.notNull(adminUserId, "flowable.idm.app.admin.user-id property must be set");
        String adminPassword = adminConfig.getPassword();
        Assert.notNull(adminPassword, "flowable.idm.app.admin.password property must be set");
        String adminFirstname = adminConfig.getFirstName();
        Assert.notNull(adminFirstname, "flowable.idm.app.admin.first-name property must be set");
        String adminLastname = adminConfig.getLastName();
        Assert.notNull(adminLastname, "flowable.idm.app.admin.last-name property must be set");
        String adminEmail = adminConfig.getEmail();

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
        if (!privilegeMappingExists(adminId, idmAppPrivilege)) {
            identityService.addUserPrivilegeMapping(idmAppPrivilege.getId(), adminId);
        }

        Privilege adminAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_ADMIN, privilegeMap);
        if (!privilegeMappingExists(adminId, adminAppPrivilege)) {
            identityService.addUserPrivilegeMapping(adminAppPrivilege.getId(), adminId);
        }

        Privilege modelerAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_MODELER, privilegeMap);
        if (!privilegeMappingExists(adminId, modelerAppPrivilege)) {
            identityService.addUserPrivilegeMapping(modelerAppPrivilege.getId(), adminId);
        }

        Privilege taskAppPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_TASK, privilegeMap);
        if (!privilegeMappingExists(adminId, taskAppPrivilege)) {
            identityService.addUserPrivilegeMapping(taskAppPrivilege.getId(), adminId);
        }
        
        Privilege restApiAccessPrivilege = findOrCreatePrivilege(DefaultPrivileges.ACCESS_REST_API, privilegeMap);
        if (!privilegeMappingExists(adminId, restApiAccessPrivilege)) {
            identityService.addUserPrivilegeMapping(restApiAccessPrivilege.getId(), adminId);
        }
    }
    
    protected Privilege findOrCreatePrivilege(String privilegeName, Map<String, Privilege> privilegeMap) {
        Privilege privilege = null;
        if (privilegeMap.containsKey(privilegeName)) {
            privilege = privilegeMap.get(privilegeName);
        } else {
            try {
                privilege = identityService.createPrivilege(privilegeName);
            } catch (Exception e) {
                privilege = identityService.createPrivilegeQuery().privilegeName(privilegeName).singleResult();
            }
        }
        
        if (privilege == null) {
            throw new FlowableException("Could not find or create " + DefaultPrivileges.ACCESS_REST_API + " privilege");
        }
        
        return privilege;
    }
    
    protected boolean privilegeMappingExists(String restAdminId, Privilege privilege) {
        return identityService.createPrivilegeQuery()
                .userId(restAdminId)
                .privilegeId(privilege.getId())
                .singleResult() != null;
    }

    @Autowired(required = false)
    public void setLdapProperties(FlowableLdapProperties ldapProperties) {
        this.ldapProperties = ldapProperties;
    }
}

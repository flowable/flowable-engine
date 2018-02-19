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

package org.flowable.rest.conf;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.repository.Deployment;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.rest.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Joram Barrez
 */
@Configuration
public class BootstrapConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfiguration.class);

    @Autowired
    protected Environment environment;
    
    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired
    protected IdmIdentityService idmIdentityService;
    
    @PostConstruct
    public void init() {

        // Rest admin user
        String restAdminUserId = environment.getProperty("rest-admin.userid");
        if (StringUtils.isNotEmpty(restAdminUserId)) {
            createDefaultAdminUserAndPrivileges(restAdminUserId);
        }
        
        // Demo process definitions
        if (environment.getProperty("create.demo.definitions", Boolean.class, false)) {
            LOGGER.info("Initializing demo process definitions");
            initDemoProcessDefinitions();
        }
        
    }
    
    protected void createDefaultAdminUserAndPrivileges(String restAdminUserId) {
        User restAdminUser = idmIdentityService.createUserQuery().userId(restAdminUserId).singleResult();
        if (restAdminUser == null) {
            LOGGER.info("No rest admin user found, initializing default entities");
            restAdminUser = initRestAdmin();
        }
        initializeDefaultPrivileges(restAdminUser.getId());
    }
    
    protected User initRestAdmin() {
        String adminUserId = environment.getRequiredProperty("rest-admin.userid");
        String adminPassword = environment.getRequiredProperty("rest-admin.password");
        String adminFirstname = environment.getRequiredProperty("rest-admin.firstname");
        String adminLastname = environment.getRequiredProperty("rest-admin.lastname");

        User restAdminUser = idmIdentityService.newUser(adminUserId);
        restAdminUser.setFirstName(adminFirstname);
        restAdminUser.setLastName(adminLastname);
        restAdminUser.setPassword(adminPassword);
        idmIdentityService.saveUser(restAdminUser);
        
        return restAdminUser;
    }
    
    protected void initializeDefaultPrivileges(String restAdminId) {
        boolean restApiPrivilegeMappingExists = false;
        Privilege privilege = idmIdentityService.createPrivilegeQuery().privilegeName(SecurityConstants.PRIVILEGE_ACCESS_REST_API).singleResult();
        if (privilege != null) {
            restApiPrivilegeMappingExists = restApiPrivilegeMappingExists(restAdminId, privilege);
        } else {
            try {
                privilege = idmIdentityService.createPrivilege(SecurityConstants.PRIVILEGE_ACCESS_REST_API);
            } catch (Exception e) {
                // Could be created by another server, retrying fetch
                privilege = idmIdentityService.createPrivilegeQuery().privilegeName(SecurityConstants.PRIVILEGE_ACCESS_REST_API).singleResult();
            }
        }
        
        if (privilege == null) {
            throw new FlowableException("Could not find or create " + SecurityConstants.PRIVILEGE_ACCESS_REST_API + " privilege");
        }
        
        if (!restApiPrivilegeMappingExists) {
            idmIdentityService.addUserPrivilegeMapping(privilege.getId(), restAdminId);
        }
    }

    protected boolean restApiPrivilegeMappingExists(String restAdminId, Privilege privilege) {
        return idmIdentityService.createPrivilegeQuery()
                .userId(restAdminId)
                .privilegeId(privilege.getId())
                .singleResult() != null;
    }
    
    protected void initDemoProcessDefinitions() {

        String deploymentName = "Demo processes";
        List<Deployment> deploymentList = repositoryService.createDeploymentQuery().deploymentName(deploymentName).list();

        if (deploymentList == null || deploymentList.isEmpty()) {
            repositoryService.createDeployment().name(deploymentName)
                .addClasspathResource("createTimersProcess.bpmn20.xml")
                .addClasspathResource("oneTaskProcess.bpmn20.xml")
                .addClasspathResource("VacationRequest.bpmn20.xml")
                .addClasspathResource("VacationRequest.png")
                .addClasspathResource("FixSystemFailureProcess.bpmn20.xml")
                .addClasspathResource("FixSystemFailureProcess.png")
                .addClasspathResource("Helpdesk.bpmn20.xml")
                .addClasspathResource("Helpdesk.png")
                .addClasspathResource("reviewSalesLead.bpmn20.xml")
                .deploy();
        }
    }
    
}

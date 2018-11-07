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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.rest.app.properties.RestAppProperties;
import org.flowable.rest.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
@Configuration
public class BootstrapConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfiguration.class);

    protected final RepositoryService repositoryService;

    protected final IdmIdentityService idmIdentityService;

    protected final RestAppProperties restAppProperties;

    public BootstrapConfiguration(RepositoryService repositoryService, IdmIdentityService idmIdentityService, RestAppProperties restAppProperties) {
        this.repositoryService = repositoryService;
        this.idmIdentityService = idmIdentityService;
        this.restAppProperties = restAppProperties;
    }

    /**
     * Initialize the rest admin user
     */
    @Bean
    @ConditionalOnProperty(prefix = "flowable.rest.app.admin", name = "user-id")
    public CommandLineRunner initDefaultAdminUserAndPrivilegesRunner() {
        return args -> {
            if (StringUtils.isNotEmpty(restAppProperties.getAdmin().getUserId())) {
                createDefaultAdminUserAndPrivileges(restAppProperties.getAdmin().getUserId());
            }
        };
    }

    /**
     * Initialize the demo process definitions
     */
    @Bean
    @ConditionalOnProperty(prefix = "flowable.rest.app", name = "create-demo-definitions", havingValue = "true")
    public CommandLineRunner initDemoProcessDefinitionsRunner() {
        return args -> initDemoProcessDefinitions();
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
        RestAppProperties.Admin admin = restAppProperties.getAdmin();
        String adminUserId = admin.getUserId();
        String adminPassword = admin.getPassword();
        String adminFirstname = admin.getFirstName();
        String adminLastname = admin.getLastName();

        User restAdminUser = idmIdentityService.newUser(adminUserId);
        restAdminUser.setFirstName(adminFirstname);
        restAdminUser.setLastName(adminLastname);
        restAdminUser.setPassword(adminPassword);
        idmIdentityService.saveUser(restAdminUser);
        
        return restAdminUser;
    }
    
    protected void initializeDefaultPrivileges(String restAdminId) {
        initializePrivilege(restAdminId, SecurityConstants.PRIVILEGE_ACCESS_REST_API);
        initializePrivilege(restAdminId, SecurityConstants.ACCESS_ADMIN);
    }

    protected void initializePrivilege(String restAdminId, String privilegeName) {
        boolean restApiPrivilegeMappingExists = false;
        Privilege privilege = idmIdentityService.createPrivilegeQuery().privilegeName(privilegeName).singleResult();
        if (privilege != null) {
            restApiPrivilegeMappingExists = restApiPrivilegeMappingExists(restAdminId, privilege);
        } else {
            try {
                privilege = idmIdentityService.createPrivilege(privilegeName);
            } catch (Exception e) {
                // Could be created by another server, retrying fetch
                privilege = idmIdentityService.createPrivilegeQuery().privilegeName(privilegeName).singleResult();
            }
        }
        
        if (privilege == null) {
            throw new FlowableException("Could not find or create " + privilegeName + " privilege");
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

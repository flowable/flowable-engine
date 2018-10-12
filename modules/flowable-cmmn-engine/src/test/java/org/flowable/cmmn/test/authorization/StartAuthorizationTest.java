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

package org.flowable.cmmn.test.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */

public class StartAuthorizationTest extends FlowableCmmnTestCase {

    protected IdmIdentityService identityService;

    protected User userInGroup1;
    protected User userInGroup2;
    protected User userInGroup3;

    protected Group group1;
    protected Group group2;
    protected Group group3;

    protected void setupUsersAndGroups() throws Exception {

        IdmEngineConfiguration idmEngineConfiguration = (IdmEngineConfiguration) cmmnEngine.getCmmnEngineConfiguration().getEngineConfigurations().get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
        identityService = idmEngineConfiguration.getIdmIdentityService();

        identityService.saveUser(identityService.newUser("user1"));
        identityService.saveUser(identityService.newUser("user2"));
        identityService.saveUser(identityService.newUser("user3"));

        // create users
        userInGroup1 = identityService.newUser("userInGroup1");
        identityService.saveUser(userInGroup1);

        userInGroup2 = identityService.newUser("userInGroup2");
        identityService.saveUser(userInGroup2);

        userInGroup3 = identityService.newUser("userInGroup3");
        identityService.saveUser(userInGroup3);

        // create groups
        group1 = identityService.newGroup("group1");
        identityService.saveGroup(group1);

        group2 = identityService.newGroup("group2");
        identityService.saveGroup(group2);

        group3 = identityService.newGroup("group3");
        identityService.saveGroup(group3);

        // relate users to groups
        identityService.createMembership(userInGroup1.getId(), group1.getId());
        identityService.createMembership(userInGroup2.getId(), group2.getId());
        identityService.createMembership(userInGroup3.getId(), group3.getId());
    }

    protected void tearDownUsersAndGroups() throws Exception {
        identityService.deleteMembership(userInGroup1.getId(), group1.getId());
        identityService.deleteMembership(userInGroup2.getId(), group2.getId());
        identityService.deleteMembership(userInGroup3.getId(), group3.getId());

        identityService.deleteGroup(group1.getId());
        identityService.deleteGroup(group2.getId());
        identityService.deleteGroup(group3.getId());

        identityService.deleteUser(userInGroup1.getId());
        identityService.deleteUser(userInGroup2.getId());
        identityService.deleteUser(userInGroup3.getId());

        identityService.deleteUser("user1");
        identityService.deleteUser("user2");
        identityService.deleteUser("user3");
    }

    @Test
    @CmmnDeployment
    public void testIdentityLinks() throws Exception {

        setupUsersAndGroups();

        try {
            CaseDefinition latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult();
            assertNotNull(latestCaseDef);
            List<IdentityLink> links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(0, links.size());

            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case2").singleResult();
            assertNotNull(latestCaseDef);
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(2, links.size());
            assertTrue(containsUserOrGroup("user1", null, links));
            assertTrue(containsUserOrGroup("user2", null, links));

            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case3").singleResult();
            assertNotNull(latestCaseDef);
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(1, links.size());
            assertEquals("user1", links.get(0).getUserId());

            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case4").singleResult();
            assertNotNull(latestCaseDef);
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(3, links.size());
            assertTrue(containsUserOrGroup("user1", null, links));
            assertTrue(containsUserOrGroup(null, "group1", links));
            assertTrue(containsUserOrGroup(null, "group2", links));

        } finally {
            tearDownUsersAndGroups();
        }
    }

    @Test
    @CmmnDeployment
    public void testAddAndRemoveIdentityLinks() throws Exception {

        setupUsersAndGroups();

        try {
            CaseDefinition latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult();
            assertNotNull(latestCaseDef);
            List<IdentityLink> links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(0, links.size());

            cmmnRepositoryService.addCandidateStarterGroup(latestCaseDef.getId(), "group1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(1, links.size());
            assertEquals("group1", links.get(0).getGroupId());

            cmmnRepositoryService.addCandidateStarterUser(latestCaseDef.getId(), "user1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(2, links.size());
            assertTrue(containsUserOrGroup(null, "group1", links));
            assertTrue(containsUserOrGroup("user1", null, links));

            cmmnRepositoryService.deleteCandidateStarterGroup(latestCaseDef.getId(), "nonexisting");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(2, links.size());

            cmmnRepositoryService.deleteCandidateStarterGroup(latestCaseDef.getId(), "group1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(1, links.size());
            assertEquals("user1", links.get(0).getUserId());

            cmmnRepositoryService.deleteCandidateStarterUser(latestCaseDef.getId(), "user1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(0, links.size());

        } finally {
            tearDownUsersAndGroups();
        }
    }
    
    @Test
    @CmmnDeployment
    public void testCaseDefinitionList() throws Exception {

        setupUsersAndGroups();
        try {

            // Case 1 has no potential starters
            CaseDefinition latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case1").singleResult();
            List<IdentityLink> links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(0, links.size());

            // user1 and user2 are potential starters of Case 2
            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case2").singleResult();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(2, links.size());
            assertTrue(containsUserOrGroup("user1", null, links));
            assertTrue(containsUserOrGroup("user2", null, links));

            // Case 3 has 3 groups as authorized starter groups
            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case3").singleResult();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertEquals(3, links.size());
            assertTrue(containsUserOrGroup(null, "group1", links));
            assertTrue(containsUserOrGroup(null, "group2", links));
            assertTrue(containsUserOrGroup(null, "group3", links));

            // do not mention user, all cases should be selected
            List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().asc().list();

            assertEquals(3, caseDefinitions.size());

            assertEquals("case1", caseDefinitions.get(0).getKey());
            assertEquals("case2", caseDefinitions.get(1).getKey());
            assertEquals("case3", caseDefinitions.get(2).getKey());

            // check user1, case2 has two authorized starters, of which one is "user1"
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().asc().startableByUser("user1").list();

            assertEquals(1, caseDefinitions.size());
            assertEquals("case2", caseDefinitions.get(0).getKey());

            // no ccase could be started with "user4"
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().startableByUser("user4").list();
            assertEquals(0, caseDefinitions.size());

            // "userInGroup3" is in "group3" and can start only case 3 via group authorization
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().startableByUser("userInGroup3").list();
            assertEquals(1, caseDefinitions.size());
            assertEquals("case3", caseDefinitions.get(0).getKey());

            // "userInGroup2" can start case 3, via both user and group authorizations
            // but we have to be sure that case 3 appears only once
            cmmnRepositoryService.addCandidateStarterUser(caseDefinitions.get(0).getId(), "userInGroup2");
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().startableByUser("userInGroup2").list();
            assertEquals(1, caseDefinitions.size());
            assertEquals("case3", caseDefinitions.get(0).getKey());

        } finally {
            tearDownUsersAndGroups();
        }
    }

    private boolean containsUserOrGroup(String userId, String groupId, List<IdentityLink> links) {
        boolean found = false;
        for (IdentityLink identityLink : links) {
            if (userId != null && userId.equals(identityLink.getUserId())) {
                found = true;
                break;
            } else if (groupId != null && groupId.equals(identityLink.getGroupId())) {
                found = true;
                break;
            }
        }
        return found;
    }

}

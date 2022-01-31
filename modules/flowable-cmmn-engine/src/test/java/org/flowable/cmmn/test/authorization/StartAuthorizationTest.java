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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.extractProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CaseDefinitionQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
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

        IdmEngineConfiguration idmEngineConfiguration = (IdmEngineConfiguration) cmmnEngine.getCmmnEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
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
            assertThat(latestCaseDef).isNotNull();
            List<IdentityLink> links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).isEmpty();

            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case2").singleResult();
            assertThat(latestCaseDef).isNotNull();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(extractProperty("userId").from(links))
                    .containsExactlyInAnyOrder("user1", "user2");

            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case3").singleResult();
            assertThat(latestCaseDef).isNotNull();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links)
                    .extracting(IdentityLink::getUserId)
                    .containsExactly("user1");

            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case4").singleResult();
            assertThat(latestCaseDef).isNotNull();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).hasSize(3);
            assertThat(extractProperty("groupId").from(links))
                    .contains("group1", "group2");
            assertThat(extractProperty("userId").from(links))
                    .contains("user1");

            // Case instance identity links should not have an impact on the identityLinks query
            Authentication.setAuthenticatedUserId("user1");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(latestCaseDef.getId()).start();
            List<IdentityLink> identityLinksForCaseInstance = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(identityLinksForCaseInstance.size()).isPositive();

            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).hasSize(3);

        } finally {
            tearDownUsersAndGroups();
            Authentication.setAuthenticatedUserId(null);
        }
    }

    @Test
    @CmmnDeployment
    public void testAddAndRemoveIdentityLinks() throws Exception {

        setupUsersAndGroups();

        try {
            CaseDefinition latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneTaskCase").singleResult();
            assertThat(latestCaseDef).isNotNull();
            List<IdentityLink> links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).isEmpty();

            cmmnRepositoryService.addCandidateStarterGroup(latestCaseDef.getId(), "group1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links)
                    .extracting(IdentityLinkInfo::getGroupId)
                    .containsExactly("group1");

            cmmnRepositoryService.addCandidateStarterUser(latestCaseDef.getId(), "user1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).hasSize(2);
            assertThat(extractProperty("groupId").from(links))
                    .contains("group1");
            assertThat(extractProperty("userId").from(links))
                    .contains("user1");

            cmmnRepositoryService.deleteCandidateStarterGroup(latestCaseDef.getId(), "nonexisting");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).hasSize(2);

            cmmnRepositoryService.deleteCandidateStarterGroup(latestCaseDef.getId(), "group1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links)
                    .extracting(IdentityLinkInfo::getUserId)
                    .containsExactly("user1");

            cmmnRepositoryService.deleteCandidateStarterUser(latestCaseDef.getId(), "user1");
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).isEmpty();

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
            assertThat(links).isEmpty();

            // user1 and user2 are potential starters of Case 2
            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case2").singleResult();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(extractProperty("userId").from(links))
                    .containsExactlyInAnyOrder("user1", "user2");

            // Case 3 has 3 groups as authorized starter groups
            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("case3").singleResult();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(extractProperty("groupId").from(links))
                    .containsExactlyInAnyOrder("group1", "group2", "group3");

            // do not mention user, all cases should be selected
            List<CaseDefinition> caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().list();
            List<String> caseDefinitionIds = new ArrayList<>();
            for (CaseDefinition caseDefinition : caseDefinitions) {
                caseDefinitionIds.add(caseDefinition.getKey());
            }
            assertThat(caseDefinitionIds)
                    .containsExactly("case1", "case2", "case3");

            // check user1, case2 has two authorized starters, of which one is "user1"
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().orderByCaseDefinitionName().asc().startableByUser("user1").list();
            assertThat(caseDefinitions)
                    .extracting(CaseDefinition::getKey)
                    .containsExactly("case2");

            // no case could be started with "user4"
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().startableByUser("user4").list();
            assertThat(caseDefinitions).isEmpty();

            // "userInGroup3" is in "group3" and can start only case 3 via group authorization
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().startableByUser("userInGroup3").list();
            assertThat(caseDefinitions)
                    .extracting(CaseDefinition::getKey)
                    .containsExactly("case3");

            // "userInGroup2" can start case 3, via both user and group authorizations
            // but we have to be sure that case 3 appears only once
            cmmnRepositoryService.addCandidateStarterUser(caseDefinitions.get(0).getId(), "userInGroup2");
            caseDefinitions = cmmnRepositoryService.createCaseDefinitionQuery().startableByUser("userInGroup2").list();
            assertThat(caseDefinitions)
                    .extracting(CaseDefinition::getKey)
                    .containsExactly("case3");

            // when groups are defined they should be used instead

            // "group1" can start case3
            assertThat(identityService.createGroupQuery().groupMember("user4").list()).isEmpty();
            assertThat(cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups("user4", Collections.singletonList("group1")).list())
                    .extracting(CaseDefinition::getKey)
                    .containsExactly("case3");

            // "userInGroup3" can only start case3 via group authorization, "unknownGroup" cannot start any process
            assertThat(
                    cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups("userInGroup3", Collections.singletonList("unknownGroup")).list())
                    .extracting(CaseDefinition::getKey)
                    .isEmpty();

            // "group3" can only start case3, query should work if no user is defined
            assertThat(cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups(null, Collections.singletonList("group3")).list())
                    .extracting(CaseDefinition::getKey)
                    .containsExactly("case3");

            // "userInGroup3" can only start case3 via group authorization, passed empty or null groups should still be used
            assertThat(cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups("userInGroup3", Collections.emptyList()).list())
                    .extracting(CaseDefinition::getKey)
                    .isEmpty();
            assertThat(cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups("userInGroup3", null).list())
                    .extracting(CaseDefinition::getKey)
                    .isEmpty();

            // "group3" can only start case3, and user1 can start case2
            assertThat(cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups("user1", Collections.singletonList("group3")).list())
                    .extracting(CaseDefinition::getKey)
                    .containsExactlyInAnyOrder("case2", "case3");

            // SQL Server has a limit of 2100 on how many parameters a query might have
            int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(cmmnEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

            Set<String> testGroups = new HashSet<>(maxGroups);
            for (int i = 0; i < maxGroups; i++) {
                testGroups.add("groupa" + i);
            }
            
            CaseDefinitionQuery caseDefinitionQuery = cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups(null, testGroups);
            assertThat(caseDefinitionQuery.count()).isEqualTo(0);
            assertThat(caseDefinitionQuery.list()).hasSize(0);
            
            testGroups.add("group1");
            
            caseDefinitionQuery = cmmnRepositoryService.createCaseDefinitionQuery().startableByUserOrGroups(null, testGroups);
            assertThat(caseDefinitionQuery.count()).isEqualTo(1);
            assertThat(caseDefinitionQuery.list()).hasSize(1);

        } finally {
            tearDownUsersAndGroups();
        }
    }

    @Test
    @CmmnDeployment
    public void testExpressionsInCandidateStarters() throws Exception {

        setupUsersAndGroups();

        try {
            //test simple expression e.g. "${"user1"}"
            CaseDefinition latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("expressionCase1").singleResult();
            assertThat(latestCaseDef).isNotNull();
            List<IdentityLink> links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).hasSize(2);
            assertThat(extractProperty("groupId").from(links))
                    .contains("group3");
            assertThat(extractProperty("userId").from(links))
                    .contains("user1");

            //test comma-seperated candidates inside expression e.g. "${"user1,user2"}"
            latestCaseDef = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("expressionCase2").singleResult();
            assertThat(latestCaseDef).isNotNull();
            links = cmmnRepositoryService.getIdentityLinksForCaseDefinition(latestCaseDef.getId());
            assertThat(links).hasSize(4);
            assertThat(extractProperty("groupId").from(links))
                    .contains("group2", "group3");
            assertThat(extractProperty("userId").from(links))
                    .contains("user1", "user2");
        } finally {
            tearDownUsersAndGroups();
            Authentication.setAuthenticatedUserId(null);
        }
    }

}

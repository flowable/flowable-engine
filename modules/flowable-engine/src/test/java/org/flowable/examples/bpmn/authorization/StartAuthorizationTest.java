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

package org.flowable.examples.bpmn.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.engine.IdentityService;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Test;

/**
 * @author Saeid Mirzaei
 * @author Tijs Rademakers
 */

public class StartAuthorizationTest extends PluggableFlowableTestCase {

    IdentityService identityService;

    User userInGroup1;
    User userInGroup2;
    User userInGroup3;

    Group group1;
    Group group2;
    Group group3;

    protected void setUpUsersAndGroups() throws Exception {

        identityService = processEngine.getIdentityService();

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
    @Deployment
    public void testIdentityLinks() throws Exception {

        setUpUsersAndGroups();

        try {
            ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process1").singleResult();
            assertThat(latestProcessDef).isNotNull();
            List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links).isEmpty();

            latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process2").singleResult();
            assertThat(latestProcessDef).isNotNull();
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links)
                    .extracting(IdentityLink::getUserId)
                    .containsExactlyInAnyOrder("user1", "user2");

            latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process3").singleResult();
            assertThat(latestProcessDef).isNotNull();
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links)
                    .extracting(IdentityLink::getUserId)
                    .containsExactly("user1");

            latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process4").singleResult();
            assertThat(latestProcessDef).isNotNull();
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links)
                    .extracting(IdentityLink::getUserId, IdentityLink::getGroupId)
                    .containsExactlyInAnyOrder(
                            tuple("userInGroup2", null),
                            tuple(null, "group1"),
                            tuple(null, "group2"),
                            tuple(null, "group3")
                    );

        } finally {
            tearDownUsersAndGroups();
        }
    }

    @Test
    @Deployment
    public void testAddAndRemoveIdentityLinks() throws Exception {

        setUpUsersAndGroups();

        try {
            ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("potentialStarterNoDefinition")
                    .singleResult();
            assertThat(latestProcessDef).isNotNull();
            List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links).isEmpty();

            repositoryService.addCandidateStarterGroup(latestProcessDef.getId(), "group1");
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links)
                    .extracting(IdentityLink::getGroupId)
                    .containsExactly("group1");

            repositoryService.addCandidateStarterUser(latestProcessDef.getId(), "user1");
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links)
                    .extracting(IdentityLink::getUserId, IdentityLink::getGroupId)
                    .containsExactlyInAnyOrder(
                            tuple("user1", null),
                            tuple(null, "group1")
                    );

            repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), "nonexisting");
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links).hasSize(2);

            repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), "group1");
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links)
                    .extracting(IdentityLink::getUserId)
                    .containsExactly("user1");

            repositoryService.deleteCandidateStarterUser(latestProcessDef.getId(), "user1");
            links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
            assertThat(links).isEmpty();

        } finally {
            tearDownUsersAndGroups();
        }
    }

    @Test
    @Deployment
    public void testPotentialStarter() throws Exception {
        // first check an unauthorized user. An exception is expected

        setUpUsersAndGroups();

        try {

            // Authentication should not be done. So an unidentified user should
            // also be able to start the process
            identityService.setAuthenticatedUserId("unauthorizedUser");
            try {
                runtimeService.startProcessInstanceByKey("potentialStarter");
            } catch (Exception e) {
                fail("No StartAuthorizationException expected, " + e.getClass().getName() + " caught.");
            }

            // check with an authorized user obviously it should be no problem
            // starting the process
            identityService.setAuthenticatedUserId("user1");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("potentialStarter");
            assertProcessEnded(processInstance.getId());
            assertThat(processInstance.isEnded()).isTrue();

            // check extensionElements with : <formalExpression>group2,
            // group(group3), user(user3)</formalExpression>
            ProcessDefinition potentialStarter = repositoryService.createProcessDefinitionQuery().processDefinitionKey("potentialStarter").startableByUser("user1").latestVersion().singleResult();
            assertThat(potentialStarter).isNotNull();

            potentialStarter = repositoryService.createProcessDefinitionQuery().processDefinitionKey("potentialStarter").startableByUser("user3")
                    .latestVersion().singleResult();
            assertThat(potentialStarter).isNotNull();

            potentialStarter = repositoryService.createProcessDefinitionQuery().processDefinitionKey("potentialStarter").startableByUser("userInGroup2")
                    .latestVersion().singleResult();
            assertThat(potentialStarter).isNotNull();

            potentialStarter = repositoryService.createProcessDefinitionQuery().processDefinitionKey("potentialStarter").startableByUser("userInGroup3")
                    .latestVersion().singleResult();
            assertThat(potentialStarter).isNotNull();
        } finally {

            tearDownUsersAndGroups();
        }
    }

    /*
     * if there is no security definition, then user authorization check is not done. This ensures backward compatibility
     */
    @Test
    @Deployment
    public void testPotentialStarterNoDefinition() throws Exception {
        identityService = processEngine.getIdentityService();

        identityService.setAuthenticatedUserId("someOneFromMars");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("potentialStarterNoDefinition");
        assertThat(processInstance.getId()).isNotNull();
        assertProcessEnded(processInstance.getId());
        assertThat(processInstance.isEnded()).isTrue();
    }

    // this test checks the list without user constraint
    @Test
    @Deployment
    public void testProcessDefinitionList() throws Exception {

        setUpUsersAndGroups();
        try {

            // Process 1 has no potential starters
            ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process1").singleResult();
            List<User> authorizedUsers = identityService.getPotentialStarterUsers(latestProcessDef.getId());
            assertThat(authorizedUsers).isEmpty();

            // user1 and user2 are potential starters of Process2
            latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process2").singleResult();
            authorizedUsers = identityService.getPotentialStarterUsers(latestProcessDef.getId());
            assertThat(authorizedUsers)
                .extracting(User::getId)
                .containsExactlyInAnyOrder("user1", "user2");

            // Process 2 has no potential starter groups
            latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process2").singleResult();
            List<Group> authorizedGroups = identityService.getPotentialStarterGroups(latestProcessDef.getId());
            assertThat(authorizedGroups).isEmpty();

            // Process 3 has 3 groups as authorized starter groups
            latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process4").singleResult();
            authorizedGroups = identityService.getPotentialStarterGroups(latestProcessDef.getId());
            assertThat(authorizedGroups)
                    .extracting(Group::getId)
                    .containsExactlyInAnyOrder("group1", "group2", "group3");

            // do not mention user, all processes should be selected
            List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionName().asc().list();
            assertThat(processDefinitions)
                    .extracting(ProcessDefinition::getKey)
                    .containsExactly("process1", "process2", "process3", "process4");

            // check user1, process3 has "user1" as only authorized starter, and
            // process2 has two authorized starters, of which one is "user1"
            processDefinitions = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionName().asc().startableByUser("user1").list();
            assertThat(processDefinitions)
                    .extracting(ProcessDefinition::getKey)
                    .containsExactly("process2", "process3");

            // "user2" can only start process2
            processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("user2").list();
            assertThat(processDefinitions)
                    .extracting(ProcessDefinition::getKey)
                    .containsExactly("process2");

            // no process could be started with "user4"
            processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("user4").list();
            assertThat(processDefinitions).isEmpty();

            // "userInGroup3" is in "group3" and can start only process4 via group authorization
            processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("userInGroup3").list();
            assertThat(processDefinitions)
                    .extracting(ProcessDefinition::getKey)
                    .containsExactly("process4");

            // "userInGroup2" can start process4, via both user and group authorizations
            // but we have to be sure that process4 appears only once
            processDefinitions = repositoryService.createProcessDefinitionQuery().startableByUser("userInGroup2").list();
            assertThat(processDefinitions)
                    .extracting(ProcessDefinition::getKey)
                    .containsExactly("process4");

            // when groups are defined they should be used instead

            // "group1" can start process4
            assertThat(identityService.createGroupQuery().groupMember("user4").list()).isEmpty();
            assertThat(repositoryService.createProcessDefinitionQuery().startableByUserOrGroups("user4", Collections.singletonList("group1")).list())
                    .extracting(ProcessDefinition::getKey)
                    .containsExactlyInAnyOrder("process4");

            // "userInGroup3" can only start process4 via group authorization, "unknownGroup" cannot start any process
            assertThat(repositoryService.createProcessDefinitionQuery().startableByUserOrGroups("userInGroup3", Collections.singletonList("unknownGroup")).list())
                .extracting(ProcessDefinition::getKey)
                .isEmpty();

            // "group3" can only start process4, query should work if no user is defined
            assertThat(repositoryService.createProcessDefinitionQuery().startableByUserOrGroups(null, Collections.singletonList("group3")).list())
                .extracting(ProcessDefinition::getKey)
                .containsExactlyInAnyOrder("process4");

            // "userInGroup3" can only start process4 via group authorization, passed empty or null groups should still be used
            assertThat(repositoryService.createProcessDefinitionQuery().startableByUserOrGroups("userInGroup3", Collections.emptyList()).list())
                .extracting(ProcessDefinition::getKey)
                .isEmpty();
            assertThat(repositoryService.createProcessDefinitionQuery().startableByUserOrGroups("userInGroup3", null).list())
                .extracting(ProcessDefinition::getKey)
                .isEmpty();

            // "group3" can only start process4, and user1 can start process2 and process3
            assertThat(repositoryService.createProcessDefinitionQuery().startableByUserOrGroups("user1", Collections.singletonList("group3")).list())
                .extracting(ProcessDefinition::getKey)
                .containsExactlyInAnyOrder("process2", "process3", "process4");

            // SQL Server has a limit of 2100 on how many parameters a query might have
            int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(processEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

            Set<String> testGroups = new HashSet<>(maxGroups);
            for (int i = 0; i < maxGroups; i++) {
                testGroups.add("groupa" + i);
            }
            
            ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery().startableByUserOrGroups(null, testGroups);
            assertThat(processDefinitionQuery.count()).isEqualTo(0);
            assertThat(processDefinitionQuery.list()).hasSize(0);
            
            testGroups.add("group1");
            
            processDefinitionQuery = repositoryService.createProcessDefinitionQuery().startableByUserOrGroups(null, testGroups);
            assertThat(processDefinitionQuery.count()).isEqualTo(1);
            assertThat(processDefinitionQuery.list()).hasSize(1);

        } finally {
            tearDownUsersAndGroups();
        }
    }

}

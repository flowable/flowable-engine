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
package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.jupiter.api.Test;

class ProcessInstanceCreateWithIdentityLinkTest extends PluggableFlowableTestCase {

    static final String USER_ALICE = "Alice";
    static final String USER_BOB = "Bob";

    static final String GROUP_ALPHA = "alpha";
    static final String GROUP_BETA = "beta";
    static final String GROUP_GAMMA = "gamma";
    static final String GROUP_DELTA = "delta";

    static final String CUSTOM_LINK_TYPE_1 = "custom_test_type_1";
    static final String CUSTOM_LINK_TYPE_2 = "custom_test_type_2";
    static final String CUSTOM_LINK_TYPE_3 = "custom_test_type_3";

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    void testProcessInstanceCreateWithSingleUserLink() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                        .userIdentityLink(IdentityLinkType.OWNER, USER_ALICE).start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertUserLinks(IdentityLinkType.OWNER, identityLinks, USER_ALICE);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    void testProcessInstanceCreateWithSeveralUserLinksAddedOneByOne() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                        .userIdentityLink(IdentityLinkType.OWNER, USER_BOB)
                        .userIdentityLink(CUSTOM_LINK_TYPE_1, USER_ALICE)
                        .userIdentityLink(CUSTOM_LINK_TYPE_1, USER_BOB)
                        .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).hasSize(3);

        assertUserLinks(IdentityLinkType.OWNER, identityLinks, USER_BOB);
        assertUserLinks(CUSTOM_LINK_TYPE_1, identityLinks, USER_ALICE, USER_BOB);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    void testProcessInstanceCreateWithSeveralUserLinksInBulk() {

        Map<String, Set<String>> userIdentityLinks = new HashMap<>();
        userIdentityLinks.put(IdentityLinkType.OWNER, createSet(USER_ALICE));
        userIdentityLinks.put(CUSTOM_LINK_TYPE_1, createSet(USER_ALICE, USER_BOB));

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                        .userIdentityLinks(userIdentityLinks)
                        .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).hasSize(3);

        assertUserLinks(IdentityLinkType.OWNER, identityLinks, USER_ALICE);
        assertUserLinks(CUSTOM_LINK_TYPE_1, identityLinks, USER_ALICE, USER_BOB);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    void testProcessInstanceCreateWithSeveralUsersAndGroups() {

        Map<String, Set<String>> userIdentityLinks = new HashMap<>();
        userIdentityLinks.put(IdentityLinkType.OWNER, createSet(USER_ALICE));
        userIdentityLinks.put(CUSTOM_LINK_TYPE_1, createSet(USER_ALICE, USER_BOB));

        Map<String, Set<String>> groupIdentityLinks = new HashMap<>();
        groupIdentityLinks.put(CUSTOM_LINK_TYPE_1, createSet(GROUP_ALPHA, GROUP_BETA));
        groupIdentityLinks.put(CUSTOM_LINK_TYPE_2, createSet(GROUP_GAMMA));

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                        .userIdentityLinks(userIdentityLinks)
                        .groupIdentityLink(CUSTOM_LINK_TYPE_1, GROUP_ALPHA)
                        .groupIdentityLinks(groupIdentityLinks)
                        .groupIdentityLink(CUSTOM_LINK_TYPE_2, GROUP_DELTA)
                        .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).hasSize(7);

        assertUserLinks(IdentityLinkType.OWNER, identityLinks, USER_ALICE);
        assertUserLinks(CUSTOM_LINK_TYPE_1, identityLinks, USER_ALICE, USER_BOB);

        assertGroupLinks(CUSTOM_LINK_TYPE_1, identityLinks, GROUP_ALPHA, GROUP_BETA);
        assertGroupLinks(CUSTOM_LINK_TYPE_2, identityLinks, GROUP_GAMMA, GROUP_DELTA);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    void testProcessInstanceCreateWithIdentityLinksToBeIgnored() {

        Set<String> setContainingNull = new HashSet<String>();
        setContainingNull.add(null);

        Map<String, Set<String>> userLinksContainingNullValues = new HashMap<>();
        userLinksContainingNullValues.put(null, createSet(USER_ALICE));
        userLinksContainingNullValues.put(CUSTOM_LINK_TYPE_2, null);
        userLinksContainingNullValues.put(CUSTOM_LINK_TYPE_3, setContainingNull);

        Map<String, Set<String>> groupLinksContainingNullValues = new HashMap<>();
        groupLinksContainingNullValues.put(null, createSet(GROUP_ALPHA));
        groupLinksContainingNullValues.put(CUSTOM_LINK_TYPE_2, null);
        groupLinksContainingNullValues.put(CUSTOM_LINK_TYPE_3, setContainingNull);

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                        .userIdentityLink(CUSTOM_LINK_TYPE_1, USER_ALICE)
                        .userIdentityLink(null, null)
                        .userIdentityLink(CUSTOM_LINK_TYPE_2, null)
                        .userIdentityLink(null, USER_BOB)
                        .userIdentityLinks(userLinksContainingNullValues)
                        .groupIdentityLink(CUSTOM_LINK_TYPE_1, GROUP_ALPHA)
                        .groupIdentityLink(null, null)
                        .groupIdentityLink(CUSTOM_LINK_TYPE_2, null)
                        .groupIdentityLink(null, GROUP_BETA)
                        .groupIdentityLinks(groupLinksContainingNullValues)
                        .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).hasSize(2);

        assertUserLinks(CUSTOM_LINK_TYPE_1, identityLinks, USER_ALICE);
        assertGroupLinks(CUSTOM_LINK_TYPE_1, identityLinks, GROUP_ALPHA);
    }

    private void assertUserLinks(String linkType, List<IdentityLink> unfilteredLinks, String... userIds) {
        assertLinks(linkType, unfilteredLinks, IdentityLinkInfo::getUserId, userIds);
    }

    private void assertGroupLinks(String linkType, List<IdentityLink> unfilteredLinks, String... groupIds) {
        assertLinks(linkType, unfilteredLinks, IdentityLinkInfo::getGroupId, groupIds);
    }

    private void assertLinks(String linkType, List<IdentityLink> unfilteredLinks,
                    Function<IdentityLinkInfo, String> idRetriever, String... expectedIds) {

        Stream<String> relevantIds = unfilteredLinks.stream()
                        .filter(identityLink -> identityLink.getType().equals(linkType))
                        .map(idRetriever)
                        .filter(Objects::nonNull);
        assertThat(relevantIds).containsOnly(expectedIds);
    }

    private Set<String> createSet(String... entries) {
        Set<String> stringSet = new HashSet<>();
        for (String entry : entries) {
            stringSet.add(entry);
        }
        return stringSet;
    }
}

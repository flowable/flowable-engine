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
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.task.Event;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.jupiter.api.Test;

/**
 * @author Wendel Kerr
 */
public class ProcessInstanceIdentityLinksTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/IdentityLinksProcess.bpmn20.xml")
    public void testParticipantUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();

        runtimeService.addParticipantUser(processInstanceId, "kermit");

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);
        IdentityLink identityLink = identityLinks.get(0);

        assertThat(identityLink.getGroupId()).isNull();
        assertThat(identityLink.getUserId()).isEqualTo("kermit");
        assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.PARTICIPANT);
        assertThat(identityLink.getProcessInstanceId()).isEqualTo(processInstanceId);

        assertThat(identityLinks).hasSize(1);

        runtimeService.deleteParticipantUser(processInstanceId, "kermit");

        assertThat(runtimeService.getIdentityLinksForProcessInstance(processInstanceId)).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/IdentityLinksProcess.bpmn20.xml")
    public void testCandidateGroupLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();

        runtimeService.addParticipantGroup(processInstanceId, "muppets");

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getProcessInstanceId)
                .containsExactly(tuple("muppets", null, IdentityLinkType.PARTICIPANT, processInstanceId));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<Event> processInstanceEvents = runtimeService.getProcessInstanceEvents(processInstanceId);
            assertThat(processInstanceEvents)
                    .extracting(Event::getAction)
                    .containsExactly(Event.ACTION_ADD_GROUP_LINK);
            List<String> processInstanceEventMessageParts = processInstanceEvents.get(0).getMessageParts();
            assertThat(processInstanceEventMessageParts)
                    .containsOnly("muppets", IdentityLinkType.PARTICIPANT);
        }

        runtimeService.deleteParticipantGroup(processInstanceId, "muppets");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<Event> processInstanceEvents = runtimeService.getProcessInstanceEvents(processInstanceId);
            assertThat(processInstanceEvents)
                    .extracting(Event::getAction)
                    .containsExactlyInAnyOrder(Event.ACTION_DELETE_GROUP_LINK, Event.ACTION_ADD_GROUP_LINK);
            List<String> processInstanceEventMessageParts = processInstanceEvents.get(0).getMessageParts();
            assertThat(processInstanceEventMessageParts)
                    .containsOnly("muppets", IdentityLinkType.PARTICIPANT);
        }

        assertThat(runtimeService.getIdentityLinksForProcessInstance(processInstanceId)).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/IdentityLinksProcess.bpmn20.xml")
    public void testCustomTypeUserLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();

        runtimeService.addUserIdentityLink(processInstanceId, "kermit", "interestee");

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getProcessInstanceId)
                .containsExactly(tuple(null, "kermit", "interestee", processInstanceId));

        runtimeService.deleteUserIdentityLink(processInstanceId, "kermit", "interestee");

        assertThat(runtimeService.getIdentityLinksForProcessInstance(processInstanceId)).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/IdentityLinksProcess.bpmn20.xml")
    public void testCreateAndRemoveUserIdentityLinksInSameCommand() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();

        managementService.executeCommand(commandContext -> {
            runtimeService.addUserIdentityLink(processInstanceId, "kermit", "interested");
            runtimeService.addUserIdentityLink(processInstanceId, "kermit", "custom");
            runtimeService.deleteUserIdentityLink(processInstanceId, "kermit", "interested");
            return null;
        });

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);

        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getUserId, IdentityLinkInfo::getType, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple("kermit", "custom", processInstanceId)
            );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/IdentityLinksProcess.bpmn20.xml")
    public void testCustomLinkGroupLink() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();

        runtimeService.addGroupIdentityLink(processInstanceId, "muppets", "playing");

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getGroupId, IdentityLink::getUserId, IdentityLink::getType, IdentityLink::getProcessInstanceId)
                .containsExactly(tuple("muppets", null, "playing", processInstanceId));

        runtimeService.deleteGroupIdentityLink(processInstanceId, "muppets", "playing");

        assertThat(runtimeService.getIdentityLinksForProcessInstance(processInstanceId)).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/IdentityLinksProcess.bpmn20.xml")
    public void testCreateAndRemoveGroupIdentityLinksInSameCommand() {
        runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

        String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();

        managementService.executeCommand(commandContext -> {
            runtimeService.addGroupIdentityLink(processInstanceId, "muppets", "playing");
            runtimeService.addGroupIdentityLink(processInstanceId, "muppets", "custom");
            runtimeService.deleteGroupIdentityLink(processInstanceId, "muppets", "playing");
            return null;
        });

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstanceId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getGroupId, IdentityLinkInfo::getType, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple("muppets", "custom", processInstanceId)
            );
    }

}

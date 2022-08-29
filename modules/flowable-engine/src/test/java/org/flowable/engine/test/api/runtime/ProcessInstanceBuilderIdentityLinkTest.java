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
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.junit.jupiter.api.Test;

/**
 * Testing the owner and assignee identity link setup methods within the process instance builder API.
 *
 * @author Micha Kiener
 */
public class ProcessInstanceBuilderIdentityLinkTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testProcessInstanceBuilderWithOwner() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTaskProcess")
            .owner("kermit")
            .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId())
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactly(
                    tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId())
                );
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testProcessInstanceBuilderWithAssignee() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTaskProcess")
            .assignee("kermit")
            .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "kermit", null, processInstance.getId())
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactly(
                    tuple(IdentityLinkType.ASSIGNEE, "kermit", null, processInstance.getId())
                );
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testProcessInstanceBuilderWithOwnerAndAssignee() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTaskProcess")
            .owner("kermit")
            .assignee("denise")
            .start();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId()),
                tuple(IdentityLinkType.ASSIGNEE, "denise", null, processInstance.getId())
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId()),
                    tuple(IdentityLinkType.ASSIGNEE, "denise", null, processInstance.getId())
                );
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testProcessInstanceBuilderWithOwnerAsync() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTaskProcess")
            .owner("kermit")
            .startAsync();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId())
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactly(
                    tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId())
                );
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testProcessInstanceBuilderWithAssigneeAsync() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTaskProcess")
            .assignee("kermit")
            .startAsync();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "kermit", null, processInstance.getId())
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactly(
                    tuple(IdentityLinkType.ASSIGNEE, "kermit", null, processInstance.getId())
                );
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testProcessInstanceBuilderWithOwnerAndAssigneeAsync() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("oneTaskProcess")
            .owner("kermit")
            .assignee("denise")
            .startAsync();

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId()),
                tuple(IdentityLinkType.ASSIGNEE, "denise", null, processInstance.getId())
            );

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getProcessInstanceId)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "kermit", null, processInstance.getId()),
                    tuple(IdentityLinkType.ASSIGNEE, "denise", null, processInstance.getId())
                );
        }
    }
}

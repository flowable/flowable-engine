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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.jupiter.api.Test;

public class ProcessInstanceDueDateAndClaimTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetProcessInstanceDueDate() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        Date dueDate = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        runtimeService.setProcessInstanceDueDate(processInstance.getId(), dueDate);

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isEqualTo(dueDate);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getDueDate()).isEqualTo(dueDate);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetProcessInstanceDueDateToNull() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.setProcessInstanceDueDate(processInstance.getId(), Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)));

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isNotNull();

        runtimeService.setProcessInstanceDueDate(processInstance.getId(), null);

        updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getDueDate()).isNull();
        }
    }

    @Test
    public void testSetProcessInstanceDueDateWithNullId() {
        assertThatThrownBy(() -> runtimeService.setProcessInstanceDueDate(null, new Date()))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testSetProcessInstanceDueDateWithNonExistingId() {
        assertThatThrownBy(() -> runtimeService.setProcessInstanceDueDate("nonExistingId", new Date()))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testClaimProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.claimProcessInstance(processInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).contains(IdentityLinkType.ASSIGNEE);
        assertThat(identityLinks).extracting(IdentityLink::getUserId).contains("kermit");

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getClaimTime()).isNotNull();
        assertThat(updatedInstance.getClaimedBy()).isEqualTo("kermit");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getClaimTime()).isNotNull();
            assertThat(historicProcessInstance.getClaimedBy()).isEqualTo("kermit");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testClaimAlreadyClaimedProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.claimProcessInstance(processInstance.getId(), "kermit");

        assertThatThrownBy(() -> runtimeService.claimProcessInstance(processInstance.getId(), "fozzie"))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("is already claimed");
    }

    @Test
    public void testClaimProcessInstanceWithNullId() {
        assertThatThrownBy(() -> runtimeService.claimProcessInstance(null, "kermit"))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testClaimProcessInstanceWithNonExistingId() {
        assertThatThrownBy(() -> runtimeService.claimProcessInstance("nonExistingId", "kermit"))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUnclaimProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.claimProcessInstance(processInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).contains(IdentityLinkType.ASSIGNEE);

        runtimeService.unclaimProcessInstance(processInstance.getId());

        identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).doesNotContain(IdentityLinkType.ASSIGNEE);

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getClaimTime()).isNull();
        assertThat(updatedInstance.getClaimedBy()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getClaimTime()).isNull();
            assertThat(historicProcessInstance.getClaimedBy()).isNull();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUnclaimAndReClaimProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        runtimeService.claimProcessInstance(processInstance.getId(), "kermit");
        runtimeService.unclaimProcessInstance(processInstance.getId());
        runtimeService.claimProcessInstance(processInstance.getId(), "fozzie");

        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).contains(IdentityLinkType.ASSIGNEE);
        assertThat(identityLinks).extracting(IdentityLink::getUserId).contains("fozzie");

        ProcessInstance updatedInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId()).singleResult();
        assertThat(updatedInstance.getClaimTime()).isNotNull();
        assertThat(updatedInstance.getClaimedBy()).isEqualTo("fozzie");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getClaimTime()).isNotNull();
            assertThat(historicProcessInstance.getClaimedBy()).isEqualTo("fozzie");
        }
    }
}

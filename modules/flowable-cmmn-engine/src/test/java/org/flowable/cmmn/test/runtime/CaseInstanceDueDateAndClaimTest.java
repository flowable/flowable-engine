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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.jupiter.api.Test;

public class CaseInstanceDueDateAndClaimTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testSetCaseInstanceDueDate() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Date dueDate = new Date();
        cmmnRuntimeService.setCaseInstanceDueDate(caseInstance.getId(), dueDate);

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isEqualTo(dueDate);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getDueDate()).isEqualTo(dueDate);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testSetCaseInstanceDueDateToNull() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.setCaseInstanceDueDate(caseInstance.getId(), new Date());

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isNotNull();

        cmmnRuntimeService.setCaseInstanceDueDate(caseInstance.getId(), null);

        updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getDueDate()).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getDueDate()).isNull();
        }
    }

    @Test
    public void testSetCaseInstanceDueDateWithNullId() {
        assertThatThrownBy(() -> cmmnRuntimeService.setCaseInstanceDueDate(null, new Date()))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testSetCaseInstanceDueDateWithNonExistingId() {
        assertThatThrownBy(() -> cmmnRuntimeService.setCaseInstanceDueDate("nonExistingId", new Date()))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testClaimCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.claimCaseInstance(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).contains(IdentityLinkType.ASSIGNEE);
        assertThat(identityLinks).extracting(IdentityLink::getUserId).contains("kermit");

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getClaimTime()).isNotNull();
        assertThat(updatedInstance.getClaimedBy()).isEqualTo("kermit");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getClaimTime()).isNotNull();
            assertThat(historicCaseInstance.getClaimedBy()).isEqualTo("kermit");
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testClaimAlreadyClaimedCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.claimCaseInstance(caseInstance.getId(), "kermit");

        assertThatThrownBy(() -> cmmnRuntimeService.claimCaseInstance(caseInstance.getId(), "fozzie"))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("is already claimed");
    }

    @Test
    public void testClaimCaseInstanceWithNullId() {
        assertThatThrownBy(() -> cmmnRuntimeService.claimCaseInstance(null, "kermit"))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testClaimCaseInstanceWithNonExistingId() {
        assertThatThrownBy(() -> cmmnRuntimeService.claimCaseInstance("nonExistingId", "kermit"))
                .isInstanceOf(FlowableObjectNotFoundException.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUnclaimCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.claimCaseInstance(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).contains(IdentityLinkType.ASSIGNEE);

        cmmnRuntimeService.unclaimCaseInstance(caseInstance.getId());

        identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).doesNotContain(IdentityLinkType.ASSIGNEE);

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getClaimTime()).isNull();
        assertThat(updatedInstance.getClaimedBy()).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getClaimTime()).isNull();
            assertThat(historicCaseInstance.getClaimedBy()).isNull();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testUnclaimAndReClaimCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        cmmnRuntimeService.claimCaseInstance(caseInstance.getId(), "kermit");
        cmmnRuntimeService.unclaimCaseInstance(caseInstance.getId());
        cmmnRuntimeService.claimCaseInstance(caseInstance.getId(), "fozzie");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks).extracting(IdentityLink::getType).contains(IdentityLinkType.ASSIGNEE);
        assertThat(identityLinks).extracting(IdentityLink::getUserId).contains("fozzie");

        CaseInstance updatedInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(updatedInstance.getClaimTime()).isNotNull();
        assertThat(updatedInstance.getClaimedBy()).isEqualTo("fozzie");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance.getClaimTime()).isNotNull();
            assertThat(historicCaseInstance.getClaimedBy()).isEqualTo("fozzie");
        }
    }
}

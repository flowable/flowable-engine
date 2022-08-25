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
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.junit.Test;

/**
 * Testing the case builder API with the owner / assignee functionality at case start.
 *
 * @author Micha Kiener
 */
public class StartCaseWithIdentityLinksTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testSetOwnerThroughCaseBuilder() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .owner("someUserId")
                .start();

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "someUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                    HistoricIdentityLink::getScopeType)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "someUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
                );
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testSetAssigneeThroughCaseBuilder() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .assignee("someUserId")
                .start();

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "someUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                    HistoricIdentityLink::getScopeType)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.ASSIGNEE, "someUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
                );
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testSetOwnerAndAssigneeThroughCaseBuilder() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .owner("firstUserId")
                .assignee("secondUserId")
                .start();

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "firstUserId", null, caseInstance.getId(), ScopeTypes.CMMN),
                tuple(IdentityLinkType.ASSIGNEE, "secondUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                    HistoricIdentityLink::getScopeType)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "firstUserId", null, caseInstance.getId(), ScopeTypes.CMMN),
                    tuple(IdentityLinkType.ASSIGNEE, "secondUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
                );
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testSetOwnerAndAssigneeThroughCaseBuilderAsync() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .owner("firstUserId")
                .assignee("secondUserId")
                .startAsync();

        // even if the case is started async, the identity links must already have been created, just the case model itself will be
        // evaluated async, therefore the identity links must be present immediately, not async
        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "firstUserId", null, caseInstance.getId(), ScopeTypes.CMMN),
                tuple(IdentityLinkType.ASSIGNEE, "secondUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                    HistoricIdentityLink::getScopeType)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "firstUserId", null, caseInstance.getId(), ScopeTypes.CMMN),
                    tuple(IdentityLinkType.ASSIGNEE, "secondUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
                );
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testSetOwnerAndAssigneeThroughCaseBuilderWithForm() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .owner("firstUserId")
                .assignee("secondUserId")
                .startWithForm();

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "firstUserId", null, caseInstance.getId(), ScopeTypes.CMMN),
                tuple(IdentityLinkType.ASSIGNEE, "secondUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(historicIdentityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                    HistoricIdentityLink::getScopeType)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "firstUserId", null, caseInstance.getId(), ScopeTypes.CMMN),
                    tuple(IdentityLinkType.ASSIGNEE, "secondUserId", null, caseInstance.getId(), ScopeTypes.CMMN)
                );
        }
    }
}

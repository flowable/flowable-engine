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
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 * @author Micha Kiener
 */
public class CaseInstanceIdentityLinksTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testAddUserIdentityLink() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        String caseInstanceId = cmmnRuntimeService.createCaseInstanceQuery().singleResult().getId();

        cmmnRuntimeService.addUserIdentityLink(caseInstanceId, "kermit", "interested");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstanceId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple("interested", "kermit", null, caseInstanceId, ScopeTypes.CMMN)
            );
        cmmnRuntimeService.deleteUserIdentityLink(caseInstanceId, "kermit", "interested");

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstanceId))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCreateAndRemoveUserIdentityLinksInSameCommand() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        String caseInstanceId = cmmnRuntimeService.createCaseInstanceQuery().singleResult().getId();

        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            cmmnRuntimeService.addUserIdentityLink(caseInstanceId, "kermit", "interested");
            cmmnRuntimeService.addUserIdentityLink(caseInstanceId, "kermit", "custom");
            cmmnRuntimeService.deleteUserIdentityLink(caseInstanceId, "kermit", "interested");
            return null;
        });

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstanceId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple("custom", "kermit", null, caseInstanceId, ScopeTypes.CMMN)
            );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testAddGroupIdentityLink() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        String caseInstanceId = cmmnRuntimeService.createCaseInstanceQuery().singleResult().getId();

        cmmnRuntimeService.addGroupIdentityLink(caseInstanceId, "muppets", "playing");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstanceId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple("playing", null, "muppets", caseInstanceId, ScopeTypes.CMMN)
            );

        cmmnRuntimeService.deleteGroupIdentityLink(caseInstanceId, "muppets", "playing");

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstanceId))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCreateAndRemoveGroupIdentityLinksInSameCommand() {
        cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        String caseInstanceId = cmmnRuntimeService.createCaseInstanceQuery().singleResult().getId();

        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            cmmnRuntimeService.addGroupIdentityLink(caseInstanceId, "muppets", "playing");
            cmmnRuntimeService.addGroupIdentityLink(caseInstanceId, "muppets", "custom");
            cmmnRuntimeService.deleteGroupIdentityLink(caseInstanceId, "muppets", "playing");
            return null;
        });

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstanceId);
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple("custom", null, "muppets", caseInstanceId, ScopeTypes.CMMN)
            );
    }

    @Test
    @CmmnDeployment
    public void testCandidateGroupFromModel() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testCandidateGroup")
            .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        List<IdentityLink> identityLinks = cmmnTaskService.getIdentityLinksForTask(task.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getGroupId)
            .containsExactly("admins");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseAssignee() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setAssignee(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.removeAssignee(caseInstance.getId());

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseAssigneeChange() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setAssignee(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.setAssignee(caseInstance.getId(), "denise");

        identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "denise", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.removeAssignee(caseInstance.getId());

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseAssigneeRemovalWithEmptyUserId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setAssignee(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.ASSIGNEE, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.setAssignee(caseInstance.getId(), null);

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseOwner() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setOwner(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.removeOwner(caseInstance.getId());

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseOwnerChange() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setOwner(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.setOwner(caseInstance.getId(), "denise");

        identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "denise", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.removeOwner(caseInstance.getId());

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseOwnerRemovalWithEmptyUserId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setOwner(caseInstance.getId(), "kermit");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.setOwner(caseInstance.getId(), null);

        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseOwnerAndAssignee() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setOwner(caseInstance.getId(), "kermit");
        cmmnRuntimeService.setAssignee(caseInstance.getId(), "denise");

        List<IdentityLink> identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactlyInAnyOrder(
                tuple(IdentityLinkType.OWNER, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN),
                tuple(IdentityLinkType.ASSIGNEE, "denise", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.removeAssignee(caseInstance.getId());
        identityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(identityLinks)
            .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                IdentityLink::getScopeType)
            .containsExactly(
                tuple(IdentityLinkType.OWNER, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
            );

        cmmnRuntimeService.removeOwner(caseInstance.getId());
        assertThat(cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId()))
            .isEmpty();
    }


    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testCaseOwnerAndAssigneeHistoryEntries() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("oneTaskCase")
            .start();

        cmmnRuntimeService.setOwner(caseInstance.getId(), "kermit");
        cmmnRuntimeService.setAssignee(caseInstance.getId(), "denise");
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> identityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(identityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                    HistoricIdentityLink::getScopeType)
                .containsExactlyInAnyOrder(
                    tuple(IdentityLinkType.OWNER, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN),
                    tuple(IdentityLinkType.ASSIGNEE, "denise", null, caseInstance.getId(), ScopeTypes.CMMN)
                );
        }

        cmmnRuntimeService.removeAssignee(caseInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> identityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(identityLinks)
                .extracting(IdentityLinkInfo::getType, IdentityLinkInfo::getUserId, IdentityLinkInfo::getGroupId, IdentityLinkInfo::getScopeId,
                    HistoricIdentityLink::getScopeType)
                .containsExactly(
                    tuple(IdentityLinkType.OWNER, "kermit", null, caseInstance.getId(), ScopeTypes.CMMN)
                );
        }

        cmmnRuntimeService.removeOwner(caseInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()))
                .isEmpty();
        }
    }

    @Test
    public void testSettingOwnerWithWrongCaseId() {
        assertThatThrownBy(() -> cmmnRuntimeService.setOwner("dummy", "kermit"))
            .isInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessage("The case instance with id 'dummy' could not be found as an active case instance.");
    }

    @Test
    public void testSettingAssigneeWithWrongCaseId() {
        assertThatThrownBy(() -> cmmnRuntimeService.setAssignee("dummy", "kermit"))
            .isInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessage("The case instance with id 'dummy' could not be found as an active case instance.");
    }

    @Test
    public void testRemovingOwnerWithWrongCaseId() {
        assertThatThrownBy(() -> cmmnRuntimeService.removeOwner("dummy"))
            .isInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessage("The case instance with id 'dummy' could not be found as an active case instance.");
    }

    @Test
    public void testRemovingAssigneeWithWrongCaseId() {
        assertThatThrownBy(() -> cmmnRuntimeService.removeAssignee("dummy"))
            .isInstanceOf(FlowableIllegalArgumentException.class)
            .hasMessage("The case instance with id 'dummy' could not be found as an active case instance.");
    }
}

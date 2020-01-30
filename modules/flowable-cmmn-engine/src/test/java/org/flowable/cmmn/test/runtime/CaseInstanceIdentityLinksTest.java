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
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Filip Hrisafov
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

}

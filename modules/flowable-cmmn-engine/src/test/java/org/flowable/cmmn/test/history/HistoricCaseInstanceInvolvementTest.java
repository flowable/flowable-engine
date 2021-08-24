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

package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.Before;
import org.junit.Test;

/**
 * @author martin.grofcik
 */
public class HistoricCaseInstanceInvolvementTest extends FlowableCmmnTestCase {

    protected String deploymentId;

    @Before
    public void createCaseInstance() {
        deploymentId = addDeploymentForAutoCleanup(cmmnEngine.getCmmnRepositoryService().createDeployment().addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .deploy());
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
    }

    @Test
    public void getCaseInstanceWithInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceWithTwoInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "gonzo", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceWithTwoInvolvedUserEmptyQuery() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "gonzo", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("").count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("").list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("").singleResult()).isNull();
        }
    }

    @Test
    public void getCaseInstanceWithNonExistingInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("gonzo").count()).isZero();
        }
    }

    @Test
    public void getCaseInstanceWithNullInvolvedUser() {
        assertThatThrownBy(() -> cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser(null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("involvedUser is null");
    }

    @Test
    public void getCaseInstanceWithNullInvolvedGroups() {
        assertThatThrownBy(() -> cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("groupIds are null");
    }

    @Test
    public void getCaseInstanceWithEmptyInvolvedGroups() {
        assertThatThrownBy(() -> cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.emptySet()))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("groupIds are empty");
    }

    @Test
    public void getCaseInstanceWithNonNullInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThatThrownBy(() -> cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser(null).count())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("involvedUser is null");
    }

    @Test
    public void getCaseInstanceWithInvolvedGroups() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId())
                .isEqualTo(caseInstance.getId());
        }
    }

    @Test
    public void getCaseInstanceWithEmptyGroupId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("")).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("")).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("")).singleResult()).isNull();
        }
    }

    @Test
    public void getCaseInstanceWithNonExistingGroupId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("NonExisting")).count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("NonExisting")).list()).isEmpty();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("NonExisting")).singleResult()).isNull();
        }
    }

    @Test
    public void getCaseInstanceWithTwoInvolvedGroups() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(
                Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())).count())
                .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(
                Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())).list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(
                Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())).singleResult().getId()).isEqualTo(caseInstance.getId());

            // SQL Server has a limit of 2100 on how many parameters a query might have
            int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(cmmnEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

            Set<String> testGroups = new HashSet<>(maxGroups);
            for (int i = 0; i < maxGroups; i++) {
                testGroups.add("group" + i);
            }
            
            HistoricCaseInstanceQuery caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(testGroups);
            assertThat(caseInstanceQuery.count()).isEqualTo(0);
            assertThat(caseInstanceQuery.list()).hasSize(0);
            
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(testGroups).includeCaseVariables();
            assertThat(caseInstanceQuery.count()).isEqualTo(0);
            assertThat(caseInstanceQuery.list()).hasSize(0);
            
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").involvedGroups(testGroups).endOr();
            assertThat(caseInstanceQuery.count()).isEqualTo(2);
            assertThat(caseInstanceQuery.list()).hasSize(2);
            
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").involvedGroups(testGroups).endOr().includeCaseVariables();
            assertThat(caseInstanceQuery.count()).isEqualTo(2);
            assertThat(caseInstanceQuery.list()).hasSize(2);
            
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("unexisting").involvedGroups(testGroups).endOr();
            assertThat(caseInstanceQuery.count()).isEqualTo(0);
            assertThat(caseInstanceQuery.list()).hasSize(0);
            
            testGroups.add("testGroup");
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(testGroups);
            assertThat(caseInstanceQuery.count()).isEqualTo(1);
            assertThat(caseInstanceQuery.list()).hasSize(1);
            
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(testGroups).includeCaseVariables();
            assertThat(caseInstanceQuery.count()).isEqualTo(1);
            assertThat(caseInstanceQuery.list()).hasSize(1);
            
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("unexisting").involvedGroups(testGroups).endOr();
            assertThat(caseInstanceQuery.count()).isEqualTo(1);
            assertThat(caseInstanceQuery.list()).hasSize(1);
            
            testGroups.add("testGroup2");
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(testGroups);
            assertThat(caseInstanceQuery.count()).isEqualTo(1);
            assertThat(caseInstanceQuery.list()).hasSize(1);
            
            caseInstanceQuery = cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("unexisting").involvedGroups(testGroups).endOr();
            assertThat(caseInstanceQuery.count()).isEqualTo(1);
            assertThat(caseInstanceQuery.list()).hasSize(1);
        }
    }

}

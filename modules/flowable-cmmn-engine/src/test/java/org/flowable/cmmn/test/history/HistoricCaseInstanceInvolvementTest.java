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

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author martin.grofcik
 */
public class HistoricCaseInstanceInvolvementTest extends FlowableCmmnTestCase {

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    protected String deploymentId;

    @Before
    public void createCaseInstance() {
        deploymentId = cmmnEngine.getCmmnRepositoryService().createDeployment().addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
            .deploy().getId();
        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
    }

    @After
    public void deleteCaseInstance() {
        cmmnEngine.getCmmnRepositoryService().deleteDeployment(deploymentId, true);
    }

    @Test
    public void getCaseInstanceWithInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceWithTwoInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "gonzo", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceWithTwoInvolvedUserEmptyQuery() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "gonzo", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("").count(), is(0L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("").list(), is(emptyList()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("").singleResult(), is(nullValue()));
    }

    @Test
    public void getCaseInstanceWithNonExistingInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("gonzo").count(), is(0L));
    }

    @Test
    public void getCaseInstanceWithNullInvolvedUser() {
        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedUser is null");

        cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser(null);
    }

    @Test
    public void getCaseInstanceWithNullInvolvedGroups() {
        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedGroups are null");

        cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(null);
    }

    @Test
    public void getCaseInstanceWithEmptyInvolvedGroups() {
        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedGroups are empty");

        cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.emptySet());
    }

    @Test
    public void getCaseInstanceWithNonNullInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedUser is null");

        cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser(null).count();
    }

    @Test
    public void getCaseInstanceWithInvolvedGroups() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceWithEmptyGroupId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("")).count(), is(0L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("")).list(), is(emptyList()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("")).singleResult(), is(nullValue()));
    }

    @Test
    public void getCaseInstanceWithNonExistingGroupId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("NonExisting")).count(), is(0L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("NonExisting")).list(), is(emptyList()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("NonExisting")).singleResult(), is(nullValue()));
    }

    @Test
    public void getCaseInstanceWithTwoInvolvedGroups() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(
            Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())
        ).count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(
            Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())
        ).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(
            Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())
        ).singleResult().getId(), is(caseInstance.getId()));
    }

}

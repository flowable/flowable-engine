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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author martin.grofcik
 */
public class CaseInstanceInvolvementTest extends FlowableCmmnTestCase {

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithTwoInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "gonzo", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithTwoInvolvedUserEmptyQuery() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "gonzo", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("").count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("").list()).isEmpty();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("").singleResult()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithNonExistingInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("gonzo").count()).isZero();
    }

    @Test
    public void getCaseInstanceWithNullInvolvedUser() {
        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedUser is null");

        cmmnRuntimeService.createCaseInstanceQuery().involvedUser(null);
    }

    @Test
    public void getCaseInstanceWithNullInvolvedGroups() {
        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedGroups are null");

        cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(null);
    }

    @Test
    public void getCaseInstanceWithEmptyInvolvedGroups() {
        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedGroups are empty");

        cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.emptySet());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithNonNullInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        this.expectException.expect(FlowableIllegalArgumentException.class);
        this.expectException.expectMessage("involvedUser is null");

        cmmnRuntimeService.createCaseInstanceQuery().involvedUser(null).count();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithInvolvedGroups() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithEmptyGroupId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("")).count()).isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("")).list()).isEmpty();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("")).singleResult()).isNull();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void getCaseInstanceWithTwoInvolvedGroups() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(
                Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())
        ).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(
                Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())
        ).list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(
                Stream.of("testGroup", "testGroup2", "testGroup3").collect(Collectors.toSet())
        ).singleResult().getId()).isEqualTo(caseInstance.getId());

        // SQL Server has a limit of 2100 on how many parameters a query might have
        int maxGroups = AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(cmmnEngineConfiguration.getDatabaseType()) ? 2050 : 2100;

        Set<String> testGroups = new HashSet<>(maxGroups);
        for (int i = 0; i < maxGroups; i++) {
            testGroups.add("group" + i);
        }
        
        CaseInstanceQuery caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(testGroups);
        assertThat(caseInstanceQuery.count()).isEqualTo(0);
        assertThat(caseInstanceQuery.list()).hasSize(0);
        
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(testGroups).includeCaseVariables();
        assertThat(caseInstanceQuery.count()).isEqualTo(0);
        assertThat(caseInstanceQuery.list()).hasSize(0);
        
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").involvedGroups(testGroups).endOr();
        assertThat(caseInstanceQuery.count()).isEqualTo(1);
        assertThat(caseInstanceQuery.list()).hasSize(1);
        
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").involvedGroups(testGroups).endOr().includeCaseVariables();
        assertThat(caseInstanceQuery.count()).isEqualTo(1);
        assertThat(caseInstanceQuery.list()).hasSize(1);
        
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("unexisting").involvedGroups(testGroups).endOr();
        assertThat(caseInstanceQuery.count()).isEqualTo(0);
        assertThat(caseInstanceQuery.list()).hasSize(0);
        
        testGroups.add("testGroup");
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(testGroups);
        assertThat(caseInstanceQuery.count()).isEqualTo(1);
        assertThat(caseInstanceQuery.list()).hasSize(1);
        
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(testGroups).includeCaseVariables();
        assertThat(caseInstanceQuery.count()).isEqualTo(1);
        assertThat(caseInstanceQuery.list()).hasSize(1);
        
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("unexisting").involvedGroups(testGroups).endOr();
        assertThat(caseInstanceQuery.count()).isEqualTo(1);
        assertThat(caseInstanceQuery.list()).hasSize(1);
        
        testGroups.add("testGroup2");
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(testGroups);
        assertThat(caseInstanceQuery.count()).isEqualTo(1);
        assertThat(caseInstanceQuery.list()).hasSize(1);
        
        caseInstanceQuery = cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("unexisting").involvedGroups(testGroups).endOr();
        assertThat(caseInstanceQuery.count()).isEqualTo(1);
        assertThat(caseInstanceQuery.list()).hasSize(1);
    }

}

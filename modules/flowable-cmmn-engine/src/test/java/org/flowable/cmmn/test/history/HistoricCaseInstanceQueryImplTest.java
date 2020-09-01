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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLinkType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class tests {@link CaseInstanceQueryImpl} implementation
 */
public class HistoricCaseInstanceQueryImplTest extends FlowableCmmnTestCase {

    private String deplId;

    @Before
    public void createCase() {
        deplId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .deploy()
                .getId();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
    }

    @After
    public void deleteCase() {
        cmmnRepositoryService.deleteDeployment(deplId, true);
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").list().get(0).getId()).isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByCaseInstanceName() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("taskName1")
                .start();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("taskName2")
                .start();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("nameTask3")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceNameLikeIgnoreCase("taskName%").count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceNameLikeIgnoreCase("%TASK3").count()).isEqualTo(1);
    }

    public void getCaseInstanceByCaseDefinitionKeyIncludingVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").includeCaseVariables().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").includeCaseVariables().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").includeCaseVariables().singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .includeCaseVariables().count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .includeCaseVariables().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionKey("oneTaskCase")
                .caseInstanceId("Undefined")
                .endOr()
                .includeCaseVariables().singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId("undefined")
                .caseDefinitionKeys(Collections.singleton("oneTaskCase"))
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByCaseDefinitionCategory() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").list()).hasSize(2);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionCategory("http://flowable.org/cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .count())
                .isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionCategory("http://flowable.org/cmmn")
                .caseInstanceId("undefined")
                .endOr()
                .list())
                .hasSize(2);
    }

    @Test
    public void getCaseInstanceByCaseDefinitionName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionName("oneTaskCaseName")
                .caseInstanceId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByCaseDefinitionId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr().list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionId(caseInstance.getCaseDefinitionId())
                .caseInstanceId("undefinedId")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByCaseDefinitionVersion() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionVersion(1).count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionVersion(1).list()).hasSize(2);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionVersion(1)
                .caseInstanceId("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseDefinitionVersion(1)
                .caseInstanceId("undefinedId")
                .endOr()
                .list())
                .hasSize(2);
    }

    @Test
    public void getCaseInstanceByCaseId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceId(caseInstance.getId())
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("businessKey")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceBusinessKey("businessKey")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByStartedBefore() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Calendar todayCal = new GregorianCalendar();
        Calendar dateCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) + 1, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_YEAR));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBefore(dateCal.getTime()).count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBefore(dateCal.getTime()).list()).hasSize(2);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedBefore(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedBefore(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(2);
    }

    @Test
    public void getCaseInstanceByStartedAfter() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        Calendar todayCal = new GregorianCalendar();
        Calendar dateCal = new GregorianCalendar(todayCal.get(Calendar.YEAR) - 1, todayCal.get(Calendar.MONTH), todayCal.get(Calendar.DAY_OF_YEAR));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedAfter(dateCal.getTime()).count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedAfter(dateCal.getTime()).list()).hasSize(2);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedAfter(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .startedAfter(dateCal.getTime())
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(2);
    }

    @Test
    public void getCaseInstanceByStartedBy() {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("kermit");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .start();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").list().get(0).getId()).isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").singleResult().getId()).isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .startedBy("kermit")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .startedBy("kermit")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .startedBy("kermit")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(caseInstance.getId());
        } finally {
            Authentication.setAuthenticatedUserId(authenticatedUserId);
        }
    }

    @Test
    public void getCaseInstanceByCallBackId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackId("callBackId")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackId("callBackId")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByCallBackType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .callbackType("callBackType")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceCallbackType("callBackType")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByReferenceId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .referenceId("testReferenceId")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionName("undefined")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionName("undefined")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceId("testReferenceId")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByReferenceType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .referenceType("testReferenceType")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceType("testReferenceType").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .caseInstanceReferenceType("testReferenceType")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }

    @Test
    public void getCaseInstanceByReferenceIdAndType() {
        for (int i = 0; i < 5; i++) {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .referenceId("testReferenceId")
                    .referenceType("testReferenceType")
                    .start();
        }

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceReferenceId("testReferenceId")
                .caseInstanceReferenceType("testReferenceType").count()).isEqualTo(5);
    }

    @Test
    public void getCaseInstanceByTenantId() {
        String tempDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .tenantId("tenantId")
                .deploy()
                .getId();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .tenantId("tenantId")
                    .start();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").singleResult().getId())
                    .isEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceTenantId("tenantId")
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(caseInstance.getId());
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }

    @Test
    public void getCaseInstanceWithoutTenantId() {
        String tempDeploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn")
                .addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
                .tenantId("tenantId")
                .deploy()
                .getId();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .tenantId("tenantId")
                    .start();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().list().get(0).getId())
                    .isNotEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().singleResult().getId())
                    .isNotEqualTo(caseInstance.getId());

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isNotEqualTo(caseInstance.getId());
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .or()
                    .caseInstanceWithoutTenantId()
                    .caseDefinitionId("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isNotEqualTo(caseInstance.getId());
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }

    @Test
    public void getCaseInstanceByInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit")
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByInvolvedUserIdentityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", "specialLink");

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "specialLink").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "specialLink").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "specialLink").singleResult().getId())
                .isEqualTo(caseInstance.getId());
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit", "wrongType").count()).isZero();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "specialLink")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "specialLink")
                .caseDefinitionKey("oneTaskCase")
                .endOr()
                .count())
                .isEqualTo(1);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedUser("kermit", "wrongType")
                .caseDefinitionKey("wrongKey")
                .endOr()
                .count())
                .isZero();
    }

    @Test
    public void getCaseInstanceByInvolvedGroup() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionName("undefinedId")
                .endOr()
                .list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Collections.singleton("testGroup"))
                .caseDefinitionId("undefined")
                .endOr()
                .singleResult().getId())
                .isEqualTo(caseInstance.getId());
    }
    
    @Test
    public void getCaseInstanceByInvolvedGroupIdentityLink() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", "specialLink");
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", "extraLink");

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup", "specialLink").count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup", "specialLink").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup", "specialLink").singleResult().getId())
                .isEqualTo(caseInstance.getId());
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroup("testGroup2", "wrongType").count()).isZero();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup", "specialLink")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(1);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup2", "extraLink")
                .caseDefinitionKey("oneTaskCase")
                .endOr()
                .count())
                .isEqualTo(1);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroup("testGroup2", "wrongType")
                .caseDefinitionKey("wrongKey")
                .endOr()
                .count())
                .isZero();
    }

    @Test
    public void getCaseInstanceByInvolvedGroupOrUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.CANDIDATE);
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance2.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance3.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").list().get(0).getId())
                .isEqualTo(caseInstance.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").singleResult().getId())
                .isEqualTo(caseInstance.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(2);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(3);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(3);
    }

    @Test
    public void getCaseInstanceByVariable() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("queryVariable", "queryVariableValue")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("queryVariable", "queryVariableValue")
                .variable("queryVariable2", "queryVariableValue2")
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("queryVariable", "queryVariableValue")
                .variable("queryVariable3", "queryVariableValue3")
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .count())
                .isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .list().get(0).getId())
                .isEqualTo(caseInstance2.getId());
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .singleResult().getId())
                .isEqualTo(caseInstance2.getId());

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .caseDefinitionName("undefinedId")
                .endOr()
                .count())
                .isEqualTo(3);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery()
                .or()
                .variableValueEquals("queryVariable", "queryVariableValue")
                .variableValueEquals("queryVariable2", "queryVariableValue2")
                .caseDefinitionName("undefinedId")
                .endOr()
                .list())
                .hasSize(3);
    }

    @Test
    public void getCaseInstanceByIdWithoutTenant() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("testCaseInstance1")
                .start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("testCaseInstance2")
                .start();

        try {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).caseInstanceWithoutTenantId().count())
                    .isEqualTo(1);
        } finally {
            cmmnRuntimeService.terminateCaseInstance(caseInstance2.getId());
            cmmnHistoryService.deleteHistoricCaseInstance(caseInstance2.getId());
            cmmnRuntimeService.terminateCaseInstance(caseInstance1.getId());
            cmmnHistoryService.deleteHistoricCaseInstance(caseInstance1.getId());
        }

    }

    @Test
    public void testQueryCaseInstanceReturnsCaseDefinitionInformation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("stringVar", "test")
                .start();

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();

        assertThat(historicCaseInstance).isNotNull();
        assertThat(historicCaseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");
        assertThat(historicCaseInstance.getCaseDefinitionName()).isEqualTo("oneTaskCaseName");
        assertThat(historicCaseInstance.getCaseDefinitionVersion()).isEqualTo(1);
        assertThat(historicCaseInstance.getCaseDefinitionDeploymentId()).isEqualTo(deplId);
        assertThat(historicCaseInstance.getCaseVariables()).isEmpty();
    }

    @Test
    public void testQueryCaseInstanceWithVariablesReturnsCaseDefinitionInformation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("stringVar", "test")
                .start();

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();

        assertThat(historicCaseInstance).isNotNull();
        assertThat(historicCaseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");
        assertThat(historicCaseInstance.getCaseDefinitionName()).isEqualTo("oneTaskCaseName");
        assertThat(historicCaseInstance.getCaseDefinitionVersion()).isEqualTo(1);
        assertThat(historicCaseInstance.getCaseDefinitionDeploymentId()).isEqualTo(deplId);
        assertThat(historicCaseInstance.getCaseVariables()).containsOnly(
                entry("stringVar","test")
        );
    }

    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {
        CaseInstance caseWithStringValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        CaseInstance caseWithNullValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With null value")
                .variable("var", null)
                .start();

        CaseInstance caseWithLongValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With long value")
                .variable("var", 100L)
                .start();

        CaseInstance caseWithDoubleValue = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "TEST").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", caseWithNullValue.getId()),
                        tuple("With long value", caseWithLongValue.getId()),
                        tuple("With double value", caseWithDoubleValue.getId())
                );

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "TEST").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseWithStringValue.getId())
                );

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", 100L).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseWithStringValue.getId()),
                        tuple("With null value", caseWithNullValue.getId()),
                        tuple("With double value", caseWithDoubleValue.getId())
                );

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", 100L).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With long value", caseWithLongValue.getId())
                );

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", 45.55).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseWithStringValue.getId()),
                        tuple("With null value", caseWithNullValue.getId()),
                        tuple("With long value", caseWithLongValue.getId())
                );

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", 45.55).list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With double value", caseWithDoubleValue.getId())
                );

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseWithStringValue.getId()),
                        tuple("With null value", caseWithNullValue.getId()),
                        tuple("With long value", caseWithLongValue.getId()),
                        tuple("With double value", caseWithDoubleValue.getId())
                );

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .isEmpty();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "test").list())
                .extracting(HistoricCaseInstance::getName, HistoricCaseInstance::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", caseWithStringValue.getId())
                );
    }

}

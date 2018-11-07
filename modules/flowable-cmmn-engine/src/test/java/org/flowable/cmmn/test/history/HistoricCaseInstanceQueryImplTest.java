package org.flowable.cmmn.test.history;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        deplId = cmmnRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn").
            addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn").
            deploy()
            .getId();
        cmmnEngineConfiguration.getClock().setCurrentTime(new Date(0));
        try {
            cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("myCase").
                start();
        } finally {
            cmmnEngineConfiguration.getClock().reset();
        }
    }

    @After
    public void deleteCase() {
        cmmnRepositoryService.deleteDeployment(deplId, true);
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("oneTaskCase").singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().
            caseInstanceId("undefined").
            caseDefinitionKeys(Collections.singleton("oneTaskCase")).
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().
            caseInstanceId("undefined").
            caseDefinitionKeys(Collections.singleton("oneTaskCase"))
            .endOr()
            .list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().or().
            caseInstanceId("undefined").
            caseDefinitionKeys(Collections.singleton("oneTaskCase")).
            endOr().singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionCategory() {
        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").count(), is(2L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").list().size(), is(2));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionCategory("http://flowable.org/cmmn").
                caseInstanceId("undefined").
            endOr().
            count(), is(2L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionCategory("http://flowable.org/cmmn").
                caseInstanceId("undefined").
            endOr().
            list().size(), is(2));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionName("oneTaskCaseName").
                caseInstanceId("undefined").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionName("oneTaskCaseName").
                caseInstanceId("undefined").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionName("oneTaskCaseName").
                caseInstanceId("undefined").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionId(caseInstance.getCaseDefinitionId()).
                caseInstanceId("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionId(caseInstance.getCaseDefinitionId()).
                caseInstanceId("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionId(caseInstance.getCaseDefinitionId()).
                caseInstanceId("undefinedId").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionVersion() {
        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionVersion(1).count(), is(2L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionVersion(1).list().size(), is(2));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionVersion(1).
                caseInstanceId("undefinedId").
            endOr().
            count(), is(2L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseDefinitionVersion(1).
                caseInstanceId("undefinedId").
            endOr().
            list().size(), is(2));
    }

    @Test
    public void getCaseInstanceByCaseId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseInstanceId(caseInstance.getId()).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseInstanceId(caseInstance.getId()).
                caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseInstanceId(caseInstance.getId()).
                caseDefinitionId("undefined").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByBusinessKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            businessKey("businessKey").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey("businessKey").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseInstanceBusinessKey("businessKey").
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseInstanceBusinessKey("businessKey").
                caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                caseInstanceBusinessKey("businessKey").
                caseDefinitionId("undefined").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByStartedBefore() {
        cmmnEngineConfiguration.getClock().setCurrentTime(new Date(0));
        try {
            cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCase").
                start();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBefore(new Date(100)).count(), is(2L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBefore(new Date(100)).list().size(),
                is(2));

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                startedBefore(new Date(100)).
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(2L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                startedBefore(new Date(100)).
                caseDefinitionName("undefinedId").
                endOr().
                list().size(), is(2));
        } finally {
            cmmnEngineConfiguration.getClock().reset();
        }
    }

    @Test
    public void getCaseInstanceByStartedAfter() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedAfter(new Date(0)).count(), is(2L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedAfter(new Date(0)).list().size(),
            is(2));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            startedAfter(new Date(0)).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(2L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                startedAfter(new Date(0)).
                caseDefinitionName("undefinedId").
            endOr().
            list().size(), is(2));
    }

    @Test
    public void getCaseInstanceByStartedBy() {
        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("kermit");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCase").
                start();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").count(), is(1L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").list().get(0).getId(),
                is(caseInstance.getId()));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().startedBy("kermit").singleResult().getId(),
                is(caseInstance.getId()));

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                startedBy("kermit").
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(1L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                startedBy("kermit").
                caseDefinitionName("undefinedId").
                endOr().
                list().get(0).getId(), is(caseInstance.getId()));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                startedBy("kermit").
                caseDefinitionId("undefined").
                endOr().
                singleResult().getId(), is(caseInstance.getId()));
        } finally {
            Authentication.setAuthenticatedUserId(authenticatedUserId);
        }
    }

    @Test
    public void getCaseInstanceByCallBackId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            callbackId("callBackId").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackId("callBackId").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            caseInstanceCallbackId("callBackId").
            caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            caseInstanceCallbackId("callBackId").
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            caseInstanceCallbackId("callBackId").
            caseDefinitionId("undefined").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCallBackType() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            callbackType("callBackType").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceCallbackType("callBackType").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            caseInstanceCallbackType("callBackType").
            caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            caseInstanceCallbackType("callBackType").
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            caseInstanceCallbackType("callBackType").
            caseDefinitionId("undefined").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByTenantId() {
        String tempDeploymentId = cmmnRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn").
            addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn").
            tenantId("tenantId").
            deploy()
            .getId();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCase").
                tenantId("tenantId").
                start();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").count(), is(1L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").list().get(0).getId(),
                is(caseInstance.getId()));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceTenantId("tenantId").singleResult().getId(),
                is(caseInstance.getId()));

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                caseInstanceTenantId("tenantId").
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(1L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                caseInstanceTenantId("tenantId").
                caseDefinitionName("undefinedId").
                endOr().
                list().get(0).getId(), is(caseInstance.getId()));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                caseInstanceTenantId("tenantId").
                caseDefinitionId("undefined").
                endOr().
                singleResult().getId(), is(caseInstance.getId()));
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }

    @Test
    public void getCaseInstanceWithoutTenantId() {
        String tempDeploymentId = cmmnRepositoryService.createDeployment().
            addClasspathResource("org/flowable/cmmn/test/runtime/CaseTaskTest.testBasicBlocking.cmmn").
            addClasspathResource("org/flowable/cmmn/test/runtime/oneTaskCase.cmmn").
            tenantId("tenantId").
            deploy()
            .getId();
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
                caseDefinitionKey("oneTaskCase").
                tenantId("tenantId").
                start();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().count(), is(1L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().list().get(0).getId(),
                is(not(caseInstance.getId())));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceWithoutTenantId().singleResult().getId(),
                is(not(caseInstance.getId())));

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                caseInstanceWithoutTenantId().
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(1L));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                caseInstanceWithoutTenantId().
                caseDefinitionName("undefinedId").
                endOr().
                list().get(0).getId(), is(not(caseInstance.getId())));
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                or().
                caseInstanceWithoutTenantId().
                caseDefinitionId("undefined").
                endOr().
                singleResult().getId(), is(not(caseInstance.getId())));
        } finally {
            cmmnRepositoryService.deleteDeployment(tempDeploymentId, true);
        }
    }

    @Test
    public void getCaseInstanceByInvolvedUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedUser("kermit").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                involvedUser("kermit").
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            involvedUser("kermit").
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            involvedUser("kermit").
            caseDefinitionId("undefined").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByInvolvedGroup() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                involvedGroups(Collections.singleton("testGroup")).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            involvedGroups(Collections.singleton("testGroup")).
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            involvedGroups(Collections.singleton("testGroup")).
            caseDefinitionId("undefined").
            endOr().
            singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByInvolvedGroupOrUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "kermit", IdentityLinkType.CANDIDATE);
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addGroupIdentityLink(caseInstance2.getId(), "testGroup2", IdentityLinkType.PARTICIPANT);
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance3.getId(), "kermit", IdentityLinkType.PARTICIPANT);

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).involvedUser("kermit").count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(2L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).
            caseDefinitionName("undefinedId").
            endOr().
            list().size(), is(2));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
                involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).
                involvedUser("kermit").
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(3L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).
            involvedUser("kermit").
            caseDefinitionName("undefinedId").
            endOr().
            list().size(), is(3));
    }

    @Test
    public void getCaseInstanceByVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            variable("queryVariable", "queryVariableValue").
            start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            variable("queryVariable", "queryVariableValue").
            variable("queryVariable2", "queryVariableValue2").
            start();
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            variable("queryVariable", "queryVariableValue").
            variable("queryVariable3", "queryVariableValue3").
            start();

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2","queryVariableValue2").
            count(), is(1L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2", "queryVariableValue2")
            .list().get(0).getId(),
            is(caseInstance2.getId()));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
                variableValueEquals("queryVariable", "queryVariableValue").
                variableValueEquals("queryVariable2", "queryVariableValue2").
                singleResult().getId(),
            is(caseInstance2.getId()));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2", "queryVariableValue2").
            caseDefinitionName("undefinedId").
            endOr().
            count(), is(3L));
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().
            or().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2", "queryVariableValue2").
            caseDefinitionName("undefinedId").
            endOr().
            list().size(), is(3));
    }

    @Test
    public void getCaseInstanceByIdWithoutTenant() {
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            name("testCaseInstance1").
            start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            name("testCaseInstance2").
            start();

        try {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance1.getId()).caseInstanceWithoutTenantId().count(),
                is(1L));
        } finally {
            cmmnRuntimeService.terminateCaseInstance(caseInstance2.getId());
            cmmnHistoryService.deleteHistoricCaseInstance(caseInstance2.getId());
            cmmnRuntimeService.terminateCaseInstance(caseInstance1.getId());
            cmmnHistoryService.deleteHistoricCaseInstance(caseInstance1.getId());
        }


    }


}
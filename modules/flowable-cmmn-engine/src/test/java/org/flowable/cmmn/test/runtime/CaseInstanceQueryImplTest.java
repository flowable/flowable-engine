package org.flowable.cmmn.test.runtime;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
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
public class CaseInstanceQueryImplTest extends FlowableCmmnTestCase {

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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneTaskCase").singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().caseDefinitionKey("oneTaskCase").caseInstanceId("Undefined").endOr().singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().
            caseInstanceId("undefined").
            caseDefinitionKeys(Collections.singleton("oneTaskCase")).
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().
            caseInstanceId("undefined").
            caseDefinitionKeys(Collections.singleton("oneTaskCase"))
            .endOr()
            .list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().or().
            caseInstanceId("undefined").
            caseDefinitionKeys(Collections.singleton("oneTaskCase")).
            endOr().singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionCategory() {
        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").list().size(), is(2));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseDefinitionCategory("http://flowable.org/cmmn").
                caseInstanceId("undefined").
            endOr().
            count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseDefinitionName("oneTaskCaseName").
                caseInstanceId("undefined").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseDefinitionName("oneTaskCaseName").
                caseInstanceId("undefined").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).singleResult().getId(), is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseDefinitionId(caseInstance.getCaseDefinitionId()).
                caseInstanceId("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseDefinitionId(caseInstance.getCaseDefinitionId()).
                caseInstanceId("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionVersion(1).count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionVersion(1).list().size(), is(2));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseDefinitionVersion(1).
                caseInstanceId("undefinedId").
            endOr().
            count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseInstanceId(caseInstance.getId()).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseInstanceId(caseInstance.getId()).
                caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey("businessKey").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey("businessKey").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey("businessKey").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseInstanceBusinessKey("businessKey").
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseInstanceBusinessKey("businessKey").
                caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBefore(new Date(100)).count(), is(2L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBefore(new Date(100)).list().size(),
                is(2));

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceStartedBefore(new Date(100)).
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(2L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceStartedBefore(new Date(100)).
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedAfter(new Date(0)).count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedAfter(new Date(0)).list().size(),
            is(2));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseInstanceStartedAfter(new Date(0)).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                caseInstanceStartedAfter(new Date(0)).
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBy("kermit").count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBy("kermit").list().get(0).getId(),
                is(caseInstance.getId()));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceStartedBy("kermit").singleResult().getId(),
                is(caseInstance.getId()));

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceStartedBy("kermit").
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceStartedBy("kermit").
                caseDefinitionName("undefinedId").
                endOr().
                list().get(0).getId(), is(caseInstance.getId()));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceStartedBy("kermit").
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId("callBackId").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId("callBackId").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackId("callBackId").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            caseInstanceCallbackId("callBackId").
            caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            caseInstanceCallbackId("callBackId").
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackType("callBackType").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackType("callBackType").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceCallbackType("callBackType").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            caseInstanceCallbackType("callBackType").
            caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            caseInstanceCallbackType("callBackType").
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantId").count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantId").list().get(0).getId(),
                is(caseInstance.getId()));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantId("tenantId").singleResult().getId(),
                is(caseInstance.getId()));

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceTenantId("tenantId").
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceTenantId("tenantId").
                caseDefinitionName("undefinedId").
                endOr().
                list().get(0).getId(), is(caseInstance.getId()));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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
    public void getCaseInstanceByTenantIdLike() {
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLike("ten%").count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLike("ten%").list().get(0).getId(),
                is(caseInstance.getId()));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceTenantIdLike("ten%").singleResult().getId(),
                is(caseInstance.getId()));

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceTenantIdLike("ten%").
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceTenantIdLike("ten%").
                caseDefinitionName("undefinedId").
                endOr().
                list().get(0).getId(), is(caseInstance.getId()));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceTenantIdLike("ten%").
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

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceWithoutTenantId().count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceWithoutTenantId().list().get(0).getId(),
                is(not(caseInstance.getId())));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceWithoutTenantId().singleResult().getId(),
                is(not(caseInstance.getId())));

            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceWithoutTenantId().
                caseDefinitionName("undefinedId").
                endOr().
                count(), is(1L));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                or().
                caseInstanceWithoutTenantId().
                caseDefinitionName("undefinedId").
                endOr().
                list().get(0).getId(), is(not(caseInstance.getId())));
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedUser("kermit").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                involvedUser("kermit").
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            involvedUser("kermit").
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Collections.singleton("testGroup")).singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                involvedGroups(Collections.singleton("testGroup")).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            involvedGroups(Collections.singleton("testGroup")).
            caseDefinitionName("undefinedId").
            endOr().
            list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).involvedUser("kermit").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").list().get(0).getId(),
            is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet()))
                .involvedUser("kermit").singleResult().getId(),
            is(caseInstance.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).
            caseDefinitionName("undefinedId").
            endOr().
            list().size(), is(2));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
                involvedGroups(Stream.of("testGroup", "testGroup2").collect(Collectors.toSet())).
                involvedUser("kermit").
                caseDefinitionName("undefinedId").
            endOr().
            count(), is(3L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
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

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2","queryVariableValue2").
            count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2", "queryVariableValue2")
            .list().get(0).getId(),
            is(caseInstance2.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
                variableValueEquals("queryVariable", "queryVariableValue").
                variableValueEquals("queryVariable2", "queryVariableValue2").
                singleResult().getId(),
            is(caseInstance2.getId()));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2", "queryVariableValue2").
            caseDefinitionName("undefinedId").
            endOr().
            count(), is(3L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().
            or().
            variableValueEquals("queryVariable", "queryVariableValue").
            variableValueEquals("queryVariable2", "queryVariableValue2").
            caseDefinitionName("undefinedId").
            endOr().
            list().size(), is(3));
    }

    @Test
    public void getCaseInstanceByIdWithoutTenant() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            name("caseInstance1").
            start();
        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            name("caseInstance2").
            start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).caseInstanceWithoutTenantId().count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).caseInstanceWithoutTenantId().list().size(), is(1));
    }

}
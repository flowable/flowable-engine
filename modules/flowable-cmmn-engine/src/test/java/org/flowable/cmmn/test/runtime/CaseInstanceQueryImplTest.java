package org.flowable.cmmn.test.runtime;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceQueryImpl;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
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
        cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("myCase").
            start();
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
    }

    @Test
    public void getCaseInstanceByCaseDefinitionKeys() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKeys(Collections.singleton("oneTaskCase")).singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionCategory() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").count(), is(2L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionCategory("http://flowable.org/cmmn").list().size(), is(2));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionName("oneTaskCaseName").singleResult().getId(), is(caseInstance.getId()));
    }

    @Test
    public void getCaseInstanceByCaseDefinitionId() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).count(), is(1L));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).list().get(0).getId(), is(caseInstance.getId()));
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionId(caseInstance.getCaseDefinitionId()).singleResult().getId(), is(caseInstance.getId()));
    }
}
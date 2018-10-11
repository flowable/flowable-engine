package org.flowable.cmmn.test.runtime;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * This class tests {@link CmmnRuntimeServiceImpl} implementation
 */
public class CmmnRuntimeServiceTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithCallBacks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            callbackId("testCallBackId").
            callbackType(CallbackTypes.CASE_ADHOC_CHILD).
            start();

        // in fact it must be possible to set any callbackType and Id
        assertThat(caseInstance.getCallbackType(), is(CallbackTypes.CASE_ADHOC_CHILD));
        assertThat(caseInstance.getCallbackId(), is("testCallBackId"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void createCaseInstanceWithoutCallBacks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default values for callbacks are null
        assertThat(caseInstance.getCallbackType(), nullValue());
        assertThat(caseInstance.getCallbackId(), nullValue());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateCaseName() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default name is empty
        assertThat(caseInstance.getName(), nullValue());

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), "My case name");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(caseInstance.getName(), "My case name");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void updateCaseNameSetEmpty() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default name is empty
        assertThat(caseInstance.getName(), nullValue());

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), "My case name");

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals(caseInstance.getName(), "My case name");

        cmmnRuntimeService.setCaseInstanceName(caseInstance.getId(), null);

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(caseInstance.getName(), nullValue());
    }
}
package org.flowable.cmmn.test.runtime;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * This class tests {@link CmmnRuntimeServiceImpl} implementation
 */
public class CmmnRuntimeServiceImplTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void crateCaseInstanceWithCallBacks() {
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
    public void crateCaseInstanceWithoutCallBacks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().
            caseDefinitionKey("oneTaskCase").
            start();

        // default values for callbacks are null
        assertThat(caseInstance.getCallbackType(), nullValue());
        assertThat(caseInstance.getCallbackId(), nullValue());
    }

}
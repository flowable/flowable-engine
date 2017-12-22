package org.flowable.cmmn.test.runtime;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author martin.grofcik
 */
public class StartCaseWithFormTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testJavaServiceTask.cmmn")
    public void testStartCaseWithFormWithoutFormKeyInModel() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .variable("test", "test2")
                .startWithForm();
        assertNotNull(caseInstance);

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/StartCaseWithFormTest.testStartCaseWithForm.cmmn")
    public void testStartCaseWithForm_withoutFormRepositoryService() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .variable("test", "test2")
                .startWithForm();
        assertNotNull("It must be possible to start case without form repository service", caseInstance);
    }
}

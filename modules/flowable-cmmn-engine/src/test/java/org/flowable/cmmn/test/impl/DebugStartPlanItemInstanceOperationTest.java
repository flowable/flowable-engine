package org.flowable.cmmn.test.impl;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.DebugCmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.job.ActivateCmmnBreakpointJobHandler;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author martin.grofcik
 */
public class DebugStartPlanItemInstanceOperationTest extends CustomCmmnConfigurationFlowableTestCase {

    private TestCmmnDebugger debugger;
    @Override
    protected String getEngineName() {
        return "DebugCmmnEngineTest";
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        debugger = new TestCmmnDebugger();
        cmmnEngineConfiguration.setCmmnEngineAgendaFactory(new DebugCmmnEngineAgendaFactory(debugger));
        cmmnEngineConfiguration.addCustomJobHandler(new ActivateCmmnBreakpointJobHandler());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void disabledDebuggerHappyPath() {
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertNotNull(task);
        cmmnTaskService.complete(task.getId());
        assertEquals(0l, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(oneTaskCase.getId()).count());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void enableDebuggerHappyPath() {
        debugger.setBreakPointPredicate((entryCriterionId, planItemInstance) -> true);
        CaseInstance oneTaskCase = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        assertEquals(0l, cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).count());

        Job job = cmmnManagementService.createSuspendedJobQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        cmmnManagementService.moveSuspendedJobToExecutableJob(job.getId());
        waitForJobExecutorToProcessAllJobs();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(oneTaskCase.getId()).singleResult();
        assertNotNull(task);

        cmmnTaskService.complete(task.getId());
        assertEquals(0l, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(oneTaskCase.getId()).count());
        debugger.setBreakPointPredicate((entryCriterionId, planItemInstance) -> false);
    }

}
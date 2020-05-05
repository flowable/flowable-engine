package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Testing a milestone triggering an exit sentry on the case level with completion semantics.
 *
 * @author Micha Kiener
 */
public class CompleteWithExitSentryByMilestoneTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/CompleteWithExitSentryByMilestoneTest.testExitSentryCompletion.cmmn")
    public void testExitSentryCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("exitSentryTriggeredByMilestoneTestCase")
            .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());

        assertThat(planItemInstances).hasSize(3);
        assertPlanItemInstanceState(planItemInstances, "Task", ACTIVE);
        cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task"));

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(0);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
    }
}

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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceCriterionTest extends FlowableCmmnTestCase {

    protected CaseInstance caseInstance;

    @Before
    public void deployTestCaseInstance() {
        addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/runtime/PlanItemInstanceCriterionTest.testCriterionSaved.cmmn")
            .deploy());
        this.caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("criterions").start();
    }

    @Test
    public void testCriterionSaved1() {

        // Completing the user event listener will exit both A and the stage

        PlanItemInstance a = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("A").singleResult();
        assertThat(a.getEntryCriterionId()).isNull();
        assertThat(a.getExitCriterionId()).isNull();
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("The Stage").singleResult();
        assertThat(stage.getEntryCriterionId()).isNull();
        assertThat(stage.getExitCriterionId()).isNull();

        UserEventListenerInstance stopUserEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().name("Stop").singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(stopUserEventListenerInstance.getId());

        a = cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceName("A").singleResult();
        assertThat(a.getEntryCriterionId()).isNull();
        assertThat(a.getExitCriterionId()).isEqualTo("exitA");

        stage = cmmnRuntimeService.createPlanItemInstanceQuery().includeEnded().planItemInstanceName("The Stage").singleResult();
        assertThat(stage.getEntryCriterionId()).isNull();
        assertThat(stage.getExitCriterionId()).isEqualTo("exitStage");
    }

    @Test
    public void testCriterionSaved2() {

        // Completing B will start E

        PlanItemInstance e = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("E").singleResult();
        assertThat(e.getEntryCriterionId()).isNull();
        assertThat(e.getExitCriterionId()).isNull();

        Task b = cmmnTaskService.createTaskQuery().taskName("B").singleResult();
        cmmnTaskService.complete(b.getId());

        e = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("E").includeEnded().singleResult();
        assertThat(e.getEntryCriterionId()).isEqualTo("entryE1");
        assertThat(e.getExitCriterionId()).isNull();
    }

    @Test
    public void testCriterionSaved3() {

        // Setting the variable will start E via another sentry

        PlanItemInstance e = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("E").singleResult();
        assertThat(e.getEntryCriterionId()).isNull();
        assertThat(e.getExitCriterionId()).isNull();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "myVar", true);

        e = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("E").includeEnded().singleResult();
        assertThat(e.getEntryCriterionId()).isEqualTo("entryE2");
        assertThat(e.getExitCriterionId()).isNull();
    }

    @Test
    public void testCriterionSaved4() {

        // Testing nested plan item instances

        PlanItemInstance d = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("D").singleResult();
        assertThat(d.getEntryCriterionId()).isNull();
        assertThat(d.getExitCriterionId()).isNull();

        Task c = cmmnTaskService.createTaskQuery().taskName("C").singleResult();
        cmmnTaskService.complete(c.getId());

        d = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("D").singleResult();
        assertThat(d.getEntryCriterionId()).isEqualTo("sid-8D2B6513-C2B7-48FA-993C-F1B7988E9DA5");
        assertThat(d.getExitCriterionId()).isNull();

        UserEventListenerInstance stopUserEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().name("Stop nested 2").singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(stopUserEventListenerInstance.getId());

        d = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("D").includeEnded().singleResult();
        assertThat(d.getEntryCriterionId()).isEqualTo("sid-8D2B6513-C2B7-48FA-993C-F1B7988E9DA5");
        assertThat(d.getExitCriterionId()).isEqualTo("stopD2");
    }

}

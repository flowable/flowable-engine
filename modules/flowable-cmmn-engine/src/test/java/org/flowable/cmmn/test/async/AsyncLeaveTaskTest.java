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
package org.flowable.cmmn.test.async;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.engine.impl.job.AsyncLeaveActivePlanItemInstanceJobHandler;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class AsyncLeaveTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testAsyncLeaveServiceTask() {
        String caseInstanceId = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start()
                .getId();

        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "serviceTaskVar")).isEqualTo("Hello World");
        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstanceId).singleResult();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncLeaveActivePlanItemInstanceJobHandler.TYPE);
        assertThat(job.isExclusive()).isTrue();
        cmmnManagementService.executeJob(job.getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isOne();
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
    }
    
    @Test
    @CmmnDeployment
    public void testAsyncLeaveNonExclusiveServiceTask() {
        String caseInstanceId = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start()
                .getId();

        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "serviceTaskVar")).isEqualTo("Hello World");
        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();

        Job job = cmmnManagementService.createJobQuery().caseInstanceId(caseInstanceId).singleResult();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncLeaveActivePlanItemInstanceJobHandler.TYPE);
        assertThat(job.isExclusive()).isFalse();
        cmmnManagementService.executeJob(job.getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isOne();
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testAsyncLeavePlanItemLifecycleListeners() {
        String caseInstanceId = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start()
                .getId();

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "any")).isEqualTo(true);
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "active")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "completed")).isNull();

        // Human task is async
        cmmnManagementService.executeJob(cmmnManagementService.createJobQuery().caseInstanceId(caseInstanceId).singleResult().getId());

        assertThat(cmmnTaskService.createTaskQuery().singleResult().getName()).isEqualTo("A");
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "any")).isEqualTo(true);
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "active")).isEqualTo(true);
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "completed")).isNull();

        // Leave async
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).singleResult().getId());

        assertThat(cmmnTaskService.createTaskQuery().count()).isZero();
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "any")).isEqualTo(true);
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "active")).isEqualTo(true);
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "completed")).isNull();

        cmmnManagementService.executeJob(cmmnManagementService.createJobQuery().caseInstanceId(caseInstanceId).singleResult().getId());
        assertThat(cmmnTaskService.createTaskQuery().singleResult().getName()).isEqualTo("Task after");
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "any")).isEqualTo(true);
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "active")).isEqualTo(true);
        assertThat(cmmnRuntimeService.getVariable(caseInstanceId, "completed")).isEqualTo(true);

    }

    @Test
    @CmmnDeployment
    public void testAsyncLeaveWithRepetition() {
        String caseInstanceId = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start()
                .getId();

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).list())
                .extracting(Task::getName)
                .containsOnly("A");

        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).singleResult().getId());
        assertThat(cmmnManagementService.createJobQuery().count()).isOne();
        cmmnManagementService.executeJob(cmmnManagementService.createJobQuery().caseInstanceId(caseInstanceId).singleResult().getId());

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).list())
                .extracting(Task::getName)
                .containsOnly("A", "B");
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).count()).isEqualTo(2);

        for (int i = 0; i < 4; i++) {
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).taskName("A").count()).isEqualTo(1);
            assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).taskName("B").count()).isEqualTo(1 + i);

            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId).taskName("A").singleResult().getId());
            assertThat(cmmnManagementService.createJobQuery().count()).isOne();
            cmmnManagementService.executeJob(cmmnManagementService.createJobQuery().caseInstanceId(caseInstanceId).singleResult().getId());
        }

    }

}

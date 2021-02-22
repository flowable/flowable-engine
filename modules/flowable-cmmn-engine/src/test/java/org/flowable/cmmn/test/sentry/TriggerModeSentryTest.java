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
package org.flowable.cmmn.test.sentry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class TriggerModeSentryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testTriggerModeOnEvent() {

        // This test verifies the behavior of the onEvent triggerMode.
        // From the start, variables are set that normally would satisfy the ifPart of some sentries.
        // However, due to the fact the triggerMode is onEvent, these sentries are evaluated when the complete event happens and not before.

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testTriggerMode")
                // Even though the variables are true, they should not be evaluated on start due to the onEvent triggerMode
                .variable("goToC", true)
                .start();

        // There should be no if parts stored
        assertSentryPartInstanceCount(caseInstance, 0);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("B");

        // When completing B, we're setting an extra variable to go to stage 1. The goToC variable is still true and should now be evaluated.
        cmmnTaskService.complete(tasks.get(0).getId(), Collections.singletonMap("gotoStage1", true));

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");
        assertSentryPartInstanceCount(caseInstance, 0);

        // Now we're completing B again, changing the variable. If we were using the default triggerMode,
        // the ifPart would already have been stored and the wrong thing would be activated now.
        // However, the onEvent semantics make sure the correct thing happens here.
        cmmnTaskService.complete(tasks.get(1).getId(), Collections.singletonMap("goToC", false));

        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C", "D");
        assertSentryPartInstanceCount(caseInstance, 0);

    }

    @Test
    @CmmnDeployment
    public void testTriggerModeOnEventConditionCalledOnce() {

        // This tests verifies that the ifPart of an onEvent triggered sentry doesn't get called unless the event happens

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIfPartTriggeredOnce")
                .variable("var", new TestCondition())
                .start();
        assertThat(TestCondition.COUNTER.get()).isZero();

        List<Task> tasks = cmmnTaskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Guarded task", "The task");

        cmmnTaskService.complete(tasks.get(1).getId());
        assertThat(TestCondition.COUNTER.get()).isEqualTo(1);
        tasks = cmmnTaskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testExitTriggersAnotherExit() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("exitTriggersAnotherExit")
                .variable("var", true)
                .start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");

        // Completing A cascades into exiting B and C when using the default trigger mode, as there is memory.
        // This should be the same case in 'onEvent', as both events are part of the same 'evaluation cycle'.
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).isEmpty();
        assertCaseInstanceEnded(caseInstance);
    }

    private void assertSentryPartInstanceCount(CaseInstance caseInstance, int count) {
        List<SentryPartInstanceEntity> sentryPartInstanceEntities = cmmnEngineConfiguration.getCommandExecutor()
                .execute(new Command<List<SentryPartInstanceEntity>>() {

                    @Override
                    public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                        return CommandContextUtil.getSentryPartInstanceEntityManager(commandContext)
                                .findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
                    }
                });
        assertThat(sentryPartInstanceEntities).hasSize(count);
    }

    // Just for testing purposes using a serialized variable
    public static class TestCondition implements Serializable {

        public static AtomicInteger COUNTER = new AtomicInteger(0);

        public static boolean calculate() {
            COUNTER.incrementAndGet();
            return true;
        }

    }

}

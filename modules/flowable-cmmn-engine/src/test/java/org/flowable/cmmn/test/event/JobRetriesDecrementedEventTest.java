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
package org.flowable.cmmn.test.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.common.engine.api.delegate.event.FlowableEngineEventType.ENTITY_UPDATED;
import static org.flowable.common.engine.api.delegate.event.FlowableEngineEventType.JOB_RETRIES_DECREMENTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
public class JobRetriesDecrementedEventTest extends FlowableCmmnTestCase {

    TestEventListener listener = new TestEventListener();

    @BeforeEach
    void addListener() {
        listener.events.clear();
        cmmnEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    void removeListener() {
        cmmnEngineConfiguration.getEventDispatcher().removeEventListener(listener);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTask.cmmn")
    void noEventForSuccess() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(task.getName()).isEqualTo("Task before service task");
        cmmnTaskService.complete(task.getId());

        waitForJobExecutorToProcessAllAsyncJobs();

        assertThat(listener.events).isEmpty();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithFailure.cmmn")
    void jobRetriesDecrementedOnFailure() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(task.getName()).isEqualTo("Task before service task");
        cmmnTaskService.complete(task.getId());

        waitForJobExecutorToProcessAllAsyncJobs();

        assertThat(listener.events)
                .filteredOn( e -> caseInstance.getId().equals(getScopeId(e)))
                .extracting(FlowableEvent::getType)
                .containsOnly(JOB_RETRIES_DECREMENTED, ENTITY_UPDATED);
    }

    private String getScopeId(FlowableEvent e) {
        return ((Job) ((FlowableEngineEntityEvent) e).getEntity()).getScopeId();
    }

    private static class TestEventListener extends AbstractFlowableEventListener {
        private static final Collection<? extends FlowableEventType> SUPPORTED_TYPES = Set.of(JOB_RETRIES_DECREMENTED, ENTITY_UPDATED);
        List<FlowableEvent> events = new ArrayList<>();

        @Override
        public void onEvent(FlowableEvent event) {
            events.add(event);
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }

        @Override
        public Collection<? extends FlowableEventType> getTypes() {
            return SUPPORTED_TYPES;
        }
    }
}

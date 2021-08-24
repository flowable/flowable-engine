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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.event.FlowableEntityEventImpl;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author David Lamas
 */
public class TaskAssignedEventTest extends FlowableCmmnTestCase {
    protected CustomEventListener listener;

    @Before
    public void setUp() {
        listener = new CustomEventListener();
        cmmnEngineConfiguration.getEventDispatcher().addEventListener(listener, FlowableEngineEventType.TASK_ASSIGNED);
    }

    @After
    public void tearDown() {
        if (listener != null) {
            cmmnEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testCaseInstanceEvents() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessKey("business key")
                .name("name")
                .start();

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();

        assertThat(task.getAssignee()).isNotNull();
        assertThat(listener.caughtEvent).isNotNull()
                .isInstanceOf(FlowableEntityEventImpl.class);

        FlowableEntityEventImpl caughtEvent = (FlowableEntityEventImpl) listener.caughtEvent;
        assertThat(caughtEvent.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(caughtEvent.getScopeDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        assertThat(caughtEvent.getSubScopeId()).isEqualTo(task.getId());
        assertThat(caughtEvent.getScopeType()).isEqualTo(task.getScopeType());

        assertThat(caughtEvent.getEntity())
                .isNotNull()
                .isInstanceOf(Task.class);
        Task taskEntity = (Task) caughtEvent.getEntity();
        assertThat(taskEntity.getId()).isEqualTo(task.getId());
        assertThat(taskEntity.getAssignee()).isEqualTo(task.getAssignee());
    }

    public static class CustomEventListener extends AbstractFlowableEventListener {
        protected FlowableEvent caughtEvent;

        @Override
        public void onEvent(FlowableEvent event) {
            this.caughtEvent = event;
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }
}

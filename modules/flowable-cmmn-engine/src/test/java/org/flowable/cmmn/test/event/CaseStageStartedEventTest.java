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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flowable.cmmn.api.event.FlowableCaseStageStartedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Micha Kiener
 */
public class CaseStageStartedEventTest extends FlowableCmmnTestCase {
    protected CustomEventListener stageListener;

    @BeforeEach
    public void setUp() {
        stageListener = new CustomEventListener();
        cmmnEngineConfiguration.getEventDispatcher().addEventListener(stageListener, FlowableEngineEventType.STAGE_STARTED);
    }

    @AfterEach
    public void tearDown() {
        if (stageListener != null) {
            cmmnEngineConfiguration.getEventDispatcher().removeEventListener(stageListener);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/event/Case_Stage_Event_Test_Case.cmmn")
    public void testCaseStageStartedEvents() {
        List<FlowableEvent> events = new ArrayList<>();
        stageListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseStageStartedEvent caseStageStartedEvent) {
                CaseInstance eventCaseInstance = caseStageStartedEvent.getCaseInstance();
                assertThat(caseStageStartedEvent.getProcessInstanceId()).isNull();
                assertThat(caseStageStartedEvent.getExecutionId()).isNull();
                assertThat(caseStageStartedEvent.getProcessDefinitionId()).isNull();
                assertThat(caseStageStartedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseStageStartedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());
                assertThat(caseStageStartedEvent.getSubScopeId()).isNotNull().isEqualTo(caseStageStartedEvent.getEntity().getId());
                assertThat(caseStageStartedEvent.getScopeDefinitionId()).isNotNull().isEqualTo(eventCaseInstance.getCaseDefinitionId());

                if (events.isEmpty()) {
                    assertThat(caseStageStartedEvent.getEntity().getName()).isEqualTo("Stage A");
                } else {
                    assertThat(caseStageStartedEvent.getEntity().getName()).isEqualTo("Stage B");
                }
                events.add(flowableEvent);
            }
        };

        // start the case which will also need to throw two stage started events (Stage A and embedded child Stage B)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseStageEventTestCase")
                .start();

        List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
        assertThat(planItemInstances).hasSize(6);
        assertPlanItemInstanceState(planItemInstances, "Stage A", PlanItemInstanceState.ACTIVE);
        assertPlanItemInstanceState(planItemInstances, "Stage B", PlanItemInstanceState.ACTIVE);

        assertThat(events).hasSize(2);
    }

    public static class CustomEventListener extends AbstractFlowableEventListener {
        private Consumer<FlowableEvent> eventConsumer;

        @Override
        public void onEvent(FlowableEvent event) {
            if (eventConsumer != null) {
                eventConsumer.accept(event);
            }
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
    }
}

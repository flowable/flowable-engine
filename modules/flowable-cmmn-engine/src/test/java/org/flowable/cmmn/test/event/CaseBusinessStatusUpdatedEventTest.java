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

import org.flowable.cmmn.api.event.FlowableCaseBusinessStatusUpdatedEvent;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Matthias Stöckli
 */
public class CaseBusinessStatusUpdatedEventTest extends FlowableCmmnTestCase {
    protected CustomEventListener businessStatusUpdatedEventListener;

    @BeforeEach
    public void setUp() {
        businessStatusUpdatedEventListener = new CustomEventListener();
        cmmnEngineConfiguration.getEventDispatcher().addEventListener(businessStatusUpdatedEventListener, FlowableEngineEventType.BUSINESS_STATUS_UPDATED);
    }

    @AfterEach
    public void tearDown() {
        if (businessStatusUpdatedEventListener != null) {
            cmmnEngineConfiguration.getEventDispatcher().removeEventListener(businessStatusUpdatedEventListener);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testEmptyBusinessStatusUpdatedFromEmptyEvent() {
        List<FlowableEvent> events = new ArrayList<>();
        businessStatusUpdatedEventListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseBusinessStatusUpdatedEvent caseBusinessStatusUpdatedEvent) {
                CaseInstance eventCaseInstance = (CaseInstance) caseBusinessStatusUpdatedEvent.getEntity();
                assertThat(caseBusinessStatusUpdatedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseBusinessStatusUpdatedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());
                assertThat(caseBusinessStatusUpdatedEvent.getOldBusinessStatus()).isEqualTo(null);
                assertThat(caseBusinessStatusUpdatedEvent.getNewBusinessStatus()).isEqualTo("newStatus");
                events.add(flowableEvent);
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        // Set business status for the first time
        cmmnRuntimeService.updateBusinessStatus(caseInstance.getId(), "newStatus");
    }


    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneTaskCase.cmmn")
    public void testBusinessStatusUpdatedEvent() {
        List<FlowableEvent> events = new ArrayList<>();
        businessStatusUpdatedEventListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableCaseBusinessStatusUpdatedEvent caseBusinessStatusUpdatedEvent) {
                CaseInstance eventCaseInstance = (CaseInstance) caseBusinessStatusUpdatedEvent.getEntity();
                assertThat(caseBusinessStatusUpdatedEvent.getScopeType()).isEqualTo(ScopeTypes.CMMN);
                assertThat(caseBusinessStatusUpdatedEvent.getScopeId()).isNotNull().isEqualTo(eventCaseInstance.getId());

                assertThat(caseBusinessStatusUpdatedEvent.getOldBusinessStatus()).isEqualTo("oldStatus");
                assertThat(caseBusinessStatusUpdatedEvent.getNewBusinessStatus()).isEqualTo("newStatus");
                events.add(flowableEvent);
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .businessStatus("oldStatus")
                .start();

        // Update the business status
        cmmnRuntimeService.updateBusinessStatus(caseInstance.getId(), "newStatus");
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

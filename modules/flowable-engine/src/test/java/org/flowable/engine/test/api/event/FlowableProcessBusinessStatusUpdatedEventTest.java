package org.flowable.engine.test.api.event;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.delegate.event.FlowableProcessBusinessStatusUpdatedEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
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
public class FlowableProcessBusinessStatusUpdatedEventTest extends PluggableFlowableTestCase {
    protected CustomEventListener businessStatusUpdatedEventListener;

    @BeforeEach
    public void setUp() {
        businessStatusUpdatedEventListener = new CustomEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(businessStatusUpdatedEventListener, FlowableEngineEventType.BUSINESS_STATUS_UPDATED);
    }

    @AfterEach
    public void tearDown() {
        if (businessStatusUpdatedEventListener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(businessStatusUpdatedEventListener);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testEmptyBusinessStatusFromEmptyUpdatedEvent() {
        List<FlowableEvent> events = new ArrayList<>();
        businessStatusUpdatedEventListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableProcessBusinessStatusUpdatedEvent processBusinessStatusUpdatedEvent) {
                Execution execution = (Execution) processBusinessStatusUpdatedEvent.getEntity();
                assertThat(processBusinessStatusUpdatedEvent.getScopeType()).isEqualTo(ScopeTypes.BPMN);
                assertThat(processBusinessStatusUpdatedEvent.getScopeId()).isNotNull().isEqualTo(execution.getId());
                assertThat(processBusinessStatusUpdatedEvent.getOldBusinessStatus()).isEqualTo(null);
                assertThat(processBusinessStatusUpdatedEvent.getNewBusinessStatus()).isEqualTo("newStatus");
                events.add(flowableEvent);
            }
        };

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        // Set business status for the first time
        runtimeService.updateBusinessStatus(processInstance.getId(), "newStatus");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testBusinessStatusUpdatedEvent() {
        List<FlowableEvent> events = new ArrayList<>();
        businessStatusUpdatedEventListener.eventConsumer = (flowableEvent) -> {
            if (flowableEvent instanceof FlowableProcessBusinessStatusUpdatedEvent processBusinessStatusUpdatedEvent) {
                Execution execution = (Execution) processBusinessStatusUpdatedEvent.getEntity();
                assertThat(processBusinessStatusUpdatedEvent.getScopeType()).isEqualTo(ScopeTypes.BPMN);
                assertThat(processBusinessStatusUpdatedEvent.getScopeId()).isNotNull().isEqualTo(execution.getId());
                assertThat(processBusinessStatusUpdatedEvent.getOldBusinessStatus()).isEqualTo("oldStatus");
                assertThat(processBusinessStatusUpdatedEvent.getNewBusinessStatus()).isEqualTo("newStatus");
                events.add(flowableEvent);
            }
        };

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .businessStatus("oldStatus")
                .start();


        // Update the business status
        runtimeService.updateBusinessStatus(processInstance.getId(), "newStatus");
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

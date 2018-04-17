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
package org.flowable.engine.impl.event.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.impl.event.logger.handler.ActivityCompensatedEventHandler;
import org.flowable.engine.impl.event.logger.handler.ActivityCompletedEventHandler;
import org.flowable.engine.impl.event.logger.handler.ActivityErrorReceivedEventHandler;
import org.flowable.engine.impl.event.logger.handler.ActivityMessageEventHandler;
import org.flowable.engine.impl.event.logger.handler.ActivitySignaledEventHandler;
import org.flowable.engine.impl.event.logger.handler.ActivityStartedEventHandler;
import org.flowable.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.flowable.engine.impl.event.logger.handler.ProcessInstanceEndedEventHandler;
import org.flowable.engine.impl.event.logger.handler.ProcessInstanceStartedEventHandler;
import org.flowable.engine.impl.event.logger.handler.SequenceFlowTakenEventHandler;
import org.flowable.engine.impl.event.logger.handler.TaskAssignedEventHandler;
import org.flowable.engine.impl.event.logger.handler.TaskCompletedEventHandler;
import org.flowable.engine.impl.event.logger.handler.TaskCreatedEventHandler;
import org.flowable.engine.impl.event.logger.handler.VariableCreatedEventHandler;
import org.flowable.engine.impl.event.logger.handler.VariableDeletedEventHandler;
import org.flowable.engine.impl.event.logger.handler.VariableUpdatedEventHandler;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 */
public class EventLogger extends AbstractFlowableEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogger.class);

    private static final String EVENT_FLUSHER_KEY = "eventFlusher";

    protected Clock clock;
    protected ObjectMapper objectMapper;

    // Mapping of type -> handler
    protected Map<FlowableEngineEventType, Class<? extends EventLoggerEventHandler>> eventHandlers = new HashMap<>();

    // Listeners for new events
    protected List<EventLoggerListener> listeners;

    public EventLogger() {
        initializeDefaultHandlers();
    }

    public EventLogger(Clock clock, ObjectMapper objectMapper) {
        this();
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    protected void initializeDefaultHandlers() {
        addEventHandler(FlowableEngineEventType.TASK_CREATED, TaskCreatedEventHandler.class);
        addEventHandler(FlowableEngineEventType.TASK_COMPLETED, TaskCompletedEventHandler.class);
        addEventHandler(FlowableEngineEventType.TASK_ASSIGNED, TaskAssignedEventHandler.class);

        addEventHandler(FlowableEngineEventType.SEQUENCEFLOW_TAKEN, SequenceFlowTakenEventHandler.class);

        addEventHandler(FlowableEngineEventType.ACTIVITY_COMPLETED, ActivityCompletedEventHandler.class);
        addEventHandler(FlowableEngineEventType.ACTIVITY_STARTED, ActivityStartedEventHandler.class);
        addEventHandler(FlowableEngineEventType.ACTIVITY_SIGNALED, ActivitySignaledEventHandler.class);
        addEventHandler(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED, ActivityMessageEventHandler.class);
        addEventHandler(FlowableEngineEventType.ACTIVITY_MESSAGE_WAITING, ActivityMessageEventHandler.class);

        addEventHandler(FlowableEngineEventType.ACTIVITY_COMPENSATE, ActivityCompensatedEventHandler.class);
        addEventHandler(FlowableEngineEventType.ACTIVITY_ERROR_RECEIVED, ActivityErrorReceivedEventHandler.class);

        addEventHandler(FlowableEngineEventType.VARIABLE_CREATED, VariableCreatedEventHandler.class);
        addEventHandler(FlowableEngineEventType.VARIABLE_DELETED, VariableDeletedEventHandler.class);
        addEventHandler(FlowableEngineEventType.VARIABLE_UPDATED, VariableUpdatedEventHandler.class);
    }

    @Override
    public void onEvent(FlowableEvent event) {
        EventLoggerEventHandler eventHandler = getEventHandler(event);
        if (eventHandler != null) {

            // Events are flushed when command context is closed
            CommandContext currentCommandContext = Context.getCommandContext();
            EventFlusher eventFlusher = (EventFlusher) currentCommandContext.getAttribute(EVENT_FLUSHER_KEY);

            if (eventFlusher == null) {

                eventFlusher = createEventFlusher();
                if (eventFlusher == null) {
                    eventFlusher = new DatabaseEventFlusher(); // Default
                }
                currentCommandContext.addAttribute(EVENT_FLUSHER_KEY, eventFlusher);

                currentCommandContext.addCloseListener(eventFlusher);
                currentCommandContext
                        .addCloseListener(new CommandContextCloseListener() {

                            @Override
                            public void closing(CommandContext commandContext) {
                            }

                            @Override
                            public void closed(CommandContext commandContext) {
                                // For those who are interested: we can now broadcast the events were added
                                if (listeners != null) {
                                    for (EventLoggerListener listener : listeners) {
                                        listener.eventsAdded(EventLogger.this);
                                    }
                                }
                            }

                            @Override
                            public void afterSessionsFlush(CommandContext commandContext) {
                            }

                            @Override
                            public void closeFailure(CommandContext commandContext) {
                            }

                        });
            }

            eventFlusher.addEventHandler(eventHandler);
        }
    }

    // Subclasses can override this if defaults are not ok
    protected EventLoggerEventHandler getEventHandler(FlowableEvent event) {

        Class<? extends EventLoggerEventHandler> eventHandlerClass = null;
        if (event.getType().equals(FlowableEngineEventType.ENTITY_INITIALIZED)) {
            Object entity = ((FlowableEntityEvent) event).getEntity();
            if (entity instanceof ExecutionEntity) {
                ExecutionEntity executionEntity = (ExecutionEntity) entity;
                if (executionEntity.getProcessInstanceId().equals(executionEntity.getId())) {
                    eventHandlerClass = ProcessInstanceStartedEventHandler.class;
                }
            }
        } else if (event.getType().equals(FlowableEngineEventType.ENTITY_DELETED)) {
            Object entity = ((FlowableEntityEvent) event).getEntity();
            if (entity instanceof ExecutionEntity) {
                ExecutionEntity executionEntity = (ExecutionEntity) entity;
                if (executionEntity.getProcessInstanceId().equals(executionEntity.getId())) {
                    eventHandlerClass = ProcessInstanceEndedEventHandler.class;
                }
            }
        } else {
            // Default: dedicated mapper for the type
            eventHandlerClass = eventHandlers.get(event.getType());
        }

        if (eventHandlerClass != null) {
            return instantiateEventHandler(event, eventHandlerClass);
        }

        return null;
    }

    protected EventLoggerEventHandler instantiateEventHandler(FlowableEvent event,
            Class<? extends EventLoggerEventHandler> eventHandlerClass) {
        try {
            EventLoggerEventHandler eventHandler = eventHandlerClass.newInstance();
            eventHandler.setTimeStamp(clock.getCurrentTime());
            eventHandler.setEvent(event);
            eventHandler.setObjectMapper(objectMapper);
            return eventHandler;
        } catch (Exception e) {
            LOGGER.warn("Could not instantiate {}, this is most likely a programmatic error", eventHandlerClass);
        }
        return null;
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    public void addEventHandler(FlowableEngineEventType eventType, Class<? extends EventLoggerEventHandler> eventHandlerClass) {
        eventHandlers.put(eventType, eventHandlerClass);
    }

    public void addEventLoggerListener(EventLoggerListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>(1);
        }
        listeners.add(listener);
    }

    /**
     * Subclasses that want something else than the database flusher should override this method
     */
    protected EventFlusher createEventFlusher() {
        return null;
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<EventLoggerListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<EventLoggerListener> listeners) {
        this.listeners = listeners;
    }

}

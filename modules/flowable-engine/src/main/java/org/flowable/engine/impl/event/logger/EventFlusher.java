package org.flowable.engine.impl.event.logger;

import java.util.List;

import org.flowable.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.flowable.engine.impl.interceptor.CommandContextCloseListener;

/**
 * @author Joram Barrez
 */
public interface EventFlusher extends CommandContextCloseListener {

  List<EventLoggerEventHandler> getEventHandlers();

  void setEventHandlers(List<EventLoggerEventHandler> eventHandlers);

  void addEventHandler(EventLoggerEventHandler databaseEventLoggerEventHandler);

}

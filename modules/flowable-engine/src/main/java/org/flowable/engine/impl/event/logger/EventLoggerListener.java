package org.flowable.engine.impl.event.logger;

/**
 * @author Joram Barrez
 */
public interface EventLoggerListener {

  void eventsAdded(EventLogger databaseEventLogger);

}

package org.flowable.crystalball.simulator.delegate.event.impl;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.delegate.event.Function;
import org.flowable.engine.event.EventLogEntry;

/**
 * This class provides abstract base for ActivitiEvent -> SimulationEvent transformation
 * 
 * @author martin.grofcik
 */
public abstract class EventLog2SimulationEventFunction implements Function<EventLogEntry, SimulationEvent> {
  protected final String simulationEventType;

  public EventLog2SimulationEventFunction(String simulationEventType) {
    this.simulationEventType = simulationEventType;
  }
}

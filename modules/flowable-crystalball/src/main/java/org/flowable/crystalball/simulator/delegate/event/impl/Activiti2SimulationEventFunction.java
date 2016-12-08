package org.flowable.crystalball.simulator.delegate.event.impl;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.delegate.event.Function;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;

/**
 * This class provides abstract base for ActivitiEvent -> SimulationEvent transformation
 * 
 * @author martin.grofcik
 */
public abstract class Activiti2SimulationEventFunction implements Function<FlowableEvent, SimulationEvent> {
  protected final String simulationEventType;

  public Activiti2SimulationEventFunction(String simulationEventType) {
    this.simulationEventType = simulationEventType;
  }
}

package org.flowable.crystalball.simulator.delegate.event.impl;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.delegate.event.Function;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class provides abstract base to records Activiti events
 * 
 * @author martin.grofcik
 */
public abstract class AbstractRecordActivitiEventListener implements FlowableEventListener {
  protected List<Function<FlowableEvent, SimulationEvent>> transformers;

  public AbstractRecordActivitiEventListener(List<Function<FlowableEvent, SimulationEvent>> transformers) {
    this.transformers = transformers;
  }

  public abstract Collection<SimulationEvent> getSimulationEvents();

  @Override
  public void onEvent(FlowableEvent event) {
    Collection<SimulationEvent> simulationEvents = transform(event);
    store(simulationEvents);
  }

  protected abstract void store(Collection<SimulationEvent> simulationEvents);

  protected Collection<SimulationEvent> transform(FlowableEvent event) {
    List<SimulationEvent> simEvents = new ArrayList<SimulationEvent>();
    for (Function<FlowableEvent, SimulationEvent> t : transformers) {
      SimulationEvent simEvent = t.apply(event);
      if (simEvent != null)
        simEvents.add(simEvent);
    }
    return simEvents;
  }

  @Override
  public boolean isFailOnException() {
    return true;
  }
}

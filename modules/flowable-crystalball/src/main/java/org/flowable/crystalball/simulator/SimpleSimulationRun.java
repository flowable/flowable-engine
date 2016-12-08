package org.flowable.crystalball.simulator;

import org.flowable.crystalball.simulator.impl.NoopEventHandler;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.delegate.VariableScope;
import org.flowable.engine.impl.ProcessEngineImpl;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author martin.grofcik
 */
public class SimpleSimulationRun extends AbstractSimulationRun {

  protected EventCalendar eventCalendar;

  /** simulation start date */
  protected Date simulationStartDate = new Date(0);
  protected Date dueDate = null;

  protected SimpleSimulationRun(Builder builder) {
    super(builder.eventHandlers);
    this.eventCalendar = builder.getEventCalendar();
    this.processEngine = builder.getProcessEngine();
    // init internal event handler map.
    eventHandlerMap.put(SimulationConstants.TYPE_END_SIMULATION, new NoopEventHandler());
  }

  @Override
  public void close() {
    // remove simulation from simulation context
    SimulationRunContext.getEventCalendar().clear();
    SimulationRunContext.removeEventCalendar();
    SimulationRunContext.getProcessEngine().close();
    SimulationRunContext.removeProcessEngine();
  }

  @Override
  protected void initSimulationRunContext(VariableScope execution) {// init new process engine
    try {
      // add context in which simulation run is executed
      SimulationRunContext.setEventCalendar(eventCalendar);
      SimulationRunContext.setProcessEngine(processEngine);
      SimulationRunContext.setExecution(execution);

      // run simulation
      // init context and task calendar and simulation time is set to current
      SimulationRunContext.getClock().setCurrentTime(simulationStartDate);

      if (dueDate != null)
        SimulationRunContext.getEventCalendar().addEvent(new SimulationEvent.Builder(SimulationConstants.TYPE_END_SIMULATION).simulationTime(dueDate.getTime()).build());
    } catch (Exception e) {
      throw new FlowableException("Unable to initialize simulation run", e);
    }
  }

  @Override
  protected boolean simulationEnd(SimulationEvent event) {
    if (event != null && event.getType().equals(SimulationConstants.TYPE_BREAK_SIMULATION))
      return true;
    if (dueDate != null)
      return event == null || (SimulationRunContext.getClock().getCurrentTime().after(dueDate));
    return event == null;
  }

  public static class Builder {
    private Map<String, SimulationEventHandler> eventHandlers = Collections.emptyMap();
    private ProcessEngineImpl processEngine;
    private EventCalendar eventCalendar;

    public ProcessEngineImpl getProcessEngine() {
      return processEngine;
    }

    public Builder processEngine(ProcessEngineImpl processEngine) {
      this.processEngine = processEngine;
      return this;
    }

    public EventCalendar getEventCalendar() {
      return eventCalendar;
    }

    public Builder eventCalendar(EventCalendar eventCalendar) {
      this.eventCalendar = eventCalendar;
      return this;
    }

    public Builder eventHandlers(Map<String, SimulationEventHandler> eventHandlersMap) {
      this.eventHandlers = eventHandlersMap;
      return this;
    }

    public SimpleSimulationRun build() {
      return new SimpleSimulationRun(this);
    }
  }
}

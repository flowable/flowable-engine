package org.activiti.crystalball.simulator;

import org.flowable.engine.impl.ProcessEngineImpl;
import org.springframework.beans.factory.FactoryBean;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author martin.grofcik
 */
public class SimpleSimulationRunFactory implements FactoryBean<SimulationRun> {

  protected Map<String, SimulationEventHandler> customEventHandlerMap;
  protected HashMap<String, SimulationEventHandler> eventHandlerMap;
  protected FactoryBean<ProcessEngineImpl> processEngine;
  protected FactoryBean<EventCalendar> eventCalendar;

  public SimpleSimulationRunFactory() {
  }

  @Override
  public SimulationRun getObject() throws Exception {
    return new SimpleSimulationRun.Builder().eventHandlers(customEventHandlerMap).processEngine(processEngine.getObject()).eventCalendar(eventCalendar.getObject()).build();
  }

  @Override
  public Class<? extends SimulationRun> getObjectType() {
    return SimpleSimulationRun.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  public void setCustomEventHandlerMap(Map<String, SimulationEventHandler> customEventHandlerMap) {
    this.customEventHandlerMap = customEventHandlerMap;
  }

  public void setEventHandlerMap(HashMap<String, SimulationEventHandler> eventHandlerMap) {
    this.eventHandlerMap = eventHandlerMap;
  }

  public void setProcessEngine(FactoryBean<ProcessEngineImpl> processEngine) {
    this.processEngine = processEngine;
  }

  public void setEventCalendar(FactoryBean<EventCalendar> eventCalendar) {
    this.eventCalendar = eventCalendar;
  }
}

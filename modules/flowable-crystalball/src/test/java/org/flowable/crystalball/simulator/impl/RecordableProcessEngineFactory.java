package org.flowable.crystalball.simulator.impl;

import org.flowable.crystalball.simulator.delegate.event.impl.AbstractRecordActivitiEventListener;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * This class is factory for recordable process engines
 */
public class RecordableProcessEngineFactory extends SimulationProcessEngineFactory {

  public RecordableProcessEngineFactory(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractRecordActivitiEventListener listener) {
    super(processEngineConfiguration);
    processEngineConfiguration.getEventDispatcher().addEventListener(listener);
  }

  @Override
  public ProcessEngineImpl getObject() {
    ProcessEngineImpl processEngine = super.getObject();

    return processEngine;
  }
}
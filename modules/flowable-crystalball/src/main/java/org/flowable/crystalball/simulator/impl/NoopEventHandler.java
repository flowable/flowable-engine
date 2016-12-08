package org.flowable.crystalball.simulator.impl;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.SimulationEventHandler;

/**
 * No operation event handler
 * 
 * @author martin.grofcik
 */
public class NoopEventHandler implements SimulationEventHandler {

  @Override
  public void init() {

  }

  @Override
  public void handle(SimulationEvent event) {

  }

}

package org.activiti.crystalball.simulator;

import org.flowable.engine.delegate.VariableScope;

/**
 * Allows to run simulation in debug mode
 * 
 * @author martin.grofcik
 */
public interface SimulationDebugger {
  /**
   * initialize simulation run
   * 
   * @param execution
   *          - variable scope to transfer variables from and to simulation run
   */
  void init(VariableScope execution);

  /**
   * step one simulation event forward
   */
  void step();

  /**
   * continue in the simulation run
   */
  void runContinue();

  /**
   * execute simulation run till simulationTime
   */
  void runTo(long simulationTime);

  /**
   * execute simulation run till simulation event of the specific type
   */
  void runTo(String simulationEventType);

  /**
   * close simulation run
   */
  void close();
}

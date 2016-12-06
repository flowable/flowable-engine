package org.activiti.crystalball.simulator;

import org.flowable.engine.delegate.VariableScope;

/**
 * This is basic interface for SimRun implementation it allows to execute simulation without any break
 * 
 * @author martin.grofcik
 */
public interface SimulationRun {

  /**
   * executes simulation run according to configuration already set
   * 
   * @param execution
   *          execution is variable scope used to transfer input/output variable from and to simulation run.
   * @throws Exception
   */
  void execute(VariableScope execution);

}

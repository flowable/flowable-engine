package org.flowable.crystalball.process;

import org.flowable.crystalball.simulator.SimulationRun;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * This class implement task which runs simulation experiment
 * 
 * @author martin.grofcik
 */
public class SimulationRunTask implements JavaDelegate {

  private Expression simulationRunExpression;

  @Override
  public void execute(DelegateExecution execution) {
    SimulationRun simulationRun = (SimulationRun) simulationRunExpression.getValue(execution);
    simulationRun.execute(execution);
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setSimulationRun(Expression simulationRun) {
    this.simulationRunExpression = simulationRun;
  }
}

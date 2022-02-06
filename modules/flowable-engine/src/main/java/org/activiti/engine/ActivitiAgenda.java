package org.activiti.engine;

import org.activiti.engine.Agenda;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;

/**
 * This interface exposes methods needed to manipulate activiti agenda.
 */
public interface ActivitiAgenda extends Agenda {

  void planOperation(Runnable operation, ExecutionEntity executionEntity);

  void planContinueProcessOperation(ExecutionEntity execution);

  void planContinueProcessSynchronousOperation(ExecutionEntity execution);

  void planContinueProcessInCompensation(ExecutionEntity execution);

  void planContinueMultiInstanceOperation(ExecutionEntity execution);

  void planTakeOutgoingSequenceFlowsOperation(ExecutionEntity execution, boolean evaluateConditions);

  void planEndExecutionOperation(ExecutionEntity execution);

  void planTriggerExecutionOperation(ExecutionEntity execution);

  void planDestroyScopeOperation(ExecutionEntity execution);

  void planExecuteInactiveBehaviorsOperation();
}

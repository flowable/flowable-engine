/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine.impl.agenda;

import org.activiti.engine.ActivitiAgenda;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.delegate.ActivityBehavior;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * For each API call (and thus {@link Command}) being executed, a new agenda instance is created.
 * On this agenda, operations are put, which the {@link CommandExecutor} will keep executing until
 * all are executed.
 *
 * The agenda also gives easy access to methods to plan new operations when writing
 * {@link ActivityBehavior} implementations.
 *
 * During a {@link Command} execution, the agenda can always be fetched using {@link Context#getAgenda()}.
 *
 * @author Joram Barrez
 */
public class DefaultActivitiAgenda implements ActivitiAgenda {

  private static final Logger logger = LoggerFactory.getLogger(DefaultActivitiAgenda.class);

  protected LinkedList<Runnable> operations = new LinkedList<Runnable>();

  @Override
  public boolean isEmpty() {
    return operations.isEmpty();
  }

  @Override
  public Runnable getNextOperation() {
    return operations.poll();
  }

  /**
   * Generic method to plan a {@link Runnable}.
   */
  @Override
  public void planOperation(Runnable operation) {
    planOperation(operation, null);
  }

  /**
   * Generic method to plan a {@link Runnable}.
   */
  @Override
  public void planOperation(Runnable operation, ExecutionEntity executionEntity) {
    operations.add(operation);
    logger.debug("Operation {} added to agenda", operation.getClass());

    if (executionEntity != null) {
      Context.getCommandContext().addInvolvedExecution(executionEntity);
    }
  }

  /* SPECIFIC operations */

  @Override
  public void planContinueProcessOperation(ExecutionEntity execution) {
    planOperation(new ContinueProcessOperation(Context.getCommandContext(), execution), execution);
  }

  @Override
  public void planContinueProcessSynchronousOperation(ExecutionEntity execution) {
    planOperation(new ContinueProcessOperation(Context.getCommandContext(), execution, true, false), execution);
  }

  @Override
  public void planContinueProcessInCompensation(ExecutionEntity execution) {
    planOperation(new ContinueProcessOperation(Context.getCommandContext(), execution, false, true), execution);
  }

  @Override
  public void planContinueMultiInstanceOperation(ExecutionEntity execution) {
    planOperation(new ContinueMultiInstanceOperation(Context.getCommandContext(), execution), execution);
  }

  @Override
  public void planTakeOutgoingSequenceFlowsOperation(ExecutionEntity execution, boolean evaluateConditions) {
    planOperation(new TakeOutgoingSequenceFlowsOperation(Context.getCommandContext(), execution, evaluateConditions), execution);
  }

  @Override
  public void planEndExecutionOperation(ExecutionEntity execution) {
    planOperation(new EndExecutionOperation(Context.getCommandContext(), execution), execution);
  }

  @Override
  public void planTriggerExecutionOperation(ExecutionEntity execution) {
    planOperation(new TriggerExecutionOperation(Context.getCommandContext(), execution), execution);
  }

  @Override
  public void planDestroyScopeOperation(ExecutionEntity execution) {
    planOperation(new DestroyScopeOperation(Context.getCommandContext(), execution), execution);
  }

  @Override
  public void planExecuteInactiveBehaviorsOperation() {
    planOperation(new ExecuteInactiveBehaviorsOperation(Context.getCommandContext()));
  }

  public CommandContext getCommandContext() {
    return Context.getCommandContext();
  }

  public LinkedList<Runnable> getOperations() {
    return operations;
  }

}

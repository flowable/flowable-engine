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
package org.flowable.engine.impl.agenda;

import org.flowable.common.engine.impl.agenda.AbstractAgenda;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For each API call (and thus {@link Command}) being executed, a new agenda instance is created. On this agenda, operations are put, which the {@link CommandExecutor} will keep executing until all
 * are executed.
 *
 * The agenda also gives easy access to methods to plan new operations when writing {@link ActivityBehavior} implementations.
 *
 * During a {@link Command} execution, the agenda can always be fetched using {@link Context#getAgenda()}.
 *
 * @author Joram Barrez
 */
public class DefaultFlowableEngineAgenda extends AbstractAgenda implements FlowableEngineAgenda {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFlowableEngineAgenda.class);

    public DefaultFlowableEngineAgenda(CommandContext commandContext) {
        super(commandContext);
    }

    /**
     * Generic method to plan a {@link Runnable}.
     */
    @Override
    public void planOperation(Runnable operation, ExecutionEntity executionEntity) {
        operations.add(operation);
        LOGGER.debug("Operation {} added to agenda", operation.getClass());

        if (executionEntity != null) {
            CommandContextUtil.addInvolvedExecution(commandContext, executionEntity);
        }
    }

    /* SPECIFIC operations */

    @Override
    public void planContinueProcessOperation(ExecutionEntity execution) {
        planOperation(new ContinueProcessOperation(commandContext, execution), execution);
    }

    @Override
    public void planContinueProcessSynchronousOperation(ExecutionEntity execution) {
        planOperation(new ContinueProcessOperation(commandContext, execution, true, false), execution);
    }

    @Override
    public void planContinueProcessInCompensation(ExecutionEntity execution) {
        planOperation(new ContinueProcessOperation(commandContext, execution, false, true), execution);
    }

    @Override
    public void planContinueMultiInstanceOperation(ExecutionEntity execution, ExecutionEntity multiInstanceRootExecution, int loopCounter) {
        planOperation(new ContinueMultiInstanceOperation(commandContext, execution, multiInstanceRootExecution, loopCounter), execution);
    }

    @Override
    public void planTakeOutgoingSequenceFlowsOperation(ExecutionEntity execution, boolean evaluateConditions) {
        planOperation(new TakeOutgoingSequenceFlowsOperation(commandContext, execution, evaluateConditions), execution);
    }

    @Override
    public void planEndExecutionOperation(ExecutionEntity execution) {
        planOperation(new EndExecutionOperation(commandContext, execution), execution);
    }

    @Override
    public void planTriggerExecutionOperation(ExecutionEntity execution) {
        planOperation(new TriggerExecutionOperation(commandContext, execution), execution);
    }

    @Override
    public void planAsyncTriggerExecutionOperation(ExecutionEntity execution) {
        planOperation(new TriggerExecutionOperation(commandContext, execution, true), execution);
    }

    @Override
    public void planDestroyScopeOperation(ExecutionEntity execution) {
        planOperation(new DestroyScopeOperation(commandContext, execution), execution);
    }

    @Override
    public void planExecuteInactiveBehaviorsOperation() {
        planOperation(new ExecuteInactiveBehaviorsOperation(commandContext));
    }

}

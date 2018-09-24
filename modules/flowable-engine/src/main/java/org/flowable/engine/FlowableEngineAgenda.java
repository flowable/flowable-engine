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
package org.flowable.engine;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * This interface exposes methods needed to manipulate the agenda implementation.
 */
public interface FlowableEngineAgenda extends Agenda {

    void planOperation(Runnable operation, ExecutionEntity executionEntity);

    void planContinueProcessOperation(ExecutionEntity execution);

    void planContinueProcessSynchronousOperation(ExecutionEntity execution);

    void planContinueProcessInCompensation(ExecutionEntity execution);

    void planContinueMultiInstanceOperation(ExecutionEntity execution, ExecutionEntity multiInstanceRootExecution, int loopCounter);

    void planTakeOutgoingSequenceFlowsOperation(ExecutionEntity execution, boolean evaluateConditions);

    void planEndExecutionOperation(ExecutionEntity execution);
    
    void planEndExecutionOperationSynchronous(ExecutionEntity execution);

    void planTriggerExecutionOperation(ExecutionEntity execution);

    void planAsyncTriggerExecutionOperation(ExecutionEntity execution);

    void planDestroyScopeOperation(ExecutionEntity execution);

    void planExecuteInactiveBehaviorsOperation();
}
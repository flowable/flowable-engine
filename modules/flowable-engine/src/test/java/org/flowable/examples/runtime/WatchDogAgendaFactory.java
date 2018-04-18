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
package org.flowable.examples.runtime;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.FlowableEngineAgendaFactory;
import org.flowable.engine.impl.agenda.DefaultFlowableEngineAgenda;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

/**
 * This class is a simple watchdog agenda implementation. It throws exception in the case when watchdog limit is exceeded for fetching operations from agenda.
 */
public class WatchDogAgendaFactory implements FlowableEngineAgendaFactory {

    @Override
    public FlowableEngineAgenda createAgenda(CommandContext commandContext) {
        return new WatchDogAgenda(new DefaultFlowableEngineAgenda(commandContext));
    }

    private static class WatchDogAgenda implements FlowableEngineAgenda {

        private static final int WATCH_DOG_LIMIT = 10;

        private final FlowableEngineAgenda agenda;
        private int counter;

        @Override
        public Runnable getNextOperation() {
            if (counter < WATCH_DOG_LIMIT) {
                counter++;
                return agenda.getNextOperation();
            }
            throw new FlowableException("WatchDog limit exceeded.");
        }

        @Override
        public Runnable peekOperation() {
            return agenda.peekOperation();
        }

        @Override
        public void planOperation(Runnable operation, ExecutionEntity executionEntity) {
            agenda.planOperation(operation, executionEntity);
        }

        @Override
        public void planContinueProcessOperation(ExecutionEntity execution) {
            agenda.planContinueProcessOperation(execution);
        }

        @Override
        public void planContinueProcessSynchronousOperation(ExecutionEntity execution) {
            agenda.planContinueProcessSynchronousOperation(execution);
        }

        @Override
        public void planContinueProcessInCompensation(ExecutionEntity execution) {
            agenda.planContinueProcessInCompensation(execution);
        }

        @Override
        public void planContinueMultiInstanceOperation(ExecutionEntity execution, ExecutionEntity multiInstanceExecution, int loopCounter) {
            agenda.planContinueMultiInstanceOperation(execution, multiInstanceExecution, loopCounter);
        }

        @Override
        public void planTakeOutgoingSequenceFlowsOperation(ExecutionEntity execution, boolean evaluateConditions) {
            agenda.planTakeOutgoingSequenceFlowsOperation(execution, evaluateConditions);
        }

        @Override
        public void planEndExecutionOperation(ExecutionEntity execution) {
            agenda.planEndExecutionOperation(execution);
        }

        @Override
        public void planTriggerExecutionOperation(ExecutionEntity execution) {
            agenda.planTriggerExecutionOperation(execution);
        }

        @Override
        public void planAsyncTriggerExecutionOperation(ExecutionEntity execution) {
            agenda.planAsyncTriggerExecutionOperation(execution);
        }

        @Override
        public void planDestroyScopeOperation(ExecutionEntity execution) {
            agenda.planDestroyScopeOperation(execution);
        }

        @Override
        public void planExecuteInactiveBehaviorsOperation() {
            agenda.planExecuteInactiveBehaviorsOperation();
        }

        private WatchDogAgenda(FlowableEngineAgenda agenda) {
            this.agenda = agenda;
        }

        @Override
        public boolean isEmpty() {
            return agenda.isEmpty();
        }

        @Override
        public void planOperation(Runnable operation) {
            agenda.planOperation(operation);
        }

        @Override
        public void flush() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void close() {
            // TODO Auto-generated method stub
            
        }

    }
}

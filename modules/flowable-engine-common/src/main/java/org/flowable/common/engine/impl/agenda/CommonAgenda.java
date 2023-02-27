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
package org.flowable.common.engine.impl.agenda;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.flowable.common.engine.api.FlowableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class CommonAgenda implements Agenda {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final AgendaOperationDecorator operationDecorator;

    protected final LinkedList<Runnable> operations = new LinkedList<>();

    protected final List<ExecuteFutureActionOperation<?>> futureOperations = new ArrayList<>();

    public CommonAgenda() {
        this(operation -> operation);
    }

    public CommonAgenda(AgendaOperationDecorator operationDecorator) {
        // We can use the decorator to wrap and count the time that is happening while running operations
        this.operationDecorator = operationDecorator;
    }

    @Override
    public boolean isEmpty() {
        return operations.isEmpty() && futureOperations.isEmpty();
    }

    @Override
    public Runnable getNextOperation() {
        assertOperationsNotEmpty();
        if (!operations.isEmpty()) {
            return operations.poll();
        } else {
            // If there are no more operations than we need to wait until any of the schedule future operations are done
            List<ExecuteFutureActionOperation<?>> copyOperations = new ArrayList<>(futureOperations);
            futureOperations.clear();
            return new WaitForAnyFutureToFinishOperation(this, copyOperations);
        }
    }

    protected void assertOperationsNotEmpty() {
        if (operations.isEmpty() && futureOperations.isEmpty()) {
            throw new FlowableException("Unable to peek empty agenda.");
        }
    }

    @Override
    public void planOperation(Runnable operation) {
        Runnable decoratedOperation = operationDecorator.decorate(operation);
        operations.add(decoratedOperation);
        if (logger.isDebugEnabled()) {
            logger.debug("Operation {} added to agenda", operation);
        }
    }

    @Override
    public <V> void planFutureOperation(CompletableFuture<V> future, BiConsumer<V, Throwable> completeAction) {
        //TODO plan it here
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }
}

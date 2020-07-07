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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Filip Hrisafov
 */
public class OperationWithFuture<T> implements Runnable {

    protected final Agenda agenda;
    protected final Future<T> future;
    protected final BiConsumer<T, Throwable> action;

    public OperationWithFuture(Agenda agenda, Future<T> future, BiConsumer<T, Throwable> action) {
        this.agenda = agenda;
        this.future = future;
        this.action = action;
    }

    @Override
    public void run() {
        if (future.isDone()) {
            try {
                T value = future.get();
                action.accept(value, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new FlowableException("Future was interrupted", e);
            } catch (ExecutionException e) {
                action.accept(null, e.getCause());
            }
        } else if (future.isCancelled()) {
            action.accept(null, new FlowableException("Future was cancelled"));
        } else {
            // If the future is not done then plan the next operation
            agenda.planOperation(this);
        }

    }
}

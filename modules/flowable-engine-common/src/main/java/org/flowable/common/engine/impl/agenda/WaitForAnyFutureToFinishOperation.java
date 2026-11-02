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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Filip Hrisafov
 */
public class WaitForAnyFutureToFinishOperation implements Runnable {

    protected final Agenda agenda;
    protected final List<ExecuteFutureActionOperation<?>> futureOperations;
    protected final Duration timeout;

    public WaitForAnyFutureToFinishOperation(Agenda agenda, List<ExecuteFutureActionOperation<?>> futureOperations, Duration timeout) {
        this.agenda = agenda;
        this.futureOperations = futureOperations;
        if (timeout == null) {
            this.timeout = null;
        } else if (timeout.isNegative() || timeout.isZero()) {
            throw new FlowableException("max wait timeout has to be positive. It was " + timeout);
        } else {
            this.timeout = timeout;
        }
    }

    @Override
    public void run() {
        CompletableFuture[] anyOfFutures = new CompletableFuture[futureOperations.size()];

        for (int i = 0; i < futureOperations.size(); i++) {
            anyOfFutures[i] = futureOperations.get(i).getFuture();
        }
        try {
            CompletableFuture<Object> anyOfFuture = CompletableFuture.anyOf(anyOfFutures);
            if (timeout == null) {
                // This blocks until at least one is future is done
                anyOfFuture.get();
            } else {
                try {
                    // This blocks until at least one is future is done or the timeout is reached
                    anyOfFuture.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    // When the timeout is reached we need to cancel all the futures that are not done
                    for (ExecuteFutureActionOperation<?> futureOperation : futureOperations) {
                        if (!futureOperation.isDone()) {
                            // If there was a timeout then we need to cancel all the futures that have not completed already
                            futureOperation.getFuture().cancel(true);
                        }
                    }
                    throw new FlowableException("None of the available futures completed within the max timeout of " + timeout, e);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlowableException("Future was interrupted", e);
        } catch (ExecutionException e) {
            // If there was any exception then it will be handled by the appropriate action
        }

        // Now go through future operation and schedule them for execution if they are done
        for (ExecuteFutureActionOperation<?> futureOperation : futureOperations) {
            if (futureOperation.isDone()) {
                // If it is done then schedule it for execution
                agenda.planOperation(futureOperation);
            } else {
                // Otherwise plan a new future operation
                agenda.planFutureOperation((CompletableFuture<Object>) futureOperation.getFuture(),
                        (BiConsumer<Object, Throwable>) futureOperation.getAction());
            }
        }
    }
}

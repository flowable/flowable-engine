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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import org.flowable.common.engine.api.FlowableException;

/**
 * @author Filip Hrisafov
 */
public class ExecuteFutureActionOperation<T> implements Runnable {

    protected final CompletableFuture<T> future;
    protected final BiConsumer<T, Throwable> action;

    public ExecuteFutureActionOperation(CompletableFuture<T> future, BiConsumer<T, Throwable> action) {
        this.future = future;
        this.action = action;
    }

    @Override
    public void run() {
        try {
            T value = future.get();
            action.accept(value, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FlowableException("Future was interrupted", e);
        } catch (CancellationException e) {
            action.accept(null, new FlowableException("Future was canceled", e));
        } catch (ExecutionException e) {
            action.accept(null, e.getCause());
        }
    }

    public boolean isDone() {
        return future.isDone();
    }

    public CompletableFuture<T> getFuture() {
        return future;
    }

    public BiConsumer<T, Throwable> getAction() {
        return action;
    }
}

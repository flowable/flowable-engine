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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.flowable.common.engine.impl.interceptor.Session;

public interface Agenda extends Session {

    /**
     * Returns whether there currently are operations planned on the agenda. 
     */
    boolean isEmpty();

    /**
     * Get next operation from agenda and remove operation from the queue.
     *
     * @return next operation from the queue
     * throws {@link org.flowable.common.engine.api.FlowableException} in the case when agenda is empty
     */
    Runnable getNextOperation();

    /**
     * Plan operation for execution
     *
     * @param operation operation to run
     */
    void planOperation(Runnable operation);
    
    /**
     * Plan an operation for a future execution
     *
     * @param <V> the type of the value the future returns
     * @param future the future that will return the value
     * @param completeAction the action that should be invoked once the future completes
     */
    <V> void planFutureOperation(CompletableFuture<V> future, BiConsumer<V, Throwable> completeAction);

}

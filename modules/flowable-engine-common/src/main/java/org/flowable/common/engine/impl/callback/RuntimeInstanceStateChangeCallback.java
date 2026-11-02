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
package org.flowable.common.engine.impl.callback;

import org.flowable.common.engine.api.delegate.BusinessError;

/**
 * @author Joram Barrez
 */
public interface RuntimeInstanceStateChangeCallback {

    void stateChanged(CallbackData callbackData);

    /**
     * Called when an uncaught {@link BusinessError} occurs in a child instance that has a callback
     * to a parent instance in another engine. This allows cross-engine error propagation.
     *
     * The default implementation re-throws the error (backward compatible).
     * Implementations can override to propagate the error to the parent engine.
     *
     * @param callbackData the callback data identifying the parent instance
     * @param error the business error that was not caught in the child instance
     */
    default void onError(CallbackData callbackData, BusinessError error) {
        throw error;
    }

}

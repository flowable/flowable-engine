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

package org.flowable.engine.impl.context;

import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;

public class Flowable5CompatibilityContext {

    // Fallback handler is only set by the v5 CommandContextInterceptor
    protected static ThreadLocal<Flowable5CompatibilityHandler> fallbackFlowable5CompatibilityHandlerThreadLocal = new ThreadLocal<>();

    public static Flowable5CompatibilityHandler getFallbackFlowable5CompatibilityHandler() {
        return fallbackFlowable5CompatibilityHandlerThreadLocal.get();
    }

    public static void setFallbackFlowable5CompatibilityHandler(Flowable5CompatibilityHandler flowable5CompatibilityHandler) {
        fallbackFlowable5CompatibilityHandlerThreadLocal.set(flowable5CompatibilityHandler);
    }

    public static void removeFallbackFlowable5CompatibilityHandler() {
        fallbackFlowable5CompatibilityHandlerThreadLocal.remove();
    }

}

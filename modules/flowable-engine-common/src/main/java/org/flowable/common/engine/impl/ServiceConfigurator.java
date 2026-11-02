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
package org.flowable.common.engine.impl;

/**
 * @param <S> The configurable service / configuration
 * @author Filip Hrisafov
 */
public interface ServiceConfigurator<S> {

    int DEFAULT_PRIORITY = 0;

    /**
     * Called <b>before</b> any initialisation has been done.
     * This can for example be useful to change configuration settings before anything that uses those properties is created.
     * Allows to tweak the service by passing it, which allows tweaking it programmatically.
     */
    void beforeInit(S service);

    /**
     * Called when the service boots up, before it is usable, but after the initialisation of internal objects is done.
     * Allows to tweak it by passing it, which allows tweaking it programmatically.
     */
    void afterInit(S service);

    /**
     * When the {@link ServiceConfigurator} instances are used, they are first ordered by this priority number (lowest to highest).
     * If you have dependencies between {@link ServiceConfigurator} instances, use the priorities accordingly to order them as needed.
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

}

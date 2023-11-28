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
package org.flowable.engine.event;

import java.util.Map;

/**
 * A builder API to create an event subscription to start an event-based process instance whenever an event with a very specific
 * combination of correlation values occurs.
 * You can model an event-based start for a process model to create a new process instance whenever that event happens, but not, if
 * it should only start a process on a particular combination of correlation values.
 *
 * In order for this to work, you need a process definition with an event registry start, configured with the manual, correlation based
 * subscription behavior.
 *
 * @author Micha Kiener
 */
public interface ProcessStartEventSubscriptionBuilder {

    /**
     * Adds a specific correlation parameter value for the subscription, which means this value needs to exactly match the event
     * payload in order to trigger the process start (along with all registered correlation parameter values of course).
     *
     * @param parameterName the name of the correlation parameter
     * @param parameterValue the value of the correlation parameter
     * @return the builder to be used for method chaining
     */
    ProcessStartEventSubscriptionBuilder addCorrelationParameterValue(String parameterName, Object parameterValue);

    /**
     * Registers a list of correlation parameter values for the subscription. The result is the same as registering
     * them one after the other.
     *
     * @param parameters the map of correlation parameter values to be registered for the subscription
     * @return the builder to be used for method chaining
     */
    ProcessStartEventSubscriptionBuilder addCorrelationParameterValues(Map<String, Object> parameters);

    /**
     * Creates the event subscription with the registered combination of correlation parameter values and saves it.
     */
    void registerProcessStartEventSubscription();
}

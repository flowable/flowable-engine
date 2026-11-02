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
package org.flowable.engine.runtime;

import java.util.Map;

import org.flowable.eventsubscription.api.EventSubscription;

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
public interface ProcessInstanceStartEventSubscriptionBuilder {

    /**
     * Set the process definition to be started using a manually added subscription by its key. By default, always the latest version is
     * used to start a new process instance, unless you use {@link #doNotUpdateToLatestVersionAutomatically()} to mark the builder to stick to
     * exactly the current version of the process definition and don't update it, if a new version would be deployed later on.
     * This method is mandatory and will throw an exception when trying to register a subscription where the process definition key was not set or
     * is null.
     *
     * @param processDefinitionKey the key of the process definition to be started a new instance of when the subscription has a match at runtime
     * @return the builder to be used for method chaining
     */
    ProcessInstanceStartEventSubscriptionBuilder processDefinitionKey(String processDefinitionKey);

    /**
     * Mark the subscription to not use the latest process definition automatically, should there be a new version deployed after the subscription
     * was created. This means, adding the subscription will always stick to the current version of the process definition, and it will NOT be updated
     * automatically should there be a new version deployed later on. By default, when this method is not invoked on the builder, the subscription will
     * be updated automatically to the latest version when a new version of the process definition is deployed.
     * The subscription can still be updated to the latest version by manually migrating it to whatever version you want.
     *
     * @return the builder to be used for method chaining
     */
    ProcessInstanceStartEventSubscriptionBuilder doNotUpdateToLatestVersionAutomatically();

    /**
     * Adds a specific correlation parameter value for the subscription, which means this value needs to exactly match the event
     * payload in order to trigger the process start (along with all registered correlation parameter values of course).
     *
     * @param parameterName the name of the correlation parameter
     * @param parameterValue the value of the correlation parameter
     * @return the builder to be used for method chaining
     */
    ProcessInstanceStartEventSubscriptionBuilder addCorrelationParameterValue(String parameterName, Object parameterValue);

    /**
     * Registers a list of correlation parameter values for the subscription. The result is the same as registering
     * them one after the other.
     *
     * @param parameters the map of correlation parameter values to be registered for the subscription
     * @return the builder to be used for method chaining
     */
    ProcessInstanceStartEventSubscriptionBuilder addCorrelationParameterValues(Map<String, Object> parameters);

    /**
     * Set the tenant id for the subscription.
     *
     * @param tenantId the id of the tenant
     * @return the builder to be used for method chaining
     */
    ProcessInstanceStartEventSubscriptionBuilder tenantId(String tenantId);

    /**
     * Creates the event subscription with the registered combination of correlation parameter values and saves it.
     */
    EventSubscription subscribe();
}

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

import org.flowable.engine.RuntimeService;

/**
 * A builder API to modify or delete a manually created process start event subscription which was created and registered using the
 * {@link RuntimeService#createProcessStartEventSubscriptionBuilder()} builder API.
 * With this API you can modify one or more such subscriptions like migrating to a specific version of a process definition (if you choose to not automatically
 * migrate then to the latest version upon deployment of a new version). Or you can even delete one or more subscriptions based on the correlation parameter
 * values and event type.
 *
 * @author Micha Kiener
 */
public interface ProcessStartEventSubscriptionModificationBuilder {

    /**
     * Set the process definition using its specific id the manually created subscription is based on. This is mandatory and must be provided.
     *
     * @param processDefinitionId the id of the process definition the subscription is based on (an exact version of it)
     * @return the builder to be used for method chaining
     */
    ProcessStartEventSubscriptionModificationBuilder processDefinitionId(String processDefinitionId);

    /**
     * Adds a specific correlation parameter value for the subscription to be modified or deleted. If you register the same correlation parameter values
     * as when creating and registering the event subscription, only that particular one will be modified or deleted with this builder.
     * If you want to modify or delete all manually created subscriptions, don't register any correlation parameter values, which would result in all matching
     * the provided process definition and event-registry start event will be modified or deleted.
     *
     * @param parameterName the name of the correlation parameter
     * @param parameterValue the value of the correlation parameter
     * @return the builder to be used for method chaining
     */
    ProcessStartEventSubscriptionModificationBuilder addCorrelationParameterValue(String parameterName, Object parameterValue);

    /**
     * Registers a list of correlation parameter values for the subscription(s) to be modified or deleted.
     *
     * @param parameters the map of correlation parameter values to be registered for the subscription
     * @return the builder to be used for method chaining
     */
    ProcessStartEventSubscriptionModificationBuilder addCorrelationParameterValues(Map<String, Object> parameters);

    /**
     * Migrate all the matching event subscriptions to the latest process definition, which should be done if you want to manually upgrade the subscriptions
     * to the latest version of the process definition.
     */
    void migrateToLatestProcessDefinitionVersion();

    /**
     * Migrate all matching event subscriptions to the specific process definition.
     * @param processDefinitionId the id of the process definition to migrate to
     */
    void migrateToProcessDefinitionVersion(String processDefinitionId);

    /**
     * Deletes all the matching event subscriptions.
     */
    void deleteSubscriptions();
}

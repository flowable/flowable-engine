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
package org.flowable.eventregistry.api.model;

import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Joram Barrez
 */
public interface EventModelBuilder {

    /**
     * Each event type will uniquely be identified with a key
     * (similar to the key of a process/case/decision/... definition),
     * which is typically referenced in process/case/... models.
     */
    EventModelBuilder key(String key);
    
    /**
     * Set the name for the event deployment.
     */
    EventModelBuilder deploymentName(String deploymentName);
    
    /**
     * Set the resource name for the event model.
     */
    EventModelBuilder resourceName(String resourceName);
    
    /**
     * Set the category for the event deployment.
     */
    EventModelBuilder category(String category);
    
    /**
     * Set the tenant id for the event deployment.
     */
    EventModelBuilder deploymentTenantId(String deploymentTenantId);
    
    /**
     * Set the parent deployment id for the event deployment.
     */
    EventModelBuilder parentDeploymentId(String parentDeploymentId);

    /**
     * Defines one payload element of an event definition.
     * Such payload elements are data that is contained within an event.
     * If certain payload needs to be used to correlate runtime instances,
     * use the {@link #correlationParameter(String, String)} method.
     *
     * One {@link EventModel} typically has multiple such elements.
     */
    EventModelBuilder payload(String name, String type);

    /**
     * Defines one parameters for correlation that can be used in models to map onto.
     * Each correlation parameter is automatically a {@link #payload(String, String)} element.
     *
     * Will create a correlation {@link org.flowable.eventregistry.model.EventPayload EventPayload} behind the scenes.
     */
    EventModelBuilder correlationParameter(String name, String type);
    
    /**
     * Creates a new event model, but does not deploy it to the Event registry engine.
     */
    EventModel createEventModel();

    /**
     * Deploys a new event definition for this event model.
     */
    EventDeployment deploy();

}

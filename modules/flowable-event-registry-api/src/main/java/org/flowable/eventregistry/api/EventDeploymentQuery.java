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

package org.flowable.eventregistry.api;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link EventDeployment}s.
 * 
 * Note that it is impossible to retrieve the deployment resources through the results of this operation, since that would cause a huge transfer of (possibly) unneeded bytes over the wire.
 * 
 * To retrieve the actual bytes of a deployment resource use the operations on the {@link EventRepositoryService#getDeploymentResourceNames(String)} and
 * {@link EventRepositoryService#getResourceAsStream(String, String)}
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface EventDeploymentQuery extends Query<EventDeploymentQuery, EventDeployment> {

    /**
     * Only select deployments with the given deployment id.
     */
    EventDeploymentQuery deploymentId(String deploymentId);

    /**
     * Only select deployments with the given name.
     */
    EventDeploymentQuery deploymentName(String name);

    /**
     * Only select deployments with a name like the given string.
     */
    EventDeploymentQuery deploymentNameLike(String nameLike);

    /**
     * Only select deployments with the given category.
     * 
     * @see EventDeploymentBuilder#category(String)
     */
    EventDeploymentQuery deploymentCategory(String category);

    /**
     * Only select deployments that have a different category then the given one.
     * 
     * @see EventDeploymentBuilder#category(String)
     */
    EventDeploymentQuery deploymentCategoryNotEquals(String categoryNotEquals);

    /**
     * Only select deployment that have the given tenant id.
     */
    EventDeploymentQuery deploymentTenantId(String tenantId);

    /**
     * Only select deployments with a tenant id like the given one.
     */
    EventDeploymentQuery deploymentTenantIdLike(String tenantIdLike);

    /**
     * Only select deployments that do not have a tenant id.
     */
    EventDeploymentQuery deploymentWithoutTenantId();

    /** Only select deployments with the given event definition key. */
    EventDeploymentQuery eventDefinitionKey(String key);

    /**
     * Only select deployments with an event definition key like the given string.
     */
    EventDeploymentQuery eventDefinitionKeyLike(String keyLike);
    
    /** Only select deployments with the given channel definition key. */
    EventDeploymentQuery channelDefinitionKey(String key);

    /**
     * Only select deployments with a channel definition key like the given string.
     */
    EventDeploymentQuery channelDefinitionKeyLike(String keyLike);

    /**
     * Only select deployment that have the given deployment parent id.
     */
    EventDeploymentQuery parentDeploymentId(String deploymentParentId);

    /**
     * Only select deployments with a deployment parent id like the given one.
     */
    EventDeploymentQuery parentDeploymentIdLike(String deploymentParentIdLike);

    // sorting ////////////////////////////////////////////////////////

    /**
     * Order by deployment id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDeploymentQuery orderByDeploymentId();

    /**
     * Order by deployment name (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDeploymentQuery orderByDeploymentName();

    /**
     * Order by deployment time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDeploymentQuery orderByDeploymentTime();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDeploymentQuery orderByTenantId();
}

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

import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link EventDefinition}s.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface EventDefinitionQuery extends Query<EventDefinitionQuery, EventDefinition> {

    /** Only select eventdefinition with the given id. */
    EventDefinitionQuery eventDefinitionId(String eventDefinitionId);

    /** Only select forms with the given ids. */
    EventDefinitionQuery eventDefinitionIds(Set<String> eventDefinitionIds);

    /** Only select event definitions with the given category. */
    EventDefinitionQuery eventCategory(String category);

    /**
     * Only select event definitions where the category matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    EventDefinitionQuery eventCategoryLike(String categoryLike);

    /**
     * Only select event definitions that have a different category then the given one.
     * 
     * @see EventDeploymentBuilder#category(String)
     */
    EventDefinitionQuery eventCategoryNotEquals(String categoryNotEquals);

    /** Only select event definitions with the given name. */
    EventDefinitionQuery eventDefinitionName(String eventDefinitionName);

    /**
     * Only select event definitions where the name matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    EventDefinitionQuery eventDefinitionNameLike(String eventDefinitionNameLike);

    /**
     * Only select event definitions where the name matches the given parameter (case-insensitive).
     * The syntax that should be used is the same as in SQL, eg. %test%
     */
    EventDefinitionQuery eventDefinitionNameLikeIgnoreCase(String nameLikeIgnoreCase);

    /**
     * Only select event definitions that are deployed in a deployment with the given deployment id
     */
    EventDefinitionQuery deploymentId(String deploymentId);

    /**
     * Select event definitions that are deployed in deployments with the given set of ids
     */
    EventDefinitionQuery deploymentIds(Set<String> deploymentIds);

    /**
     * Only select event definitions that are deployed in a deployment with the given parent deployment id
     */
    EventDefinitionQuery parentDeploymentId(String parentDeploymentId);

    /**
     * Only select event definition with the given key.
     */
    EventDefinitionQuery eventDefinitionKey(String eventDefinitionKey);

    /**
     * Only select event definitions where the key matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    EventDefinitionQuery eventDefinitionKeyLike(String eventDefinitionKeyLike);

    /**
     * Only select event definitions where the key matches the given parameter (case-insensitive).
     * The syntax that should be used is the same as in SQL, eg. %test%
     */
    EventDefinitionQuery eventDefinitionKeyLikeIgnoreCase(String keyLikeIgnoreCase);
    
    /**
     * Only select event definitions with a certain version. Particularly useful when used in combination with {@link #eventDefinitionKey(String)}
     */
    EventDefinitionQuery eventVersion(Integer eventVersion);

    /**
     * Only select event definitions which version are greater than a certain version.
     */
    EventDefinitionQuery eventVersionGreaterThan(Integer eventVersion);

    /**
     * Only select event definitions which version are greater than or equals a certain version.
     */
    EventDefinitionQuery eventVersionGreaterThanOrEquals(Integer eventVersion);

    /**
     * Only select event definitions which version are lower than a certain version.
     */
    EventDefinitionQuery eventVersionLowerThan(Integer eventVersion);

    /**
     * Only select event definitions which version are lower than or equals a certain version.
     */
    EventDefinitionQuery eventVersionLowerThanOrEquals(Integer eventVersion);

    /**
     * Only select the event definitions which are the latest deployed (ie. which have the highest version number for the given key).
     * 
     * Can also be used without any other criteria (ie. query.latestVersion().list()), which will then give all the latest versions of all the deployed event definitions.
     * 
     * @throws FlowableIllegalArgumentException
     *             if used in combination with {{@link #eventVersion(Integer)} or {@link #deploymentId(String)}
     */
    EventDefinitionQuery latestVersion();

    /** Only select event definition with the given resource name. */
    EventDefinitionQuery eventDefinitionResourceName(String resourceName);

    /** Only select event definition with a resource name like the given . */
    EventDefinitionQuery eventDefinitionResourceNameLike(String resourceNameLike);

    /**
     * Only select event definitions that have the given tenant id.
     */
    EventDefinitionQuery tenantId(String tenantId);

    /**
     * Only select event definitions with a tenant id like the given one.
     */
    EventDefinitionQuery tenantIdLike(String tenantIdLike);

    /**
     * Only select event definitions that do not have a tenant id.
     */
    EventDefinitionQuery withoutTenantId();

    // ordering ////////////////////////////////////////////////////////////

    /**
     * Order by the category of the event definitions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDefinitionQuery orderByEventDefinitionCategory();

    /**
     * Order by event definition key (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDefinitionQuery orderByEventDefinitionKey();

    /**
     * Order by the id of the event definitions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDefinitionQuery orderByEventDefinitionId();

    /**
     * Order by the name of the event definitions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDefinitionQuery orderByEventDefinitionName();

    /**
     * Order by deployment id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDefinitionQuery orderByDeploymentId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventDefinitionQuery orderByTenantId();

}

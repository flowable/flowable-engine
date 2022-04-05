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

import java.util.Date;
import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link ChannelDefinition}s.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface ChannelDefinitionQuery extends Query<ChannelDefinitionQuery, ChannelDefinition> {

    /** Only select channel definition with the given id. */
    ChannelDefinitionQuery channelDefinitionId(String channelDefinitionId);

    /** Only select channel definitions with the given ids. */
    ChannelDefinitionQuery channelDefinitionIds(Set<String> channelDefinitionIds);

    /** Only select channel definitions with the given category. */
    ChannelDefinitionQuery channelCategory(String category);

    /**
     * Only select channel definitions where the category matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    ChannelDefinitionQuery channelCategoryLike(String categoryLike);

    /**
     * Only select channel definitions that have a different category then the given one.
     */
    ChannelDefinitionQuery channelCategoryNotEquals(String categoryNotEquals);

    /** Only select channel definitions with the given name. */
    ChannelDefinitionQuery channelDefinitionName(String channelDefinitionName);

    /**
     * Only select channel definitions where the name matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    ChannelDefinitionQuery channelDefinitionNameLike(String channelDefinitionNameLike);

    /**
     * Only select channel definitions where the name matches the given parameter (case-insensitive).
     * The syntax that should be used is the same as in SQL, eg. %test%
     */
    ChannelDefinitionQuery channelDefinitionNameLikeIgnoreCase(String nameLikeIgnoreCase);

    /**
     * Only select channel definitions that are deployed in a deployment with the given deployment id
     */
    ChannelDefinitionQuery deploymentId(String deploymentId);

    /**
     * Select channel definitions that are deployed in deployments with the given set of ids
     */
    ChannelDefinitionQuery deploymentIds(Set<String> deploymentIds);

    /**
     * Only select channel definitions that are deployed in a deployment with the given parent deployment id
     */
    ChannelDefinitionQuery parentDeploymentId(String parentDeploymentId);

    /**
     * Only select channel definition with the given key.
     */
    ChannelDefinitionQuery channelDefinitionKey(String channelDefinitionKey);

    /**
     * Only select channel definitions where the key matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    ChannelDefinitionQuery channelDefinitionKeyLike(String channelDefinitionKeyLike);

    /**
     * Only select channel definitions where the key matches the given parameter (case-insensitive).
     * The syntax that should be used is the same as in SQL, eg. %test%
     */
    ChannelDefinitionQuery channelDefinitionKeyLikeIgnoreCase(String keyLikeIgnoreCase);
    
    /**
     * Only select channel definitions with a certain version. Particularly useful when used in combination with {@link #channelDefinitionKey(String)}
     */
    ChannelDefinitionQuery channelVersion(Integer channelVersion);

    /**
     * Only select channel definitions which version are greater than a certain version.
     */
    ChannelDefinitionQuery channelVersionGreaterThan(Integer channelVersion);

    /**
     * Only select channel definitions which version are greater than or equals a certain version.
     */
    ChannelDefinitionQuery channelVersionGreaterThanOrEquals(Integer channelVersion);

    /**
     * Only select channel definitions which version are lower than a certain version.
     */
    ChannelDefinitionQuery channelVersionLowerThan(Integer channelVersion);

    /**
     * Only select channel definitions which version are lower than or equals a certain version.
     */
    ChannelDefinitionQuery channelVersionLowerThanOrEquals(Integer channelVersion);

    /**
     * Only select the channel definitions which are the latest deployed (ie. which have the highest version number for the given key).
     * 
     * Can also be used without any other criteria (ie. query.latestVersion().list()), which will then give all the latest versions of all the deployed channel definitions.
     * 
     * @throws FlowableIllegalArgumentException
     *             if used in combination with {{@link #channelVersion(Integer)} or {@link #deploymentId(String)}
     */
    ChannelDefinitionQuery latestVersion();

    /**
     * Only select the inbound channel definitions.
     */
    ChannelDefinitionQuery onlyInbound();

    /**
     * Only select the outbound channel definitions.
     */
    ChannelDefinitionQuery onlyOutbound();

    /**
     * Only select the channel definitions with the given implementation.
     * e.g. jms, rabbit.
     */
    ChannelDefinitionQuery implementation(String implementation);
    
    /**
     * Only select channel definitions where the create time is equal to a certain date.
     */
    ChannelDefinitionQuery channelCreateTime(Date createTime);
    
    /**
     * Only select channel definitions which create time is after a certain date.
     */
    ChannelDefinitionQuery channelCreateTimeAfter(Date createTimeAfter);

    /**
     * Only select channel definitions which create time is before a certain date.
     */
    ChannelDefinitionQuery channelCreateTimeBefore(Date createTimeBefore);

    /** Only select channel definition with the given resource name. */
    ChannelDefinitionQuery channelDefinitionResourceName(String resourceName);

    /** Only select channel definition with a resource name like the given . */
    ChannelDefinitionQuery channelDefinitionResourceNameLike(String resourceNameLike);

    /**
     * Only select channel definitions that have the given tenant id.
     */
    ChannelDefinitionQuery tenantId(String tenantId);

    /**
     * Only select channel definitions with a tenant id like the given one.
     */
    ChannelDefinitionQuery tenantIdLike(String tenantIdLike);

    /**
     * Only select channel definitions that do not have a tenant id.
     */
    ChannelDefinitionQuery withoutTenantId();

    // ordering ////////////////////////////////////////////////////////////

    /**
     * Order by the category of the channel definitions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ChannelDefinitionQuery orderByChannelDefinitionCategory();

    /**
     * Order by channel definition key (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ChannelDefinitionQuery orderByChannelDefinitionKey();

    /**
     * Order by the id of the channel definitions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ChannelDefinitionQuery orderByChannelDefinitionId();

    /**
     * Order by the name of the channel definitions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ChannelDefinitionQuery orderByChannelDefinitionName();

    /**
     * Order by deployment id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ChannelDefinitionQuery orderByDeploymentId();
    
    /**
     * Order by create time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ChannelDefinitionQuery orderByCreateTime();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ChannelDefinitionQuery orderByTenantId();

}

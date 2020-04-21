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

package org.flowable.engine.repository;

import java.util.List;

import org.flowable.common.engine.api.query.Query;
import org.flowable.engine.RepositoryService;

/**
 * Allows programmatic querying of {@link Deployment}s.
 * 
 * Note that it is impossible to retrieve the deployment resources through the results of this operation, since that would cause a huge transfer of (possibly) unneeded bytes over the wire.
 * 
 * To retrieve the actual bytes of a deployment resource use the operations on the {@link RepositoryService#getDeploymentResourceNames(String)} and
 * {@link RepositoryService#getResourceAsStream(String, String)}
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface DeploymentQuery extends Query<DeploymentQuery, Deployment> {

    /**
     * Only select deployments with the given deployment id.
     */
    DeploymentQuery deploymentId(String deploymentId);

    /**
     * Only select deployments with an id in the given set of ids.
     */
    DeploymentQuery deploymentIds(List<String> deploymentId);

    /**
     * Only select deployments with the given name.
     */
    DeploymentQuery deploymentName(String name);

    /**
     * Only select deployments with a name like the given string.
     */
    DeploymentQuery deploymentNameLike(String nameLike);

    /**
     * Only select deployments with the given category.
     * 
     * @see DeploymentBuilder#category(String)
     */
    DeploymentQuery deploymentCategory(String category);

    /**
     * Only select deployments with a category like the given string.
     */
    DeploymentQuery deploymentCategoryLike(String categoryLike);

    /**
     * Only select deployments that have a different category then the given one.
     * 
     * @see DeploymentBuilder#category(String)
     */
    DeploymentQuery deploymentCategoryNotEquals(String categoryNotEquals);

    /**
     * Only select deployments with the given key.
     */
    DeploymentQuery deploymentKey(String key);

    /**
     * Only select deployments with a key like the given string.
     */
    DeploymentQuery deploymentKeyLike(String keyLike);

    /**
     * Only select deployment that have the given tenant id.
     */
    DeploymentQuery deploymentTenantId(String tenantId);

    /**
     * Only select deployments with a tenant id like the given one.
     */
    DeploymentQuery deploymentTenantIdLike(String tenantIdLike);

    /**
     * Only select deployments that do not have a tenant id.
     */
    DeploymentQuery deploymentWithoutTenantId();

    /**
     * Only select deployment that have the given engine version.
     */
    DeploymentQuery deploymentEngineVersion(String engineVersion);
    
    /**
     * Only select deployments that are derived from the given deployment.
     */
    DeploymentQuery deploymentDerivedFrom(String deploymentId);
    
    /**
     * Only select deployments that have the given parent deployment id.
     */
    DeploymentQuery parentDeploymentId(String parentDeploymentId);
    
    /**
     * Only select deployments that have a parent deployment id like the given value.
     */
    DeploymentQuery parentDeploymentIdLike(String parentDeploymentIdLike);
    
    /**
     * Only select deployments with a parent deployment id that is the same as one of the given deployment identifiers.
     */
    DeploymentQuery parentDeploymentIds(List<String> parentDeploymentIds);

    /**
     * Only select deployments with the given process definition key. 
     */
    DeploymentQuery processDefinitionKey(String key);

    /**
     * Only select deployments with a process definition key like the given string.
     */
    DeploymentQuery processDefinitionKeyLike(String keyLike);

    /**
     * Only select deployments where the deployment time is the latest value. Can only be used together with the deployment key.
     */
    DeploymentQuery latest();

    // sorting ////////////////////////////////////////////////////////

    /**
     * Order by deployment id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DeploymentQuery orderByDeploymentId();

    /**
     * Order by deployment name (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DeploymentQuery orderByDeploymentName();

    /**
     * Order by deployment time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DeploymentQuery orderByDeploymentTime();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DeploymentQuery orderByTenantId();
}

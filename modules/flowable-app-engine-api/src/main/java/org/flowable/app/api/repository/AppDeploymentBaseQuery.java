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

package org.flowable.app.api.repository;

import java.util.List;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Tijs Rademakers
 */
public interface AppDeploymentBaseQuery <T extends AppDeploymentBaseQuery<T, C>, C extends AppDeployment> extends Query<T, C> {

    /**
     * Only select deployments with the given deployment id.
     */
    T deploymentId(String deploymentId);
    
    /**
     * Only select deployments with the given deployment ids.
     */
    T deploymentIds(List<String> deploymentIds);

    /**
     * Only select deployments with the given name.
     */
    T deploymentName(String name);

    /**
     * Only select deployments with a name like the given string.
     */
    T deploymentNameLike(String nameLike);

    /**
     * Only select deployments with the given category.
     * 
     * @see AppDeploymentBuilder#category(String)
     */
    T deploymentCategory(String category);

    /**
     * Only select deployments that have a different category then the given one.
     * 
     * @see AppDeploymentBuilder#category(String)
     */
    T deploymentCategoryNotEquals(String categoryNotEquals);
    
    /**
     * Only select deployments with the given key.
     * 
     * @see AppDeploymentBuilder#key(String)
     */
    T deploymentKey(String key);

    /**
     * Only select deployment that have the given tenant id.
     */
    T deploymentTenantId(String tenantId);

    /**
     * Only select deployments with a tenant id like the given one.
     */
    T deploymentTenantIdLike(String tenantIdLike);

    /**
     * Only select deployments that do not have a tenant id.
     */
    T deploymentWithoutTenantId();

    /**
     * Only select deployments where the deployment time is the latest value. Can only be used together with the deployment key.
     */
    T latest();

    // sorting ////////////////////////////////////////////////////////

    /**
     * Order by deployment id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByDeploymentId();

    /**
     * Order by deployment name (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByDeploymentName();

    /**
     * Order by deployment time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByDeploymentTime();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    T orderByTenantId();

}

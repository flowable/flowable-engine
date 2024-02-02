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

package org.flowable.dmn.api;

import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link DmnDecision}s.
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public interface DmnDecisionQuery extends Query<DmnDecisionQuery, DmnDecision> {

    /**
     * Only select decision with the given id.
     */
    DmnDecisionQuery decisionId(String decisionId);

    /**
     * Only select decisions with the given ids.
     */
    DmnDecisionQuery decisionIds(Set<String> decisionIds);

    /**
     * Only select decisions with the given category.
     */
    DmnDecisionQuery decisionCategory(String decisionCategory);

    /**
     * Only select decisions where the category matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    DmnDecisionQuery decisionCategoryLike(String decisionCategoryLike);

    /**
     * Only select deployments that have a different category then the given one.
     *
     * @see DmnDeploymentBuilder#category(String)
     */
    DmnDecisionQuery decisionCategoryNotEquals(String categoryNotEquals);

    /**
     * Only select decisions with the given name.
     */
    DmnDecisionQuery decisionName(String decisionName);

    /**
     * Only select decisions where the name matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    DmnDecisionQuery decisionNameLike(String decisionNameLike);

    /**
     * Only select decisions that are deployed in a deployment with the given deployment id
     */
    DmnDecisionQuery deploymentId(String deploymentId);

    /**
     * Select decisions that are deployed in deployments with the given set of ids
     */
    DmnDecisionQuery deploymentIds(Set<String> deploymentIds);

    /**
     * Only select decisions that are deployed in a deployment with the given parent deployment id
     */
    DmnDecisionQuery parentDeploymentId(String parentDeploymentId);

    /**
     * Only select decision with the given key.
     */
    DmnDecisionQuery decisionKey(String decisionKey);

    /**
     * Only select decisions where the key matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    DmnDecisionQuery decisionKeyLike(String decisionKeyLike);

    /**
     * Only select decisions with a certain version. Particularly useful when used in combination with {@link #decisionKey(String)}
     */
    DmnDecisionQuery decisionVersion(Integer decisionVersion);

    /**
     * Only select decisions which version are greater than a certain version.
     */
    DmnDecisionQuery decisionVersionGreaterThan(Integer decisionVersion);

    /**
     * Only select decisions which version are greater than or equals a certain version.
     */
    DmnDecisionQuery decisionVersionGreaterThanOrEquals(Integer decisionVersion);

    /**
     * Only select decisions which version are lower than a certain version.
     */
    DmnDecisionQuery decisionVersionLowerThan(Integer decisionVersion);

    /**
     * Only select decisions which version are lower than or equals a certain version.
     */
    DmnDecisionQuery decisionVersionLowerThanOrEquals(Integer decisionVersion);

    /**
     * Only select the decisions which are the latest deployed (ie. which have the highest version number for the given key).
     * <p>
     * Can also be used without any other criteria (ie. query.latest().list()), which will then give all the latest versions of all the deployed decisions.
     *
     * @throws FlowableIllegalArgumentException if used in combination with {@link #decisionVersion(Integer)} or {@link #deploymentId(String)}
     */
    DmnDecisionQuery latestVersion();

    /**
     * Only select decision with the given resource name.
     */
    DmnDecisionQuery decisionResourceName(String resourceName);

    /**
     * Only select decision with a resource name like the given .
     */
    DmnDecisionQuery decisionResourceNameLike(String resourceNameLike);

    /**
     * Only select decisions that have the given tenant id.
     */
    DmnDecisionQuery decisionTenantId(String tenantId);

    /**
     * Only select decisions with a tenant id like the given one.
     */
    DmnDecisionQuery decisionTenantIdLike(String tenantIdLike);

    /**
     * Only select decisions that do not have a tenant id.
     */
    DmnDecisionQuery decisionWithoutTenantId();

    /**
     * Only select decisions with the given type.
     */
    DmnDecisionQuery decisionType(String decisionType);

    /**
     * Only select decisions like the given type.
     */
    DmnDecisionQuery decisionTypeLike(String decisionType);

    // ordering ////////////////////////////////////////////////////////////

    /**
     * Order by the category of the decisions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByDecisionCategory();

    /**
     * Order by decision key (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByDecisionKey();

    /**
     * Order by the id of the decisions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByDecisionId();

    /**
     * Order by the version of the decisions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByDecisionVersion();

    /**
     * Order by the name of the decisions (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByDecisionName();

    /**
     * Order by deployment id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByDeploymentId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByTenantId();

    /**
     * Order by decision type (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    DmnDecisionQuery orderByDecisionType();
}

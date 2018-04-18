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

package org.flowable.form.api;

import java.util.Set;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link FormDefinition}s.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface FormDefinitionQuery extends Query<FormDefinitionQuery, FormDefinition> {

    /** Only select form with the given id. */
    FormDefinitionQuery formId(String formId);

    /** Only select forms with the given ids. */
    FormDefinitionQuery formIds(Set<String> formIds);

    /** Only select forms with the given category. */
    FormDefinitionQuery formCategory(String formCategory);

    /**
     * Only select forms where the category matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    FormDefinitionQuery formCategoryLike(String formCategoryLike);

    /**
     * Only select deployments that have a different category then the given one.
     * 
     * @see FormDeploymentBuilder#category(String)
     */
    FormDefinitionQuery formCategoryNotEquals(String categoryNotEquals);

    /** Only select forms with the given name. */
    FormDefinitionQuery formName(String formName);

    /**
     * Only select forms where the name matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    FormDefinitionQuery formNameLike(String formNameLike);

    /**
     * Only select forms that are deployed in a deployment with the given deployment id
     */
    FormDefinitionQuery deploymentId(String deploymentId);

    /**
     * Select forms that are deployed in deployments with the given set of ids
     */
    FormDefinitionQuery deploymentIds(Set<String> deploymentIds);

    /**
     * Only select forms that have the given parent deployment id
     */
    FormDefinitionQuery parentDeploymentId(String parentDeploymentId);

    /**
     * Only select forms that have a parent deployment id like the given parameter
     */
    FormDefinitionQuery parentDeploymentIdLike(String parentDeploymentIdLike);

    /**
     * Only select form with the given key.
     */
    FormDefinitionQuery formDefinitionKey(String formDefinitionKey);

    /**
     * Only select forms where the key matches the given parameter. The syntax that should be used is the same as in SQL, eg. %test%
     */
    FormDefinitionQuery formDefinitionKeyLike(String formDefinitionKeyLike);

    /**
     * Only select form with a certain version. Particularly useful when used in combination with {@link #formDefinitionKey(String)}
     */
    FormDefinitionQuery formVersion(Integer formVersion);

    /**
     * Only select forms which version are greater than a certain version.
     */
    FormDefinitionQuery formVersionGreaterThan(Integer formVersion);

    /**
     * Only select forms which version are greater than or equals a certain version.
     */
    FormDefinitionQuery formVersionGreaterThanOrEquals(Integer formVersion);

    /**
     * Only select forms which version are lower than a certain version.
     */
    FormDefinitionQuery formVersionLowerThan(Integer formVersion);

    /**
     * Only select forms which version are lower than or equals a certain version.
     */
    FormDefinitionQuery formVersionLowerThanOrEquals(Integer formVersion);

    /**
     * Only select the forms which are the latest deployed (ie. which have the highest version number for the given key).
     * 
     * Can also be used without any other criteria (ie. query.latest().list()), which will then give all the latest versions of all the deployed decision tables.
     * 
     * @throws FlowableIllegalArgumentException
     *             if used in combination with {@link #groupId(string)}, {@link #formVersion(int)} or {@link #deploymentId(String)}
     */
    FormDefinitionQuery latestVersion();

    /** Only select form with the given resource name. */
    FormDefinitionQuery formResourceName(String resourceName);

    /** Only select form with a resource name like the given . */
    FormDefinitionQuery formResourceNameLike(String resourceNameLike);

    /**
     * Only select forms that have the given tenant id.
     */
    FormDefinitionQuery formTenantId(String tenantId);

    /**
     * Only select forms with a tenant id like the given one.
     */
    FormDefinitionQuery formTenantIdLike(String tenantIdLike);

    /**
     * Only select forms that do not have a tenant id.
     */
    FormDefinitionQuery formWithoutTenantId();

    // ordering ////////////////////////////////////////////////////////////

    /**
     * Order by the category of the forms (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormDefinitionQuery orderByFormCategory();

    /**
     * Order by form definition key (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormDefinitionQuery orderByFormDefinitionKey();

    /**
     * Order by the id of the forms (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormDefinitionQuery orderByFormId();

    /**
     * Order by the version of the forms (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormDefinitionQuery orderByFormVersion();

    /**
     * Order by the name of the forms (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormDefinitionQuery orderByFormName();

    /**
     * Order by deployment id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormDefinitionQuery orderByDeploymentId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    FormDefinitionQuery orderByTenantId();

}

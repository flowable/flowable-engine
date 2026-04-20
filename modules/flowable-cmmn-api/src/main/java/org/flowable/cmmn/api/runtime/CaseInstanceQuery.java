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
package org.flowable.cmmn.api.runtime;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface CaseInstanceQuery extends Query<CaseInstanceQuery, CaseInstance> {

    CaseInstanceQuery caseDefinitionKey(String caseDefinitionKey);
    CaseInstanceQuery caseDefinitionKeyLike(String caseDefinitionKeyLike);
    CaseInstanceQuery caseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase);
    CaseInstanceQuery caseDefinitionKeys(Set<String> caseDefinitionKeys);
    CaseInstanceQuery excludeCaseDefinitionKeys(Set<String> caseDefinitionKeys);
    CaseInstanceQuery caseDefinitionId(String caseDefinitionId);
    CaseInstanceQuery caseDefinitionIds(Set<String> caseDefinitionIds);
    CaseInstanceQuery caseDefinitionCategory(String caseDefinitionCategory);
    CaseInstanceQuery caseDefinitionCategoryLike(String caseDefinitionCategoryLike);
    CaseInstanceQuery caseDefinitionCategoryLikeIgnoreCase(String caseDefinitionCategoryLikeIgnoreCase);
    CaseInstanceQuery caseDefinitionName(String caseDefinitionName);
    CaseInstanceQuery caseDefinitionNameLike(String caseDefinitionNameLike);
    CaseInstanceQuery caseDefinitionNameLikeIgnoreCase(String caseDefinitionNameLikeIgnoreCase);
    CaseInstanceQuery caseDefinitionVersion(Integer caseDefinitionVersion);
    CaseInstanceQuery caseInstanceId(String caseInstanceId);
    CaseInstanceQuery caseInstanceIds(Set<String> caseInstanceIds);
    CaseInstanceQuery caseInstanceName(String caseInstanceName);
    CaseInstanceQuery caseInstanceNameLike(String caseInstanceNameLike);
    CaseInstanceQuery caseInstanceNameLikeIgnoreCase(String caseInstanceNameLikeIgnoreCase);
    CaseInstanceQuery caseInstanceRootScopeId(String rootScopeId);
    CaseInstanceQuery caseInstanceParentScopeId(String parentScopeId);
    CaseInstanceQuery caseInstanceBusinessKey(String caseInstanceBusinessKey);
    CaseInstanceQuery caseInstanceBusinessKeyLike(String caseInstanceBusinessKeyLike);
    CaseInstanceQuery caseInstanceBusinessKeyLikeIgnoreCase(String caseInstanceBusinessKeyLikeIgnoreCase);
    CaseInstanceQuery caseInstanceBusinessStatus(String caseInstanceBusinessStatus);
    CaseInstanceQuery caseInstanceBusinessStatusLike(String caseInstanceBusinessStatusLike);
    CaseInstanceQuery caseInstanceBusinessStatusLikeIgnoreCase(String caseInstanceBusinessStatusLikeIgnoreCase);
    CaseInstanceQuery caseInstanceParentId(String parentId);
    CaseInstanceQuery caseInstanceStartedBefore(Date beforeTime);
    CaseInstanceQuery caseInstanceStartedAfter(Date afterTime);
    CaseInstanceQuery caseInstanceStartedBy(String userId);
    CaseInstanceQuery caseInstanceState(String state);
    CaseInstanceQuery caseInstanceLastReactivatedBefore(Date beforeTime);
    CaseInstanceQuery caseInstanceLastReactivatedAfter(Date afterTime);
    CaseInstanceQuery caseInstanceLastReactivatedBy(String userId);
    CaseInstanceQuery caseInstanceCallbackId(String callbackId);
    CaseInstanceQuery caseInstanceCallbackIds(Set<String> callbackIds);
    CaseInstanceQuery caseInstanceCallbackType(String callbackType);
    CaseInstanceQuery parentCaseInstanceId(String parentCaseInstanceId);
    CaseInstanceQuery caseInstanceReferenceId(String referenceId);
    CaseInstanceQuery caseInstanceReferenceType(String referenceType);
    CaseInstanceQuery caseInstanceIsCompleteable();
    CaseInstanceQuery caseInstanceTenantId(String tenantId);
    CaseInstanceQuery caseInstanceTenantIdLike(String tenantIdLike);
    CaseInstanceQuery caseInstanceTenantIdLikeIgnoreCase(String tenantIdLikeIgnoreCase);
    CaseInstanceQuery caseInstanceWithoutTenantId();
    
    /**
     * Select the case instances with an active plan item definition id equal to the provided definition id.
     */
    CaseInstanceQuery activePlanItemDefinitionId(String planItemDefinitionId);
    
    /**
     * Select the case instances with an active plan item definition id equal to one of the provided definition ids.
     */
    CaseInstanceQuery activePlanItemDefinitionIds(Set<String> planItemDefinitionIds);

    /**
     * Select the case instances with which the user with the given id is involved.
     */
    CaseInstanceQuery involvedUser(String userId);
    
    /**
     * Select the case instances with which the user with the given id and identity link type are involved.
     */
    CaseInstanceQuery involvedUser(String userId, String identityLinkType);
    
    /**
     * Select the case instances with which the group with the given id and identity link type are involved.
     */
    CaseInstanceQuery involvedGroup(String groupId, String identityLinkType);

    /**
     * Select the case instances with which the groups with the given ids are involved.
     */
    CaseInstanceQuery involvedGroups(Set<String> groupIds);

    /**
     * Only select case instances which have a global variable with the given value. The type of variable is determined based on the value.
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     *
     * @param name
     *            name of the variable, cannot be null.
     */
    CaseInstanceQuery variableValueEquals(String name, Object value);

    /**
     * Only select case instances which have at least one global variable with the given value. The type of variable is determined based on the value.
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     */
    CaseInstanceQuery variableValueEquals(Object value);

    /**
     * Only select case instances which have a local string variable with the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     *
     * @param name
     *            name of the variable, cannot be null.
     * @param value
     *            value of the variable, cannot be null.
     */
    CaseInstanceQuery variableValueEqualsIgnoreCase(String name, String value);

    /**
     * Only select case instances which have a global variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not
     * primitive type wrappers) are not supported.
     *
     * @param name
     *            name of the variable, cannot be null.
     */
    CaseInstanceQuery variableValueNotEquals(String name, Object value);

    /**
     * Only select case instances which have a local string variable which is not the given value, case insensitive.
     * <p>
     * This method only works if your database has encoding/collation that supports case-sensitive queries. For example, use "collate UTF-8" on MySQL and for MSSQL, select one of the case-sensitive
     * Collations available (<a href="http://msdn.microsoft.com/en-us/library/ms144250(v=sql.105).aspx" >MSDN Server Collation Reference</a>).
     * </p>
     *
     * @param name
     *            name of the variable, cannot be null.
     * @param value
     *            value of the variable, cannot be null.
     */
    CaseInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value);

    /**
     * Only select case instances which have a variable value greater than the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not
     * supported.
     *
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    CaseInstanceQuery variableValueGreaterThan(String name, Object value);

    /**
     * Only select case instances which have a global variable value greater than or equal to the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     *
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    CaseInstanceQuery variableValueGreaterThanOrEqual(String name, Object value);

    /**
     * Only select case instances which have a global variable value less than the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
     * not supported.
     *
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    CaseInstanceQuery variableValueLessThan(String name, Object value);

    /**
     * Only select case instances which have a global variable value less than or equal to the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     *
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    CaseInstanceQuery variableValueLessThanOrEqual(String name, Object value);

    /**
     * Only select case instances which have a global variable value like the given value. This be used on string variables only.
     *
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    CaseInstanceQuery variableValueLike(String name, String value);

    /**
     * Only select case instances which have a global variable value like the given value (case insensitive). This be used on string variables only.
     *
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    CaseInstanceQuery variableValueLikeIgnoreCase(String name, String value);

    /**
     * Only select case instances which have a variable with the given name.
     *
     * @param name
     *            cannot be null.
     */
    CaseInstanceQuery variableExists(String name);

    /**
     * Only select case instances which don't have a variable with the given name.
     *
     * @param name
     *            cannot be null.
     */
    CaseInstanceQuery variableNotExists(String name);

    /**
     * Includes case variables into the query result.
     *
     * @return caseInstanceQuery with the flag to retrieve case variables into the response.
     */
    CaseInstanceQuery includeCaseVariables();

    /**
     * Include the case variables with the given names into the query result.
     */
    CaseInstanceQuery includeCaseVariables(Collection<String> variableNames);

    /**
     * Begin an OR statement. Make sure you invoke the endOr method at the end of your OR statement.
     */
    CaseInstanceQuery or();

    /**
     * End an OR statement.
     */
    CaseInstanceQuery endOr();

    /**
     * Localize case name to specified locale.
     */
    CaseInstanceQuery locale(String locale);

    /**
     * Instruct localization to fallback to more general locales including the default locale of the JVM if the specified locale is not found.
     */
    CaseInstanceQuery withLocalizationFallback();

    CaseInstanceQuery orderByCaseInstanceId();
    CaseInstanceQuery orderByCaseDefinitionKey();
    CaseInstanceQuery orderByCaseDefinitionId();
    CaseInstanceQuery orderByStartTime();
    CaseInstanceQuery orderByTenantId();

}

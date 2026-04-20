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
package org.flowable.cmmn.api.history;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.query.BatchDeleteQuery;
import org.flowable.common.engine.api.query.DeleteQuery;
import org.flowable.common.engine.api.query.Query;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface HistoricCaseInstanceQuery extends Query<HistoricCaseInstanceQuery, HistoricCaseInstance>, DeleteQuery<HistoricCaseInstanceQuery, HistoricCaseInstance>,
        BatchDeleteQuery<HistoricCaseInstanceQuery> {

    /**
     * Only select historic case instances with the given identifier.
     */
    HistoricCaseInstanceQuery caseInstanceId(String caseInstanceId);
    
    /**
     * Only select historic case instances with one the given identifiers.
     */
    HistoricCaseInstanceQuery caseInstanceIds(Set<String> caseInstanceIds);
    
    /**
     * Only select historic case instances with the given name.
     */
    HistoricCaseInstanceQuery caseInstanceName(String caseInstanceName);
    
    /**
     * Only select historic case instances like the given name.
     */
    HistoricCaseInstanceQuery caseInstanceNameLike(String caseInstanceNameLike);
    
    /**
     * Only select case instances that have a name like (case insensitive) the given name.
     *
     * @param nameLikeIgnoreCase
     *          cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    HistoricCaseInstanceQuery caseInstanceNameLikeIgnoreCase(String nameLikeIgnoreCase);
    
    /**
     * Only select historic case instances with the given business key.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessKey(String caseInstanceBusinessKey);
    
    /**
     * Only select historic case instances with a business key like the given value.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessKeyLike(String caseInstanceBusinessKeyLike);
    
    /**
     * Only select historic case instances with a business key like the given value, ignoring upper/lower case.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessKeyLikeIgnoreCase(String caseInstanceBusinessKeyLikeIgnoreCase);

    /**
     * Only select historic case instances with the given case instance root scope id.
     */
    HistoricCaseInstanceQuery caseInstanceRootScopeId(String rootScopeId);

    /**
     * Only select historic case instances with the given case instance parent scope id.
     */
    HistoricCaseInstanceQuery caseInstanceParentScopeId(String parentScopeId);
    
    /**
     * Only select historic case instances with the given business status.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessStatus(String caseInstanceBusinessStatus);
    
    /**
     * Only select historic case instances with a business status like the given value.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessStatusLike(String caseInstanceBusinessStatusLike);
    
    /**
     * Only select historic case instances with a business status like the given value, ignoring upper/lower case.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessStatusLikeIgnoreCase(String caseInstanceBusinessStatusLikeIgnoreCase);
    
    /**
     * Only select historic case instances with the parent identifier.
     */
    HistoricCaseInstanceQuery caseInstanceParentId(String parentId);

    /**
     * Only select historic case instances without a parent identifier.
     */
    HistoricCaseInstanceQuery withoutCaseInstanceParent();
    
    /**
     * Only select historic case instances with the given key.
     */
    HistoricCaseInstanceQuery caseDefinitionKey(String caseDefinitionKey);
    
    /**
     * Only select historic case instances with a definition key like the given value.
     */
    HistoricCaseInstanceQuery caseDefinitionKeyLike(String caseDefinitionKeyLike);
    
    /**
     * Only select historic case instances with a definition key like the given value, ignoring upper/lower case.
     */
    HistoricCaseInstanceQuery caseDefinitionKeyLikeIgnoreCase(String caseDefinitionKeyLikeIgnoreCase);
    
    /**
     * Only select historic case instances with the given keys.
     */
    HistoricCaseInstanceQuery caseDefinitionKeys(Set<String> caseDefinitionKeys);
    
    /**
     * Only select historic case instances that don't match the given case definition keys.
     */
    HistoricCaseInstanceQuery excludeCaseDefinitionKeys(Set<String> caseDefinitionKeys);
    
    /**
     * Only select historic case instances with the given case definition identifier.
     */
    HistoricCaseInstanceQuery caseDefinitionId(String caseDefinitionId);

    /**
     * Only select historic case instances with the given case definition ids.
     */
    HistoricCaseInstanceQuery caseDefinitionIds(Set<String> caseDefinitionIds);
    
    /**
     * Only select historic case instances with the given case definition category.
     */
    HistoricCaseInstanceQuery caseDefinitionCategory(String caseDefinitionCategory);
    
    /**
     * Only select historic case instances with a case definition category like the given value.
     */
    HistoricCaseInstanceQuery caseDefinitionCategoryLike(String caseDefinitionCategoryLike);
    
    /**
     * Only select historic case instances with a case definition category like the given value, ignoring upper/lower case.
     */
    HistoricCaseInstanceQuery caseDefinitionCategoryLikeIgnoreCase(String caseDefinitionCategoryLikeIgnoreCase);
    
    /**
     * Only select historic case instances with the given case definition name.
     */
    HistoricCaseInstanceQuery caseDefinitionName(String caseDefinitionName);
    
    /**
     * Only select historic case instances with a case definition name like the given value.
     */
    HistoricCaseInstanceQuery caseDefinitionNameLike(String caseDefinitionNameLike);
    
    /**
     * Only select historic case instances with a case definition name like the given value, ignoring upper/lower case.
     */
    HistoricCaseInstanceQuery caseDefinitionNameLikeIgnoreCase(String caseDefinitionNameLikeIgnoreCase);
    
    /**
     * Only select historic case instances with the given case definition version.
     */
    HistoricCaseInstanceQuery caseDefinitionVersion(Integer caseDefinitionVersion);

    /**
     * Include historic case variables in the historic case query result
     */
    HistoricCaseInstanceQuery includeCaseVariables();

    /**
     * Include the historic case variables with the given names into the query result.
     */
    HistoricCaseInstanceQuery includeCaseVariables(Collection<String> variableNames);

    /**
     * Only select historic case instances that are defined by a case definition with the given deployment identifier.
     */
    HistoricCaseInstanceQuery deploymentId(String deploymentId);

    /**
     * Only select historic case instances that are defined by a case definition with one of the given deployment identifiers.
     */
    HistoricCaseInstanceQuery deploymentIds(List<String> deploymentIds);
    
    /**
     * Only select historic case instances that are finished.
     */
    HistoricCaseInstanceQuery finished();
    
    /**
     * Only select historic case instances that are not finished.
     */
    HistoricCaseInstanceQuery unfinished();
    
    /**
     * Only select historic case instances that are started before the provided date time.
     */
    HistoricCaseInstanceQuery startedBefore(Date beforeTime);
    
    /**
     * Only select historic case instances that are started after the provided date time.
     */
    HistoricCaseInstanceQuery startedAfter(Date afterTime);
    
    /**
     * Only select historic case instances that are finished before the provided date time.
     */
    HistoricCaseInstanceQuery finishedBefore(Date beforeTime);
    
    /**
     * Only select historic case instances that are finished after the provided date time.
     */
    HistoricCaseInstanceQuery finishedAfter(Date afterTime);
    
    /**
     * Only select historic case instances that are started by the provided user identifier.
     */
    HistoricCaseInstanceQuery startedBy(String userId);

    /**
     * Only select historic case instances that are ended by the provided user identifier.
     */
    HistoricCaseInstanceQuery finishedBy(String userId);
    
    /**
     * Only select historic case instances that have a state that is equal to the provided value.
     */
    HistoricCaseInstanceQuery state(String state);

    /**
     * Only select historic case instance that are reactivated before the provided date time.
     */
    HistoricCaseInstanceQuery lastReactivatedBefore(Date beforeTime);

    /**
     * Only select historic case instance that are reactivated after the provided date time.
     */
    HistoricCaseInstanceQuery lastReactivatedAfter(Date afterTime);

    /**
     * Only select historic case instances that are reactivated by the provided user identifier.
     */
    HistoricCaseInstanceQuery lastReactivatedBy(String userId);
    
    /**
     * Only select historic case instances that have the provided callback identifier.
     */
    HistoricCaseInstanceQuery caseInstanceCallbackId(String callbackId);

    /**
     * Only select historic case instances that have the provided callback identifiers.
     */
    HistoricCaseInstanceQuery caseInstanceCallbackIds(Set<String> callbackId);
    
    /**
     * Only select historic case instances that have the provided callback type.
     */
    HistoricCaseInstanceQuery caseInstanceCallbackType(String callbackType);
    
    /**
     * Only select historic case instances that have a parent case instance with the provided id.
     */
    HistoricCaseInstanceQuery parentCaseInstanceId(String parentCaseInstanceId);

    /**
     * Only select historic case instances that do not have a callback identifier.
     */
    HistoricCaseInstanceQuery withoutCaseInstanceCallbackId();

    /**
     * Only select historic case instance that have the provided reference identifier.
     */
    HistoricCaseInstanceQuery caseInstanceReferenceId(String referenceId);

    /**
     * Only select historic case instance that have the provided reference type.
     */
    HistoricCaseInstanceQuery caseInstanceReferenceType(String referenceType);
    
    /**
     * Only select historic case instances that have the tenant identifier.
     */
    HistoricCaseInstanceQuery caseInstanceTenantId(String tenantId);
    
    /**
     * Only select historic case instances with a tenant identifier like the given value.
     */
    HistoricCaseInstanceQuery caseInstanceTenantIdLike(String tenantIdLike);
    
    /**
     * Only select historic case instances with a tenant identifier like the given value, ignoring upper/lower case.
     */
    HistoricCaseInstanceQuery caseInstanceTenantIdLikeIgnoreCase(String tenantIdLikeIgnoreCase);
    
    /**
     * Only select historic case instances that have no tenant identifier.
     */
    HistoricCaseInstanceQuery caseInstanceWithoutTenantId();

    /**
     * Begin an OR statement. Make sure you invoke the endOr method at the end of your OR statement. Only one OR statement is allowed, for the second call to this method an exception will be thrown.
     */
    HistoricCaseInstanceQuery or();

    /**
     * End an OR statement. Only one OR statement is allowed, for the second call to this method an exception will be thrown.
     */
    HistoricCaseInstanceQuery endOr();
    
    /**
     * Select the historic case instances with an active plan item definition id equal to the provided definition id.
     */
    HistoricCaseInstanceQuery activePlanItemDefinitionId(String planItemDefinitionId);
    
    /**
     * Select the historic case instances with an active plan item definition id equal to one of the provided definition ids.
     */
    HistoricCaseInstanceQuery activePlanItemDefinitionIds(Set<String> planItemDefinitionIds);

    /**
     * Select the historic case instances with which the user with the given id is involved.
     */
    HistoricCaseInstanceQuery involvedUser(String userId);
    
    /**
     * Select the historic case instances with which the user with the given id and identity link type are involved.
     */
    HistoricCaseInstanceQuery involvedUser(String userId, String identityLinkType);
    
    /**
     * Select the historic case instances with which the group with the given id and identity link type are involved.
     */
    HistoricCaseInstanceQuery involvedGroup(String groupId, String identityLinkType);

    /**
     * Select the historic case instances with which the groups with the given ids are involved.
     */
    HistoricCaseInstanceQuery involvedGroups(Set<String> groupIds);

    /**
     * Only select case instances which had a global variable with the given value when they ended. 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     * 
     * @param name
     *            of the variable, cannot be null.
     */
    HistoricCaseInstanceQuery variableValueEquals(String name, Object value);

    /**
     * Only select case instances which had at least one global variable with the given value when they ended. 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     */
    HistoricCaseInstanceQuery variableValueEquals(Object value);

    /**
     * Only select historic case instances which have a string variable with the given value, case insensitive.
     * 
     * @param name
     *            name of the variable, cannot be null.
     * @param value
     *            value of the variable, cannot be null.
     */
    HistoricCaseInstanceQuery variableValueEqualsIgnoreCase(String name, String value);

    /**
     * Only select case instances which had a global variable with the given name, but with a different value than the passed value when they ended. 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     * 
     * @param name
     *            of the variable, cannot be null.
     */
    HistoricCaseInstanceQuery variableValueNotEquals(String name, Object value);

    /**
     * Only select case instances which had a global variable value greater than the passed value when they ended. Booleans, 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    HistoricCaseInstanceQuery variableValueGreaterThan(String name, Object value);

    /**
     * Only select case instances which had a global variable value greater than or equal to the passed value when they ended. Booleans, Byte-arrays and {@link Serializable} objects (which are not
     * primitive type wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    HistoricCaseInstanceQuery variableValueGreaterThanOrEqual(String name, Object value);

    /**
     * Only select case instances which had a global variable value less than the passed value when the ended. 
     * Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    HistoricCaseInstanceQuery variableValueLessThan(String name, Object value);

    /**
     * Only select case instances which has a global variable value less than or equal to the passed value when they ended. 
     * Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null.
     */
    HistoricCaseInstanceQuery variableValueLessThanOrEqual(String name, Object value);

    /**
     * Only select case instances which had global variable value like the given value when they ended. This can be used on string variables only.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    HistoricCaseInstanceQuery variableValueLike(String name, String value);

    /**
     * Only select case instances which had global variable value like (case insensitive) the given value when they ended. This can be used on string variables only.
     * 
     * @param name
     *            cannot be null.
     * @param value
     *            cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    HistoricCaseInstanceQuery variableValueLikeIgnoreCase(String name, String value);
    
    /**
     * Only select case instances which have a variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    HistoricCaseInstanceQuery variableExists(String name);
    
    /**
     * Only select case instances which does not have a variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    HistoricCaseInstanceQuery variableNotExists(String name);
    

    /**
     * Localize case name to specified locale.
     */
    HistoricCaseInstanceQuery locale(String locale);

    /**
     * Instruct localization to fallback to more general locales including the default locale of the JVM if the specified locale is not found.
     */
    HistoricCaseInstanceQuery withLocalizationFallback();
    
    /**
     * Perform the query without applying sorting parameters. By default sorting will be applied.
     */
    HistoricCaseInstanceQuery withoutSorting();
    
    /**
     * Return only the id value of the case instances, to reduce any additional instance data to be returned.
     */
    HistoricCaseInstanceQuery returnIdsOnly();

    HistoricCaseInstanceQuery orderByCaseInstanceId();
    HistoricCaseInstanceQuery orderByCaseInstanceName();
    HistoricCaseInstanceQuery orderByCaseDefinitionKey();
    HistoricCaseInstanceQuery orderByCaseDefinitionId();
    HistoricCaseInstanceQuery orderByStartTime();
    HistoricCaseInstanceQuery orderByEndTime();
    HistoricCaseInstanceQuery orderByTenantId();
    
}

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
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface HistoricCaseInstanceQuery extends Query<HistoricCaseInstanceQuery, HistoricCaseInstance> {

    /**
     * Only select historic case instances with the given identifier.
     */
    HistoricCaseInstanceQuery caseInstanceId(String caseInstanceId);
    
    /**
     * Only select historic case instances with one the given identifiers.
     */
    HistoricCaseInstanceQuery caseInstanceIds(Set<String> caseInstanceIds);
    
    /**
     * Only select historic case instances with the given business key.
     */
    HistoricCaseInstanceQuery caseInstanceBusinessKey(String caseInstanceBusinessKey);
    
    /**
     * Only select historic case instances with the parent identifier.
     */
    HistoricCaseInstanceQuery caseInstanceParentId(String parentId);
    
    /**
     * Only select historic case instances with the given key.
     */
    HistoricCaseInstanceQuery caseDefinitionKey(String caseDefinitionKey);
    
    /**
     * Only select historic case instances with the given keys.
     */
    HistoricCaseInstanceQuery caseDefinitionKeys(Set<String> caseDefinitionKeys);
    
    /**
     * Only select historic case instances with the given case definition identifier.
     */
    HistoricCaseInstanceQuery caseDefinitionId(String caseDefinitionId);
    
    /**
     * Only select historic case instances with the given case definition category.
     */
    HistoricCaseInstanceQuery caseDefinitionCategory(String caseDefinitionCategory);
    
    /**
     * Only select historic case instances with the given case definition name.
     */
    HistoricCaseInstanceQuery caseDefinitionName(String caseDefinitionName);
    
    /**
     * Only select historic case instances with the given case definition version.
     */
    HistoricCaseInstanceQuery caseDefinitionVersion(Integer caseDefinitionVersion);

    /**
     * Include historic case variables in the historic case query result
     */
    HistoricCaseInstanceQuery includeCaseVariables();

    /**
     * Limit historic case instance variables
     */
    HistoricCaseInstanceQuery limitCaseVariables(Integer historicCaseVariablesLimit);

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
     * Only select historic case instances that have the provided callback identifier.
     */
    HistoricCaseInstanceQuery caseInstanceCallbackId(String callbackId);
    
    /**
     * Only select historic case instances that have the provided callback type.
     */
    HistoricCaseInstanceQuery caseInstanceCallbackType(String callbackType);
    
    /**
     * Only select historic case instances that have the tenant identifier.
     */
    HistoricCaseInstanceQuery caseInstanceTenantId(String tenantId);
    
    /**
     * Only select historic case instances that have no tenant identifier.
     */
    HistoricCaseInstanceQuery caseInstanceWithoutTenantId();
    
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
    
    HistoricCaseInstanceQuery orderByCaseInstanceId();
    HistoricCaseInstanceQuery orderByCaseDefinitionKey();
    HistoricCaseInstanceQuery orderByCaseDefinitionId();
    HistoricCaseInstanceQuery orderByStartTime();
    HistoricCaseInstanceQuery orderByEndTime();
    HistoricCaseInstanceQuery orderByTenantId();
    
}

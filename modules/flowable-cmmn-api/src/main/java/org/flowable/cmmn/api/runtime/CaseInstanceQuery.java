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
import java.util.Date;
import java.util.Set;

import org.flowable.engine.common.api.query.Query;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface CaseInstanceQuery extends Query<CaseInstanceQuery, CaseInstance> {

    CaseInstanceQuery caseDefinitionKey(String caseDefinitionKey);
    CaseInstanceQuery caseDefinitionKeys(Set<String> caseDefinitionKeys);
    CaseInstanceQuery caseDefinitionId(String caseDefinitionId);
    CaseInstanceQuery caseDefinitionCategory(String caseDefinitionCategory);
    CaseInstanceQuery caseDefinitionName(String caseDefinitionName);
    CaseInstanceQuery caseDefinitionVersion(Integer caseDefinitionVersion);
    CaseInstanceQuery caseInstanceId(String caseInstanceId);
    CaseInstanceQuery caseInstanceIds(Set<String> caseInstanceIds);
    CaseInstanceQuery caseInstanceBusinessKey(String caseInstanceBusinessKey);
    CaseInstanceQuery caseInstanceParentId(String parentId);
    CaseInstanceQuery caseInstanceStartedBefore(Date beforeTime);
    CaseInstanceQuery caseInstanceStartedAfter(Date afterTime);
    CaseInstanceQuery caseInstanceStartedBy(String userId);
    CaseInstanceQuery caseInstanceCallbackId(String callbackId);
    CaseInstanceQuery caseInstanceCallbackType(String callbackType);
    CaseInstanceQuery caseInstanceIsCompleteable();
    CaseInstanceQuery caseInstanceTenantId(String tenantId);
    CaseInstanceQuery caseInstanceTenantIdLike(String tenantIdLike);
    CaseInstanceQuery caseInstanceWithoutTenantId();

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
     * Limit case instance variables
     */
    CaseInstanceQuery limitCaseInstanceVariables(Integer caseInstanceVariablesLimit);


    CaseInstanceQuery orderByCaseInstanceId();
    CaseInstanceQuery orderByCaseDefinitionKey();
    CaseInstanceQuery orderByCaseDefinitionId();
    CaseInstanceQuery orderByStartTime();
    CaseInstanceQuery orderByTenantId();

}

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

import org.flowable.common.engine.api.query.Query;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface PlanItemInstanceQuery extends Query<PlanItemInstanceQuery, PlanItemInstance> {

    PlanItemInstanceQuery caseDefinitionId(String caseDefinitionId);
    PlanItemInstanceQuery caseInstanceId(String caseInstanceId);
    PlanItemInstanceQuery stageInstanceId(String stageInstanceId);
    PlanItemInstanceQuery planItemInstanceId(String planItemInstanceId);
    PlanItemInstanceQuery planItemInstanceElementId(String elementId);
    PlanItemInstanceQuery planItemInstanceName(String name);
    PlanItemInstanceQuery planItemInstanceState(String state);
    PlanItemInstanceQuery planItemInstanceStateWaitingForRepetition();
    PlanItemInstanceQuery planItemInstanceStateEnabled();
    PlanItemInstanceQuery planItemInstanceStateDisabled();
    PlanItemInstanceQuery planItemInstanceStateActive();
    PlanItemInstanceQuery planItemInstanceStateAsyncActive();
    PlanItemInstanceQuery planItemInstanceStateAvailable();
    PlanItemInstanceQuery planItemInstanceStateCompleted();
    PlanItemInstanceQuery planItemInstanceStateTerminated();
    PlanItemInstanceQuery planItemInstanceStartedBefore(Date startedBefore);
    PlanItemInstanceQuery planItemInstanceStartedAfter(Date startedAfer);
    PlanItemInstanceQuery planItemInstanceStartUserId(String startUserId);
    PlanItemInstanceQuery planItemInstanceReferenceId(String referenceId);
    PlanItemInstanceQuery planItemInstanceReferenceType(String referenceType);
    PlanItemInstanceQuery planItemCompleteable();
    PlanItemInstanceQuery planItemInstanceTenantId(String tenantId);
    PlanItemInstanceQuery planItemInstanceWithoutTenantId();
    
    PlanItemInstanceQuery planItemDefinitionId(String planItemDefinitionId);
    PlanItemInstanceQuery planItemDefinitionType(String planItemDefinitionType);
    
    /**
     * Only select plan item instances which have a local variable with the given value. The type of variable is determined based on the value. 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     * 
     * @param name
     *            name of the variable, cannot be null.
     */
    PlanItemInstanceQuery variableValueEquals(String name, Object value);

    /**
     * Only select plan item instances which have at least one local variable with the given value. The type of variable is determined based on the value. 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     */
    PlanItemInstanceQuery variableValueEquals(Object value);

    /**
     * Only select plan item instances which have a local string variable with the given value, case insensitive.
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
    PlanItemInstanceQuery variableValueEqualsIgnoreCase(String name, String value);

    /**
     * Only select plan item instances which have a local variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not
     * primitive type wrappers) are not supported.
     * 
     * @param name
     *            name of the variable, cannot be null.
     */
    PlanItemInstanceQuery variableValueNotEquals(String name, Object value);

    /**
     * Only select plan item instances which have a local string variable which is not the given value, case insensitive.
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
    PlanItemInstanceQuery variableValueNotEqualsIgnoreCase(String name, String value);

    /**
     * Only select plan item instances which have a local variable value greater than the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not
     * supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery variableValueGreaterThan(String name, Object value);

    /**
     * Only select plan item instances which have a local variable value greater than or equal to the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery variableValueGreaterThanOrEqual(String name, Object value);

    /**
     * Only select plan item instances which have a local variable value less than the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
     * not supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery variableValueLessThan(String name, Object value);

    /**
     * Only select plan item instances which have a local variable value less than or equal to the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery variableValueLessThanOrEqual(String name, Object value);

    /**
     * Only select plan item instances which have a local variable value like the given value. This be used on string variables only.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    PlanItemInstanceQuery variableValueLike(String name, String value);

    /**
     * Only select plan item instances which have a local variable value like the given value (case insensitive). This be used on string variables only.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    PlanItemInstanceQuery variableValueLikeIgnoreCase(String name, String value);
    
    /**
     * Only select plan item instances which have a local variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    PlanItemInstanceQuery variableExists(String name);
    
    /**
     * Only select plan item instances which don't have a local variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    PlanItemInstanceQuery variableNotExists(String name);
    
    /**
     * Only select plan item instances which have a global variable with the given value. The type of variable is determined based on the value. 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     * 
     * @param name
     *            name of the variable, cannot be null.
     */
    PlanItemInstanceQuery caseVariableValueEquals(String name, Object value);

    /**
     * Only select plan item instances which have at least one global variable with the given value. The type of variable is determined based on the value. 
     * Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not supported.
     */
    PlanItemInstanceQuery caseVariableValueEquals(Object value);

    /**
     * Only select plan item instances which have a global string variable with the given value, case insensitive.
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
    PlanItemInstanceQuery caseVariableValueEqualsIgnoreCase(String name, String value);

    /**
     * Only select plan item instances which have a global variable with the given name, but with a different value than the passed value. Byte-arrays and {@link Serializable} objects (which are not
     * primitive type wrappers) are not supported.
     * 
     * @param name
     *            name of the variable, cannot be null.
     */
    PlanItemInstanceQuery caseVariableValueNotEquals(String name, Object value);

    /**
     * Only select plan item instances which have a global string variable which is not the given value, case insensitive.
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
    PlanItemInstanceQuery caseVariableValueNotEqualsIgnoreCase(String name, String value);

    /**
     * Only select plan item instances which have a global variable value greater than the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are not
     * supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery caseVariableValueGreaterThan(String name, Object value);

    /**
     * Only select plan item instances which have a global variable value greater than or equal to the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery caseVariableValueGreaterThanOrEqual(String name, Object value);

    /**
     * Only select plan item instances which have a global variable value less than the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type wrappers) are
     * not supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery caseVariableValueLessThan(String name, Object value);

    /**
     * Only select plan item instances which have a global variable value less than or equal to the passed value. Booleans, Byte-arrays and {@link Serializable} objects (which are not primitive type
     * wrappers) are not supported.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null.
     */
    PlanItemInstanceQuery caseVariableValueLessThanOrEqual(String name, Object value);

    /**
     * Only select plan item instances which have a global variable value like the given value. This be used on string variables only.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    PlanItemInstanceQuery caseVariableValueLike(String name, String value);

    /**
     * Only select plan item instances which have a global variable value like the given value (case insensitive). This be used on string variables only.
     * 
     * @param name
     *            variable name, cannot be null.
     * @param value
     *            variable value, cannot be null. The string can include the wildcard character '%' to express like-strategy: starts with (string%), ends with (%string) or contains (%string%).
     */
    PlanItemInstanceQuery caseVariableValueLikeIgnoreCase(String name, String value);
    
    /**
     * Only select plan item instances which have a global variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    PlanItemInstanceQuery caseVariableExists(String name);
    
    /**
     * Only select plan item instances which don't have a global variable with the given name.
     * 
     * @param name
     *            cannot be null.
     */
    PlanItemInstanceQuery caseVariableNotExists(String name);
    
    PlanItemInstanceQuery orderByStartTime();
    PlanItemInstanceQuery orderByName();
    
}

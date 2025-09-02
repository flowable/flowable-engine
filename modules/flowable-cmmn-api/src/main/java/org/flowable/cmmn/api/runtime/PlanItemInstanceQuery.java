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
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows to query for {@link PlanItemInstance}s.
 *
 * By default, as with other Flowable runtime API's, only runtime (not ended) plan item instances are returned.
 * However, {@link PlanItemInstance} entities are only removed once a case instance has ended.
 * This means that {@link PlanItemInstance} entities can still be queries when the case instance hasn't finished yet.
 * To return the 'ended' (i.e. completed/terminated/exit/occurred) instances, use the {@link #ended()}
 * or {@link #includeEnded()} methods.
 *
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface PlanItemInstanceQuery extends Query<PlanItemInstanceQuery, PlanItemInstance> {

    PlanItemInstanceQuery caseDefinitionId(String caseDefinitionId);
    PlanItemInstanceQuery derivedCaseDefinitionId(String derivedCaseDefinitionId);
    PlanItemInstanceQuery caseInstanceId(String caseInstanceId);
    PlanItemInstanceQuery caseInstanceIds(Set<String> caseInstanceIds);

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
    PlanItemInstanceQuery planItemInstanceStateAsyncActiveLeave();
    PlanItemInstanceQuery planItemInstanceStateAvailable();
    PlanItemInstanceQuery planItemInstanceStateUnavailable();
    PlanItemInstanceQuery planItemInstanceStateCompleted();
    PlanItemInstanceQuery planItemInstanceStateTerminated();
    PlanItemInstanceQuery planItemInstanceCreatedBefore(Date createdBefore);
    PlanItemInstanceQuery planItemInstanceCreatedAfter(Date createdAfter);
    PlanItemInstanceQuery planItemInstanceLastAvailableBefore(Date availableBefore);
    PlanItemInstanceQuery planItemInstanceLastAvailableAfter(Date availableAfter);
    PlanItemInstanceQuery planItemInstanceLastUnavailableBefore(Date unavailableBefore);
    PlanItemInstanceQuery planItemInstanceLastUnavailableAfter(Date unavailableAfter);
    PlanItemInstanceQuery planItemInstanceLastEnabledBefore(Date enabledBefore);
    PlanItemInstanceQuery planItemInstanceLastEnabledAfter(Date enabledAfter);
    PlanItemInstanceQuery planItemInstanceLastDisabledBefore(Date disabledBefore);
    PlanItemInstanceQuery planItemInstanceLastDisabledAfter(Date disabledAfter);
    PlanItemInstanceQuery planItemInstanceLastStartedBefore(Date startedBefore);
    PlanItemInstanceQuery planItemInstanceLastStartedAfter(Date startedAfter);
    PlanItemInstanceQuery planItemInstanceLastSuspendedBefore(Date suspendedBefore);
    PlanItemInstanceQuery planItemInstanceLastSuspendedAfter(Date suspendedAfter);
    PlanItemInstanceQuery planItemInstanceCompletedBefore(Date completedBefore);
    PlanItemInstanceQuery planItemInstanceCompletedAfter(Date completedAfter);
    PlanItemInstanceQuery planItemInstanceOccurredBefore(Date occurredBefore);
    PlanItemInstanceQuery planItemInstanceOccurredAfter(Date occurredAfter);
    PlanItemInstanceQuery planItemInstanceTerminatedBefore(Date terminatedBefore);
    PlanItemInstanceQuery planItemInstanceTerminatedAfter(Date terminatedAfter);
    PlanItemInstanceQuery planItemInstanceExitBefore(Date exitBefore);
    PlanItemInstanceQuery planItemInstanceExitAfter(Date exitAfter);
    PlanItemInstanceQuery planItemInstanceEndedBefore(Date endedBefore);
    PlanItemInstanceQuery planItemInstanceEndedAfter(Date endedAfter);
    PlanItemInstanceQuery planItemInstanceStartUserId(String startUserId);
    PlanItemInstanceQuery planItemInstanceAssignee(String assignee);
    PlanItemInstanceQuery planItemInstanceCompletedBy(String completedBy);
    PlanItemInstanceQuery planItemInstanceReferenceId(String referenceId);
    PlanItemInstanceQuery planItemInstanceReferenceType(String referenceType);
    PlanItemInstanceQuery planItemInstanceEntryCriterionId(String entryCriterionId);
    PlanItemInstanceQuery planItemInstanceExitCriterionId(String exitCriterionId);
    PlanItemInstanceQuery planItemInstanceFormKey(String formKey);
    PlanItemInstanceQuery planItemInstanceExtraValue(String extraValue);
    PlanItemInstanceQuery planItemInstanceCompletable();
    PlanItemInstanceQuery onlyStages();
    PlanItemInstanceQuery involvedUser(String involvedUser);
    PlanItemInstanceQuery involvedGroups(Collection<String> involvedGroups);
    PlanItemInstanceQuery planItemInstanceTenantId(String tenantId);
    PlanItemInstanceQuery planItemInstanceWithoutTenantId();
    
    PlanItemInstanceQuery planItemDefinitionId(String planItemDefinitionId);
    PlanItemInstanceQuery planItemDefinitionType(String planItemDefinitionType);
    PlanItemInstanceQuery planItemDefinitionTypes(List<String> planItemDefinitionType);

    /**
     * Begin an OR statement. Make sure you invoke the endOr method at the end of your OR statement.
     */
    PlanItemInstanceQuery or();

    /**
     * End an OR statement.
     */
    PlanItemInstanceQuery endOr();

    /**
     * @return The query will only return ended (completed/terminated/occurred/exited) plan item instances.
     *         No runtime instances will be returned.
     */
    PlanItemInstanceQuery ended();

    /**
     * @return The query will include both runtime and ended (completed/terminated/occurred/exited) plan item instances.
     */
    PlanItemInstanceQuery includeEnded();
    
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

    /**
     *
     * Include local plan item instance variables in the query result
     */
    PlanItemInstanceQuery includeLocalVariables();

    /**
     * Localize plan item name to specified locale.
     */
    PlanItemInstanceQuery locale(String locale);

    /**
     * Instruct localization to fallback to more general locales including the default locale of the JVM if the specified locale is not found.
     */
    PlanItemInstanceQuery withLocalizationFallback();
    
    PlanItemInstanceQuery orderByCreateTime();
    PlanItemInstanceQuery orderByEndTime();
    PlanItemInstanceQuery orderByName();
    
}

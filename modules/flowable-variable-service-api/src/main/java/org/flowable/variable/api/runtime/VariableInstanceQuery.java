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

package org.flowable.variable.api.runtime;

import java.util.Set;

import org.flowable.common.engine.api.query.Query;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * Programmatic querying for {@link VariableInstance}s.
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface VariableInstanceQuery extends Query<VariableInstanceQuery, VariableInstance> {

    /** Only select a historic variable with the given id. */
    VariableInstanceQuery id(String id);

    /** Only select historic process variables with the given process instance. */
    VariableInstanceQuery processInstanceId(String processInstanceId);

    /** Only select historic process variables with the given id. **/
    VariableInstanceQuery executionId(String executionId);

    /** Only select historic process variables whose id is in the given set of ids. */
    VariableInstanceQuery executionIds(Set<String> executionIds);

    /** Only select historic process variables with the given task. */
    VariableInstanceQuery taskId(String taskId);

    /** Only select historic process variables whose id is in the given set of ids. */
    VariableInstanceQuery taskIds(Set<String> taskIds);

    /** Only select historic process variables with the given variable name. */
    VariableInstanceQuery variableName(String variableName);

    /** Only select historic process variables where the given variable name is like. */
    VariableInstanceQuery variableNameLike(String variableNameLike);

    /** Only select historic process variables which were not set task-local. */
    VariableInstanceQuery excludeTaskVariables();

    /** Don't initialize variable values. This is foremost a way to deal with variable delete queries */
    VariableInstanceQuery excludeVariableInitialization();

    /** only select historic process variables with the given name and value */
    VariableInstanceQuery variableValueEquals(String variableName, Object variableValue);

    /**
     * only select historic process variables that don't have the given name and value
     */
    VariableInstanceQuery variableValueNotEquals(String variableName, Object variableValue);

    /**
     * only select historic process variables like the given name and value
     */
    VariableInstanceQuery variableValueLike(String variableName, String variableValue);

    /**
     * only select historic process variables like the given name and value (case insensitive)
     */
    VariableInstanceQuery variableValueLikeIgnoreCase(String variableName, String variableValue);
    
    /**
     * Only select historic variables with the given scope id.
     */
    VariableInstanceQuery scopeId(String scopeId);
    
    /**
     * Only select historic variables with the given sub scope id.
     */
    VariableInstanceQuery subScopeId(String subScopeId);
    
    /**
     * Only select historic variables with the give scope type.
     */
    VariableInstanceQuery scopeType(String scopeType);

    /**
     * Only select historic process variables which were not set local.
     */
    VariableInstanceQuery excludeLocalVariables();

    VariableInstanceQuery orderByProcessInstanceId();

    VariableInstanceQuery orderByVariableName();

}

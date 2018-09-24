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

import java.util.Set;

import org.flowable.common.engine.api.query.Query;
import org.flowable.variable.api.history.HistoricVariableInstance;

/**
 * @author Joram Barrez
 */
public interface HistoricVariableInstanceQuery extends Query<HistoricVariableInstanceQuery, HistoricVariableInstance> {
    
    /** Only select a historic variable with the given id. */
    HistoricVariableInstanceQuery id(String id);

    /** Only select historic variables with the given case instance. */
    HistoricVariableInstanceQuery caseInstanceId(String caseInstanceId);
    
    /** Only select historic variables with the given plan item instance id. */
    HistoricVariableInstanceQuery planItemInstanceId(String planItemInstanceId);

    /** Only select historic variables with the given task. */
    HistoricVariableInstanceQuery taskId(String taskId);

    /** Only select historic variables whose id is in the given set of ids. */
    HistoricVariableInstanceQuery taskIds(Set<String> taskIds);

    /** Only select historic variables with the given variable name. */
    HistoricVariableInstanceQuery variableName(String variableName);

    /** Only select historic variables where the given variable name is like. */
    HistoricVariableInstanceQuery variableNameLike(String variableNameLike);

    /** Only select historic variables which were not set task-local. */
    HistoricVariableInstanceQuery excludeTaskVariables();

    /** Don't initialize variable values. This is foremost a way to deal with variable delete queries */
    HistoricVariableInstanceQuery excludeVariableInitialization();

    /** only select historic variables with the given name and value */
    HistoricVariableInstanceQuery variableValueEquals(String variableName, Object variableValue);

    /** only select historic variables that don't have the given name and value */
    HistoricVariableInstanceQuery variableValueNotEquals(String variableName, Object variableValue);

    /** only select historic variables like the given name and value */
    HistoricVariableInstanceQuery variableValueLike(String variableName, String variableValue);

    /** only select historic variables like the given name and value (case insensitive) */
    HistoricVariableInstanceQuery variableValueLikeIgnoreCase(String variableName, String variableValue);

    /** order historic variables by the name. Use asc() or desc() afterwards. */
    HistoricVariableInstanceQuery orderByVariableName();

}

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
package org.flowable.variable.service;

import java.util.List;

import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * Service which provides access to variables.
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface VariableService {

    InternalVariableInstanceQuery createInternalVariableInstanceQuery();

    default List<VariableInstanceEntity> findVariableInstancesByExecutionId(String executionId) {
        return createInternalVariableInstanceQuery().executionId(executionId).withoutTaskId().list();
    }

    default List<VariableInstanceEntity> findVariableInstanceByScopeIdAndScopeType(String scopeId, String scopeType) {
        return createInternalVariableInstanceQuery().scopeId(scopeId).withoutSubScopeId().scopeType(scopeType).list();
    }

    default List<VariableInstanceEntity> findVariableInstanceBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return createInternalVariableInstanceQuery().subScopeId(subScopeId).scopeType(scopeType).list();
    }

    VariableInstanceEntity createVariableInstance(String name, VariableType type, Object value);

    /**
     * Create a variable instance without setting the value on it.
     * <b>IMPORTANT:</b> If you use this method you would have to call {@link VariableInstanceEntity#setValue(Object)}
     * for setting the value
     * @param name the name of the variable to create
     * @param type the type of the created variable
     *
     * @return the {@link VariableInstanceEntity} to be used
     */
    VariableInstanceEntity createVariableInstance(String name, VariableType type);

    void insertVariableInstance(VariableInstanceEntity variable);

    /**
     * Updates variable instance with the new value
     *
     * @param variable to update
     */
    void updateVariableInstance(VariableInstanceEntity variable);

    void deleteVariableInstance(VariableInstanceEntity variable);

    void deleteVariablesByExecutionId(String executionId);
    
    void deleteVariablesByTaskId(String taskId);

}

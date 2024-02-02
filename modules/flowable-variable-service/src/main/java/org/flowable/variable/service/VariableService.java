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

    /**
     * Create a variable instance with the given name and value for the given tenant.
     *
     * @param name the name of the variable to create
     * @return the {@link VariableInstanceEntity} to be used
     */
    VariableInstanceEntity createVariableInstance(String name);

    void insertVariableInstance(VariableInstanceEntity variable);

    /**
     * Inserts a variable instance with the given value.
     * @param variable the variable instance to insert
     * @param value the value to set
     * @param tenantId the tenant id of the variable instance
     */
    void insertVariableInstanceWithValue(VariableInstanceEntity variable, Object value, String tenantId);

    void deleteVariableInstance(VariableInstanceEntity variable);

    void deleteVariablesByExecutionId(String executionId);
    
    void deleteVariablesByTaskId(String taskId);

}

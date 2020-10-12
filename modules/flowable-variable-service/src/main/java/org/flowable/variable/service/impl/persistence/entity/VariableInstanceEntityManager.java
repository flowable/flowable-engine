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
package org.flowable.variable.service.impl.persistence.entity;

import java.util.Collection;

import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.InternalVariableInstanceQuery;

/**
 * @author Joram Barrez
 */
public interface VariableInstanceEntityManager extends EntityManager<VariableInstanceEntity> {

    VariableInstanceEntity create(String name, VariableType type, Object value);

    /**
     * Create a variable instance without setting the value on it.
     * <b>IMPORTANT:</b> If you use this method you would have to call {@link VariableInstanceEntity#setValue(Object)}
     * for setting the value
     * @param name the name of the variable to create
     * @param type the type of the created variable
     *
     * @return the {@link VariableInstanceEntity} to be used
     */
    VariableInstanceEntity create(String name, VariableType type);

    InternalVariableInstanceQuery createInternalVariableInstanceQuery();

    void deleteVariablesByTaskId(String taskId);

    void deleteVariablesByExecutionId(String executionId);
    
    void deleteByScopeIdAndScopeType(String scopeId, String scopeType);

    void deleteByScopeIdAndScopeTypes(String scopeId, Collection<String> scopeTypes);

    void deleteBySubScopeIdAndScopeTypes(String subScopeId, Collection<String> scopeTypes);

}
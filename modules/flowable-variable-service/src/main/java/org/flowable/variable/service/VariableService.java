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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.VariableType;

/**
 * Service which provides access to variables.
 * 
 * @author Tijs Rademakers
 */
public interface VariableService {
    
    VariableInstanceEntity getVariableInstance(String id);
    
    List<VariableInstanceEntity> findVariableInstancesByTaskId(String taskId);
    
    List<VariableInstanceEntity> findVariableInstancesByTaskIds(Set<String> taskIds);
    
    List<VariableInstanceEntity> findVariableInstancesByExecutionId(String executionId);
    
    List<VariableInstanceEntity> findVariableInstancesByExecutionIds(Set<String> executionIds);
    
    VariableInstanceEntity findVariableInstanceByTaskAndName(String taskId, String taskName);
    
    List<VariableInstanceEntity> findVariableInstancesByTaskAndNames(String taskId, Collection<String> taskNames);
    
    VariableInstanceEntity findVariableInstanceByExecutionAndName(String executionId, String taskName);
    
    List<VariableInstanceEntity> findVariableInstancesByExecutionAndNames(String executionId, Collection<String> taskNames);
    
    VariableInstanceEntity createVariableInstance(String name, VariableType type, Object value);
    
    void insertVariableInstance(VariableInstanceEntity variable);
    
    void deleteVariableInstance(VariableInstanceEntity variable);
    
    void deleteVariableInstanceMap(Map<String, VariableInstanceEntity> variableInstances);
    
}

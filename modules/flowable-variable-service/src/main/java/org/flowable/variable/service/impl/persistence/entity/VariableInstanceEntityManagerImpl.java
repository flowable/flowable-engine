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
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Saeid Mirzaei
 */
public class VariableInstanceEntityManagerImpl
    extends AbstractServiceEngineEntityManager<VariableServiceConfiguration, VariableInstanceEntity, VariableInstanceDataManager>
    implements VariableInstanceEntityManager {

    public VariableInstanceEntityManagerImpl(VariableServiceConfiguration variableServiceConfiguration, VariableInstanceDataManager variableInstanceDataManager) {
        super(variableServiceConfiguration, variableInstanceDataManager);
    }

    @Override
    public VariableInstanceEntity create(String name, VariableType type, Object value) {
        VariableInstanceEntity variableInstance = create(name, type);
        variableInstance.setValue(value);
        return variableInstance;
    }

    @Override
    public VariableInstanceEntity create(String name, VariableType type) {
        VariableInstanceEntity variableInstance = create();
        variableInstance.setName(name);
        variableInstance.setType(type);
        variableInstance.setTypeName(type.getTypeName());
        return variableInstance;
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskId(String taskId) {
        return dataManager.findVariableInstancesByTaskId(taskId);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskIds(Set<String> taskIds) {
        return dataManager.findVariableInstancesByTaskIds(taskIds);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionId(final String executionId) {
        return dataManager.findVariableInstancesByExecutionId(executionId);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionIds(Set<String> executionIds) {
        return dataManager.findVariableInstancesByExecutionIds(executionIds);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByExecutionAndName(String executionId, String variableName) {
        return dataManager.findVariableInstanceByExecutionAndName(executionId, variableName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionAndNames(String executionId, Collection<String> names) {
        return dataManager.findVariableInstancesByExecutionAndNames(executionId, names);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByTaskAndName(String taskId, String variableName) {
        return dataManager.findVariableInstanceByTaskAndName(taskId, variableName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskAndNames(String taskId, Collection<String> names) {
        return dataManager.findVariableInstancesByTaskAndNames(taskId, names);
    }
    
    @Override
    public List<VariableInstanceEntity> findVariableInstanceByScopeIdAndScopeType(String scopeId, String scopeType) {
        return dataManager.findVariableInstanceByScopeIdAndScopeType(scopeId, scopeType);
    }
    
    @Override
    public VariableInstanceEntity findVariableInstanceByScopeIdAndScopeTypeAndName(String scopeId, String scopeType, String variableName) {
        return dataManager.findVariableInstanceByScopeIdAndScopeTypeAndName(scopeId, scopeType, variableName);
    }
    
    @Override
    public List<VariableInstanceEntity> findVariableInstancesByScopeIdAndScopeTypeAndNames(String scopeId, String scopeType, Collection<String> variableNames) {
        return dataManager.findVariableInstancesByScopeIdAndScopeTypeAndNames(scopeId, scopeType, variableNames);
    }
    
    @Override
    public List<VariableInstanceEntity> findVariableInstanceBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return dataManager.findVariableInstanceBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceBySubScopeIdAndScopeTypeAndName(String subScopeId, String scopeType, String variableName) {
        return dataManager.findVariableInstanceBySubScopeIdAndScopeTypeAndName(subScopeId, scopeType, variableName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesBySubScopeIdAndScopeTypeAndNames(String subScopeId, String scopeType, Collection<String> variableNames) {
        return dataManager.findVariableInstancesBySubScopeIdAndScopeTypeAndNames(subScopeId, scopeType, variableNames);
    }

    @Override
    public void delete(VariableInstanceEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, false);
        VariableByteArrayRef byteArrayRef = entity.getByteArrayRef();
        if (byteArrayRef != null) {
            byteArrayRef.delete();
        }
        entity.setDeleted(true);
    }

    @Override
    public void deleteVariablesByTaskId(String taskId) {
        dataManager.deleteVariablesByTaskId(taskId);
    }
    
    @Override
    public void deleteVariablesByExecutionId(String executionId) {
        dataManager.deleteVariablesByExecutionId(executionId);
    }
    
    @Override
    public void deleteByScopeIdAndScopeType(String scopeId, String scopeType) {
        dataManager.deleteByScopeIdAndScopeType(scopeId, scopeType);
    }

}

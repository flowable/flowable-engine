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
package org.flowable.variable.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.service.CommonServiceImpl;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class VariableServiceImpl extends CommonServiceImpl<VariableServiceConfiguration> implements VariableService {

    public VariableServiceImpl(VariableServiceConfiguration variableServiceConfiguration) {
        super(variableServiceConfiguration);
    }

    @Override
    public VariableInstanceEntity getVariableInstance(String id) {
        return getVariableInstanceEntityManager().findById(id);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskId(String taskId) {
        return getVariableInstanceEntityManager().findVariableInstancesByTaskId(taskId);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskIds(Set<String> taskIds) {
        return getVariableInstanceEntityManager().findVariableInstancesByTaskIds(taskIds);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionId(String executionId) {
        return getVariableInstanceEntityManager().findVariableInstancesByExecutionId(executionId);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionIds(Set<String> executionIds) {
        return getVariableInstanceEntityManager().findVariableInstancesByExecutionIds(executionIds);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByTaskAndName(String taskId, String taskName) {
        return getVariableInstanceEntityManager().findVariableInstanceByTaskAndName(taskId, taskName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskAndNames(String taskId, Collection<String> taskNames) {
        return getVariableInstanceEntityManager().findVariableInstancesByTaskAndNames(taskId, taskNames);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByExecutionAndName(String executionId, String taskName) {
        return getVariableInstanceEntityManager().findVariableInstanceByExecutionAndName(executionId, taskName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionAndNames(String executionId, Collection<String> taskNames) {
        return getVariableInstanceEntityManager().findVariableInstancesByExecutionAndNames(executionId, taskNames);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstanceByScopeIdAndScopeType(String scopeId, String scopeType) {
        return getVariableInstanceEntityManager().findVariableInstanceByScopeIdAndScopeType(scopeId, scopeType);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByScopeIdAndScopeTypeAndName(String scopeId, String scopeType, String variableName) {
        return getVariableInstanceEntityManager().findVariableInstanceByScopeIdAndScopeTypeAndName(scopeId, scopeType, variableName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByScopeIdAndScopeTypeAndNames(String scopeId, String scopeType, Collection<String> variableNames) {
        return getVariableInstanceEntityManager().findVariableInstancesByScopeIdAndScopeTypeAndNames(scopeId, scopeType, variableNames);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstanceBySubScopeIdAndScopeType(String subScopeId, String scopeType) {
        return getVariableInstanceEntityManager().findVariableInstanceBySubScopeIdAndScopeType(subScopeId, scopeType);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceBySubScopeIdAndScopeTypeAndName(String subScopeId, String scopeType, String variableName) {
        return getVariableInstanceEntityManager().findVariableInstanceBySubScopeIdAndScopeTypeAndName(subScopeId, scopeType, variableName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesBySubScopeIdAndScopeTypeAndNames(String subScopeId, String scopeType, Collection<String> variableNames) {
        return getVariableInstanceEntityManager().findVariableInstancesBySubScopeIdAndScopeTypeAndNames(subScopeId, scopeType, variableNames);
    }

    @Override
    public VariableInstanceEntity createVariableInstance(String name, VariableType type, Object value) {
        return getVariableInstanceEntityManager().create(name, type, value);
    }

    @Override
    public void insertVariableInstance(VariableInstanceEntity variable) {
        getVariableInstanceEntityManager().insert(variable);
    }

    @Override
    public void updateVariableInstance(VariableInstanceEntity variableInstance) {
        getVariableInstanceEntityManager().update(variableInstance, true);
    }

    @Override
    public void deleteVariableInstance(VariableInstanceEntity variable) {
        getVariableInstanceEntityManager().delete(variable);
    }

    @Override
    public void deleteVariablesByExecutionId(String executionId) {
        getVariableInstanceEntityManager().deleteVariablesByExecutionId(executionId);
    }
    
    @Override
    public void deleteVariablesByTaskId(String taskId) {
        getVariableInstanceEntityManager().deleteVariablesByTaskId(taskId);
    }

    public VariableInstanceEntityManager getVariableInstanceEntityManager() {
        return configuration.getVariableInstanceEntityManager();
    }

}

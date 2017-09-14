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
import java.util.Map;
import java.util.Set;

import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;
import org.flowable.variable.service.impl.types.VariableType;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Saeid Mirzaei
 */
public class VariableInstanceEntityManagerImpl extends AbstractEntityManager<VariableInstanceEntity> implements VariableInstanceEntityManager {

    protected VariableInstanceDataManager variableInstanceDataManager;

    public VariableInstanceEntityManagerImpl(VariableServiceConfiguration variableServiceConfiguration, VariableInstanceDataManager variableInstanceDataManager) {
        super(variableServiceConfiguration);
        this.variableInstanceDataManager = variableInstanceDataManager;
    }

    @Override
    protected DataManager<VariableInstanceEntity> getDataManager() {
        return variableInstanceDataManager;
    }

    @Override
    public VariableInstanceEntity create(String name, VariableType type, Object value) {
        VariableInstanceEntity variableInstance = create();
        variableInstance.setName(name);
        variableInstance.setType(type);
        variableInstance.setTypeName(type.getTypeName());
        variableInstance.setValue(value);
        return variableInstance;
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskId(String taskId) {
        return variableInstanceDataManager.findVariableInstancesByTaskId(taskId);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskIds(Set<String> taskIds) {
        return variableInstanceDataManager.findVariableInstancesByTaskIds(taskIds);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionId(final String executionId) {
        return variableInstanceDataManager.findVariableInstancesByExecutionId(executionId);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionIds(Set<String> executionIds) {
        return variableInstanceDataManager.findVariableInstancesByExecutionIds(executionIds);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByExecutionAndName(String executionId, String variableName) {
        return variableInstanceDataManager.findVariableInstanceByExecutionAndName(executionId, variableName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionAndNames(String executionId, Collection<String> names) {
        return variableInstanceDataManager.findVariableInstancesByExecutionAndNames(executionId, names);
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByTaskAndName(String taskId, String variableName) {
        return variableInstanceDataManager.findVariableInstanceByTaskAndName(taskId, variableName);
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskAndNames(String taskId, Collection<String> names) {
        return variableInstanceDataManager.findVariableInstancesByTaskAndNames(taskId, names);
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
    public void deleteVariableInstanceMap(Map<String, VariableInstanceEntity> variableInstances) {
        if (variableInstances != null) {
            for (VariableInstanceEntity variableInstance : variableInstances.values()) {
                delete(variableInstance);
            }
        }
    }

    public VariableInstanceDataManager getVariableInstanceDataManager() {
        return variableInstanceDataManager;
    }

    public void setVariableInstanceDataManager(VariableInstanceDataManager variableInstanceDataManager) {
        this.variableInstanceDataManager = variableInstanceDataManager;
    }

}

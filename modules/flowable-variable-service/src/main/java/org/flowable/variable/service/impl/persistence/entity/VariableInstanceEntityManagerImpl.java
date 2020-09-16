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

import org.flowable.common.engine.impl.persistence.entity.AbstractServiceEngineEntityManager;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.InternalVariableInstanceQuery;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.InternalVariableInstanceQueryImpl;
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
        super(variableServiceConfiguration, variableServiceConfiguration.getEngineName(), variableInstanceDataManager);
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
    public InternalVariableInstanceQuery createInternalVariableInstanceQuery() {
        return new InternalVariableInstanceQueryImpl(dataManager);
    }

    @Override
    public void delete(VariableInstanceEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, false);
        ByteArrayRef byteArrayRef = entity.getByteArrayRef();
        if (byteArrayRef != null) {
            byteArrayRef.delete(serviceConfiguration.getEngineName());
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

    @Override
    public void deleteByScopeIdAndScopeTypes(String scopeId, Collection<String> scopeTypes) {
        dataManager.deleteByScopeIdAndScopeTypes(scopeId, scopeTypes);
    }

    @Override
    public void deleteBySubScopeIdAndScopeTypes(String subScopeId, Collection<String> scopeTypes) {
        dataManager.deleteBySubScopeIdAndScopeTypes(subScopeId, scopeTypes);
    }
}

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

import org.flowable.common.engine.impl.service.CommonServiceImpl;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.InternalVariableInstanceQuery;
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
    public InternalVariableInstanceQuery createInternalVariableInstanceQuery() {
        return getVariableInstanceEntityManager().createInternalVariableInstanceQuery();
    }

    @Override
    public VariableInstanceEntity createVariableInstance(String name, VariableType type, Object value) {
        return getVariableInstanceEntityManager().create(name, type, value);
    }

    @Override
    public VariableInstanceEntity createVariableInstance(String name, VariableType type) {
        return getVariableInstanceEntityManager().create(name, type);
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

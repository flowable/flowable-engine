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

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * Wrapper class for {@link VariableServiceConfiguration} to enable VariableValueProvider functionality
 *
 * @author Christopher Welsch
 */
public class VariableServiceConfigurationVariableValueProvider implements VariableValueProvider {

    protected VariableServiceConfiguration variableServiceConfiguration;

    public VariableServiceConfigurationVariableValueProvider(VariableServiceConfiguration variableServiceConfiguration) {
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    @Override
    public VariableType findVariableType(Object value) {
        return variableServiceConfiguration.getVariableTypes().findVariableType(value);
    }

    @Override
    public ValueFields createValueFields(String name, VariableType type, Object value) {
        VariableInstanceEntity variableInstanceEntity = variableServiceConfiguration.getVariableInstanceEntityManager().create();
        variableInstanceEntity.setName(name);
        variableInstanceEntity.setTypeName(type.getTypeName());
        variableInstanceEntity.setType(type);
        // We don't have to go through VariableInstanceValueModifier here, because this is used in the sqlmap for the where clause.
        // The 'type' has been predefined in this case.
        variableInstanceEntity.setValue(value);
        return variableInstanceEntity;
    }
}

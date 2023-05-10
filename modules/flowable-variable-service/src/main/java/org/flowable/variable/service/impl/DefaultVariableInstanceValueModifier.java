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

import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * The default implementation of the {@link VariableInstanceValueModifier} interface for the Flowable Variable Service.
 * <p>
 * It implements the default lookup logic for variable types, setting and updating of values for variable instances.
 */
public class DefaultVariableInstanceValueModifier implements VariableInstanceValueModifier {

    protected final VariableServiceConfiguration serviceConfiguration;

    public DefaultVariableInstanceValueModifier(VariableServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    @Override
    public void setVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
        if (variableInstance instanceof VariableInstanceEntity) {
            VariableInstanceEntity variableInstanceEntity = (VariableInstanceEntity) variableInstance;
            VariableType variableType = determineVariableType(serviceConfiguration.getVariableTypes(), variableInstance, value, tenantId);
            setVariableType(variableInstanceEntity, variableType);
        }
        setOrUpdateValue(variableInstance, value, tenantId);
    }

    @Override
    public void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
        /* Always check if the type should be altered. It's possible that the previous type is lower in the type
         * checking chain (e.g. serializable) and will return true on isAbleToStore(), even though another type higher in the chain is eligible for storage.
         */
        if (variableInstance instanceof VariableInstanceEntity) {
            VariableInstanceEntity variableInstanceEntity = (VariableInstanceEntity) variableInstance;
            VariableType variableType = determineVariableType(serviceConfiguration.getVariableTypes(), variableInstance, value, tenantId);
            if (!variableType.equals(variableInstanceEntity.getType())) {
                // variable type has changed
                variableInstance.setValue(null);
                setVariableType(variableInstanceEntity, variableType);
                variableInstanceEntity.forceUpdate();
            }
        }
        setOrUpdateValue(variableInstance, value, tenantId);
    }

    protected VariableType determineVariableType(VariableTypes variableTypes, VariableInstance variableInstance, Object value, String tenantId) {
        return variableTypes.findVariableType(value);
    }

    /**
     * Sets the type of the variable instance.
     * @param variableInstance the variable instance to be modified
     * @param type the type to be set for the variable instance
     */
    protected void setVariableType(VariableInstanceEntity variableInstance, VariableType type) {
        variableInstance.setTypeName(type.getTypeName());
        variableInstance.setType(type);
    }

    /**
     * Central hook method for all modifications of a value for the variable instance.
     * Please note, that transient variable instances are passed here as well.
     *
     * @param variableInstance the variable instance to be modified
     * @param value the value to be set for the variable instance
     * @param tenantId the ID of the tenant the variable instance belongs to
     */
    protected void setOrUpdateValue(VariableInstance variableInstance, Object value, String tenantId) {
        variableInstance.setValue(value);
    }
}

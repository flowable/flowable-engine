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

/**
 * Interface that can be implemented to enhance variable instances during the lifecyle of a {@link org.flowable.variable.api.persistence.entity.VariableInstance}
 * @author Arthur Hupka-Merle
 */
public interface VariableInstanceEnhancer {

    /**
     * Called before the value is set on the variable instance. The returned value will be set on the variable instance
     * as value and cached value.
     * @param variableInstance the variable instance entity the value will be set on
     * @param originalValue the original value for the variableInstanceEntity
     * @return the value that will be set on the variable instance
     */
    Object preSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue);

    /**
     * Called after the value is set on the variable instance.
     * @param variableInstance the variable instance entity the value was set on
     * @param originalValue the non-enhanced value that was used to choose the {@link org.flowable.variable.api.types.VariableType}.
     * {@code variableValue} and {@code originalValue} may often be the same
     * @param variableValue the value that was set on the variable instance
     */
    void postSetVariableValue(String tenantId, VariableInstance variableInstance, Object originalValue, Object variableValue);


}

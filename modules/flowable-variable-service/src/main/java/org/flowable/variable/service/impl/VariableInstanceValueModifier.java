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
 * This interface is the contract for modifying the values for persistent and transient variable instances.
 * It is called, whenever a value is set to a new variable or an existing (persistent) variable is updated.
 *
 * @author Arthur Hupka-Merle
 */
public interface VariableInstanceValueModifier {


    /**
     * Sets the value of a new persistent or transient variable instance.
     * This is invoked when a variable instance is created.
     *
     * @param variableInstance the variable instance to be modified
     * @param value the new value to be set for the variable instance.
     * @param tenantId the ID of the tenant the variable instance belongs to
     */
    void setVariableValue(VariableInstance variableInstance, Object value, String tenantId);

    /**
     * Updates the value of a variable instance.
     * This is invoked when a variable instance already exists and its value is being updated.
     *
     * @param variableInstance the variable instance to be modified
     * @param value the value to be set for the updated variable instance.
     * @param tenantId the ID of the tenant the variable instance belongs to
     */
    void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId);
}

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
 * An empty default implementation of {@link VariableInstanceEnhancer}, which can be used as a base class to
 * override specific methods.
 * @author Arthur Hupka-Merle
 */
public class DefaultVariableInstanceEnhancer implements VariableInstanceEnhancer {

    @Override
    public Object preSetVariableValue(VariableInstance variableInstance, Object originalValue) {
        return originalValue;
    }

    @Override
    public void postSetVariableValue(VariableInstance variableInstance, Object originalValue, Object variableValue) {
    }
}

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
package org.flowable.variable.service.impl.types;

import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @param <O> The type of the original object
 * @param <C> The type of the original copy object
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class TraceableObject<O, C> {

    protected MutableVariableType<O, C> type;
    protected O tracedObject;
    protected C tracedObjectOriginalValue;
    protected VariableInstanceEntity variableInstanceEntity;
    protected VariableServiceConfiguration variableServiceConfiguration;

    public TraceableObject(MutableVariableType<O, C> type, O tracedObject, C tracedObjectOriginalValue, 
                    VariableInstanceEntity variableInstanceEntity, VariableServiceConfiguration variableServiceConfiguration) {
        
        this.type = type;
        this.tracedObject = tracedObject;
        this.tracedObjectOriginalValue = tracedObjectOriginalValue;
        this.variableInstanceEntity = variableInstanceEntity;
        this.variableServiceConfiguration = variableServiceConfiguration;
    }

    public void updateIfValueChanged() {
        if (tracedObject == variableInstanceEntity.getCachedValue() && !variableInstanceEntity.isDeleted()) {
            if (type.updateValueIfChanged(tracedObject, tracedObjectOriginalValue, variableInstanceEntity)) {
                variableServiceConfiguration.getInternalHistoryVariableManager().recordVariableUpdate(
                                variableInstanceEntity, variableServiceConfiguration.getClock().getCurrentTime());
            }
        }
    }
}

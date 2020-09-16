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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.HasVariableServiceConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
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

    public TraceableObject(MutableVariableType<O, C> type, O tracedObject, C tracedObjectOriginalValue, 
                    VariableInstanceEntity variableInstanceEntity) {
        
        this.type = type;
        this.tracedObject = tracedObject;
        this.tracedObjectOriginalValue = tracedObjectOriginalValue;
        this.variableInstanceEntity = variableInstanceEntity;
    }

    public void updateIfValueChanged() {
        if (tracedObject == variableInstanceEntity.getCachedValue()) {
            if (type.updateValueIfChanged(tracedObject, tracedObjectOriginalValue, variableInstanceEntity)) {
                VariableServiceConfiguration variableServiceConfiguration = getVariableServiceConfiguration();
                variableServiceConfiguration.getInternalHistoryVariableManager().recordVariableUpdate(
                        variableInstanceEntity, variableServiceConfiguration.getClock().getCurrentTime());
            }
        }
    }
    
    protected VariableServiceConfiguration getVariableServiceConfiguration() {
        String engineType = getEngineType(variableInstanceEntity.getScopeType());
        Map<String, AbstractEngineConfiguration> engineConfigurationMap = Context.getCommandContext().getEngineConfigurations();
        AbstractEngineConfiguration engineConfiguration = engineConfigurationMap.get(engineType);
        if (engineConfiguration == null) {
            for (AbstractEngineConfiguration possibleEngineConfiguration : engineConfigurationMap.values()) {
                if (possibleEngineConfiguration instanceof HasVariableServiceConfiguration) {
                    engineConfiguration = possibleEngineConfiguration;
                }
            }
        }
        
        if (engineConfiguration == null) {
            throw new FlowableException("Could not find engine configuration with variable service configuration");
        }
        
        if (!(engineConfiguration instanceof HasVariableServiceConfiguration)) {
            throw new FlowableException("Variable entity engine scope has no variable service configuration " + engineType);
        }
        
        return (VariableServiceConfiguration) engineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG);
    }
    
    protected String getEngineType(String scopeType) {
        if (StringUtils.isNotEmpty(scopeType)) {
            return scopeType;
        } else {
            return ScopeTypes.BPMN;
        }
    }
}

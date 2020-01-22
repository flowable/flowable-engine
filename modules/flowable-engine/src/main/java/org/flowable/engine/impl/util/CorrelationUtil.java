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
package org.flowable.engine.impl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

public class CorrelationUtil {

    public static String getCorrelationKey(String elementName, CommandContext commandContext, ExecutionEntity executionEntity) {
        return getCorrelationKey(elementName, commandContext, executionEntity.getCurrentFlowElement(), executionEntity);
    }
    
    public static String getCorrelationKey(String elementName, CommandContext commandContext, FlowElement flowElement, ExecutionEntity executionEntity) {
        String correlationKey = null;
        if (flowElement != null) {
            List<ExtensionElement> eventCorrelations = flowElement.getExtensionElements().get(elementName);
            if (eventCorrelations != null && !eventCorrelations.isEmpty()) {
                ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
                ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

                Map<String, Object> correlationParameters = new HashMap<>();
                for (ExtensionElement eventCorrelation : eventCorrelations) {
                    String name = eventCorrelation.getAttributeValue(null, "name");
                    String valueExpression = eventCorrelation.getAttributeValue(null, "value");
                    if (StringUtils.isNotEmpty(valueExpression)) {
                        if (executionEntity != null) {
                            Object value = expressionManager.createExpression(valueExpression).getValue(executionEntity);
                            correlationParameters.put(name, value);
                        } else {
                            correlationParameters.put(name, valueExpression);
                        }
                        
                    } else {
                        correlationParameters.put(name, null);
                    }
                }

                correlationKey = CommandContextUtil.getEventRegistry().generateKey(correlationParameters);
            }
        }
        
        return correlationKey;
    }
    
}

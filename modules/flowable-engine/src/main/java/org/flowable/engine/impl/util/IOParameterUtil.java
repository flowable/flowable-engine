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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class IOParameterUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOParameterUtil.class);

    public static void processInParameters(List<IOParameter> inParameters, VariableContainer sourceContainer, VariableContainer targetContainer,
            ExpressionManager expressionManager) {
        processInParameters(inParameters, sourceContainer, targetContainer::setVariable, targetContainer::setTransientVariable, expressionManager);
    }

    public static void processInParameters(List<IOParameter> inParameters, VariableContainer sourceContainer, BiConsumer<String, Object> targetVariableConsumer,
            BiConsumer<String, Object> targetTransientVariableConsumer, ExpressionManager expressionManager) {
        processParameters(inParameters, sourceContainer, targetVariableConsumer, targetTransientVariableConsumer, expressionManager, "In");
    }

    public static void processOutParameters(List<IOParameter> outParameters, VariableContainer sourceContainer, BiConsumer<String, Object> targetVariableConsumer,
            BiConsumer<String, Object> targetTransientVariableConsumer, ExpressionManager expressionManager) {
        processParameters(outParameters, sourceContainer, targetVariableConsumer, targetTransientVariableConsumer, expressionManager, "Out");
    }

    public static Map<String, Object> extractOutVariables(List<IOParameter> outParameters, VariableContainer sourceContainer, ExpressionManager expressionManager) {
        if (outParameters == null || outParameters.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> payload = new HashMap<>();
        processParameters(outParameters, sourceContainer, payload::put, payload::put, expressionManager, "Out");

        return payload;
    }

    protected static void processParameters(List<IOParameter> parameters, VariableContainer sourceContainer, BiConsumer<String, Object> targetVariableConsumer,
            BiConsumer<String, Object> targetTransientVariableConsumer, ExpressionManager expressionManager, String parameterType) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        for (IOParameter parameter : parameters) {

            Object value;
            if (StringUtils.isNotEmpty(parameter.getSourceExpression())) {
                Expression expression = expressionManager.createExpression(parameter.getSourceExpression().trim());
                value = expression.getValue(sourceContainer);
            } else {
                value = sourceContainer.getVariable(parameter.getSource());
            }

            String variableName = null;
            if (StringUtils.isNotEmpty(parameter.getTargetExpression())) {
                Expression expression = expressionManager.createExpression(parameter.getTargetExpression());

                Object variableNameValue = expression.getValue(sourceContainer);
                if (variableNameValue != null) {
                    variableName = variableNameValue.toString();
                } else {
                    LOGGER.warn("{} parameter target expression {} did not resolve to a variable name, this is most likely a programmatic error",
                            parameterType, parameter.getTargetExpression());
                }

            } else if (StringUtils.isNotEmpty(parameter.getTarget())){
                variableName = parameter.getTarget();

            }

            if (parameter.isTransient()) {
                targetTransientVariableConsumer.accept(variableName, value);
            } else {
                targetVariableConsumer.accept(variableName, value);
            }
        }
    }

}

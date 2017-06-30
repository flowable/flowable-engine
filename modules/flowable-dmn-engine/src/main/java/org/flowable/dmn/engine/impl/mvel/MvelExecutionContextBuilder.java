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
package org.flowable.dmn.engine.impl.mvel;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.dmn.engine.impl.audit.DecisionExecutionAuditUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.flowable.engine.common.api.FlowableException;
import org.joda.time.LocalDate;
import org.mvel2.ParserContext;
import org.mvel2.integration.PropertyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class MvelExecutionContextBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MvelExecutionContextBuilder.class);

    public static MvelExecutionContext build(Decision decision, Map<String, Object> inputVariables,
            Map<String, Method> customExpressionFunctions, Map<Class<?>, PropertyHandler> propertyHandlers) {

        MvelExecutionContext executionContext = new MvelExecutionContext();

        // initialize audit trail
        executionContext.setAuditContainer(DecisionExecutionAuditUtil.initializeRuleExecutionAudit(decision, inputVariables));

        ParserContext parserContext = new ParserContext();

        // add custom functions to context
        if (customExpressionFunctions != null && !customExpressionFunctions.isEmpty()) {
            for (Map.Entry<String, Method> config : customExpressionFunctions.entrySet()) {
                parserContext.addImport(config.getKey(), config.getValue());
            }
        }

        executionContext.setParserContext(parserContext);

        // add property handlers to context
        if (propertyHandlers != null) {
            for (Class<?> variableClass : propertyHandlers.keySet()) {
                executionContext.addPropertyHandler(variableClass, propertyHandlers.get(variableClass));
            }
        }

        DecisionTable decisionTable = (DecisionTable) decision.getExpression();

        // add output values to context
        if (decisionTable.getOutputs() != null) {
            for (OutputClause outputClause : decisionTable.getOutputs()) {
                if (outputClause.getOutputValues() != null && outputClause.getOutputValues().getTextValues() != null) {
                    executionContext.addOutputValues(outputClause.getName(),
                        ExecutionVariableFactory.getExecutionVariables(outputClause.getTypeRef(), outputClause.getOutputValues().getTextValues()));
                }
            }
        }

        // set aggregator
        if (decisionTable.getAggregation() != null) {
            executionContext.setAggregator(decisionTable.getAggregation());
        }

        preProcessInputVariables(decisionTable, inputVariables);

        executionContext.setStackVariables(inputVariables);

        LOGGER.debug("Execution Context created");

        return executionContext;
    }

    protected static void preProcessInputVariables(DecisionTable decisionTable, Map<String, Object> inputVariables) {

        if (inputVariables == null) {
            inputVariables = new HashMap<>();
        }

        // check if there are input expressions that refer to none existing input variables
        // that need special handling
        for (InputClause inputClause : decisionTable.getInputs()) {
            if (!inputVariables.containsKey(inputClause.getInputExpression().getText()) && "boolean".equals(inputClause.getInputExpression().getTypeRef())) {
                inputVariables.put(inputClause.getInputExpression().getText(), Boolean.FALSE);
            }
        }

        // check if there are output expressions that refer to none existing input variables
        // in that case create them with default values
        for (OutputClause outputClause : decisionTable.getOutputs()) {
            if (!inputVariables.containsKey(outputClause.getName()) || inputVariables.get(outputClause.getName()) == null) {
                if ("number".equals(outputClause.getTypeRef())) {
                    inputVariables.put(outputClause.getName(), 0D);
                } else if ("date".equals(outputClause.getTypeRef())) {
                    inputVariables.put(outputClause.getName(), new Date());
                } else {
                    inputVariables.put(outputClause.getName(), "");
                }
            }
        }

        // check if transformation is needed
        for (Map.Entry<String, Object> inputVariable : inputVariables.entrySet()) {
            try {
                if (inputVariable.getValue() instanceof LocalDate) {
                    Date transformedDate = ((LocalDate) inputVariable.getValue()).toDate();
                    inputVariables.put(inputVariable.getKey(), transformedDate);
                }
            } catch (Exception ex) {
                throw new FlowableException("error while transforming variable", ex);
            }
        }
    }
}

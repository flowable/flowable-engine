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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.api.DecisionExecutionAuditContainer;
import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.engine.common.api.FlowableException;
import org.mvel2.ParserContext;
import org.mvel2.integration.PropertyHandler;

/**
 * @author Yvo Swillens
 */
public class MvelExecutionContext {

    protected Map<Integer, Map<String, Object>> ruleResults = new LinkedHashMap<>();
    protected List<Map<String, Object>> decisionResults;
    protected Map<String, Object> stackVariables;
    protected ParserContext parserContext;
    protected Map<Class<?>, PropertyHandler> propertyHandlers = new HashMap<>();
    protected DecisionExecutionAuditContainer auditContainer;
    protected Map<String, List<Object>> outputValues = new HashMap<>();
    protected BuiltinAggregator aggregator;

    public void checkExecutionContext(String variableId) {

        if (StringUtils.isEmpty(variableId)) {
            throw new IllegalArgumentException("Variable id cannot be empty");
        }

        if (stackVariables == null || stackVariables.isEmpty()) {
            throw new IllegalArgumentException("Variables cannot be empty when variable id: " + variableId + " is used");
        }

        if (variableId.contains(".")) {
            String rootVariableId = variableId.substring(0, variableId.indexOf('.'));
            if (!stackVariables.containsKey(rootVariableId)) {
                throw new FlowableException("referred id: " + rootVariableId + " is not present on the context");
            }

        } else if (!stackVariables.containsKey(variableId)) {
            throw new FlowableException("referred id: " + variableId + " is not present on the context");
        }
    }

    public void addRuleResult(int ruleNumber, String outputName, Object outputValue) {
        Map ruleResult;
        if (ruleResults.containsKey(ruleNumber)) {
            ruleResult = ruleResults.get(ruleNumber);
        } else {
            ruleResult = new HashMap();
            ruleResults.put(ruleNumber, ruleResult);
        }
        ruleResult.put(outputName, outputValue);
    }

    public void setStackVariables(Map<String, Object> variables) {
        this.stackVariables = variables;
    }

    public Map<String, Object> getStackVariables() {
        return stackVariables;
    }

    public Map<String, Object> getRuleResult(int ruleNumber) {
        return ruleResults.get(ruleNumber);
    }

    public Map<Integer, Map<String, Object>> getRuleResults() {
        return ruleResults;
    }

    public ParserContext getParserContext() {
        return parserContext;
    }

    public void setParserContext(ParserContext parserContext) {
        this.parserContext = parserContext;
    }

    public Map<Class<?>, PropertyHandler> getPropertyHandlers() {
        return propertyHandlers;
    }

    public void addPropertyHandler(Class<?> variableClass, PropertyHandler propertyHandler) {
        propertyHandlers.put(variableClass, propertyHandler);
    }

    public DecisionExecutionAuditContainer getAuditContainer() {
        return auditContainer;
    }

    public void setAuditContainer(DecisionExecutionAuditContainer auditContainer) {
        this.auditContainer = auditContainer;
    }

    public Map<String, List<Object>> getOutputValues() {
        return outputValues;
    }

    public void addOutputValues(String outputName, List<Object> outputValues) {
        this.outputValues.put(outputName, outputValues);
    }

    public void setDecisionResults(List<Map<String, Object>> decisionResults) {
        this.decisionResults = decisionResults;
    }

    public List<Map<String, Object>> getDecisionResults() {
        return decisionResults;
    }

    public BuiltinAggregator getAggregator() {
        return aggregator;
    }

    public void setAggregator(BuiltinAggregator aggregator) {
        this.aggregator = aggregator;
    }
}

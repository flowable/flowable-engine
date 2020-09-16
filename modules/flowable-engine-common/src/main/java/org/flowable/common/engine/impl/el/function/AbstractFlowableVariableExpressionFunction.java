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
package org.flowable.common.engine.impl.el.function;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstFunction;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstIdentifier;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstNode;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstParameters;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.ast.AstText;
import org.flowable.common.engine.impl.el.FlowableAstFunctionCreator;
import org.flowable.common.engine.impl.el.FlowableExpressionParser;
import org.flowable.common.engine.impl.el.VariableContainerELResolver;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class AbstractFlowableVariableExpressionFunction implements FlowableAstFunctionCreator, FlowableFunctionDelegate {
    
    private static final List<String> FUNCTION_PREFIXES = Arrays.asList("variables", "vars", "var");
    
    protected Method method;
    protected String functionName;
    protected Collection<String> functionNamesOptions;
    protected String variableScopeName = VariableContainerELResolver.VARIABLE_CONTAINER_KEY;

    public AbstractFlowableVariableExpressionFunction(String functionName) {
        this(Collections.singletonList(functionName), functionName);
    }
    
    public AbstractFlowableVariableExpressionFunction(List<String> functionNameOptions, String functionName) {
        this.functionNamesOptions = new LinkedHashSet<>(functionNameOptions);
        this.functionName = functionName;

    }

    protected Method findMethod(String functionName) {
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) && method.getName().equals(functionName)) {
                return method;
            }
        }
        throw new FlowableException("Programmatic error: could not find method " + functionName + " on class " + this.getClass());
    }

    @Override
    public String prefix() {
        throw new UnsupportedOperationException("Function has more than one prefix");
    }

    @Override
    public Collection<String> prefixes() {
        return FUNCTION_PREFIXES;
    }

    @Override
    public String localName() {
        throw new UnsupportedOperationException("Function has more than one local name");
    }

    @Override
    public Collection<String> localNames() {
        return functionNamesOptions;
    }

    @Override
    public Method functionMethod() {
        if (method != null) {
            return method;
        }

        method = findMethod(functionName);
        return method;
    }

    // Helper methods
    
    protected static Object getVariableValue(VariableContainer variableContainer, String variableName) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("Variable name passed is null");
        }
        return variableContainer.getVariable(variableName);
    }
    
    protected static boolean valuesAreNumbers(Object variableValue, Object actualValue) {
        return actualValue instanceof Number && variableValue instanceof Number;
    }

    @Override
    public Collection<String> getFunctionNames() {
        Collection<String> functionNames = new LinkedHashSet<>();
        for (String functionPrefix : prefixes()) {
            for (String functionNameOption : localNames()) {
                functionNames.add(functionPrefix + ":" + functionNameOption);
            }
        }

        return functionNames;
    }

    @Override
    public AstFunction createFunction(String name, int index, AstParameters parameters, boolean varargs, FlowableExpressionParser parser) {
        Method method = functionMethod();
        int parametersCardinality = parameters.getCardinality();
        int methodParameterCount = method.getParameterCount();
        if (method.isVarArgs() || parametersCardinality < methodParameterCount) {
            // If the method is a varargs or the number of parameters is less than the defined in the method
            // then create an identifier for the variableContainer
            // and analyze the parameters
            List<AstNode> newParameters = new ArrayList<>(parametersCardinality + 1);
            newParameters.add(parser.createIdentifier(variableScopeName));

            if (methodParameterCount >= 1) {
                // If the first parameter is an identifier we have to convert it to a text node
                // We want to allow writing variables:get(varName) where varName is without quotes
                newParameters.add(createVariableNameNode(parameters.getChild(0)));
                for (int i = 1; i < parametersCardinality; i++) {
                    // the rest of the parameters should be treated as is
                    newParameters.add(parameters.getChild(i));
                }
            }

            return new AstFunction(name, index, new AstParameters(newParameters), varargs);
        } else {
            // If the resolved parameters are of the same size as the current method then nothing to do
            return new AstFunction(name, index, parameters, varargs);
        }
    }

    protected AstNode createVariableNameNode(AstNode variableNode) {
        if (variableNode instanceof AstIdentifier) {
            return new AstText(((AstIdentifier) variableNode).getName());
        } else {
            return variableNode;
        }
    }
}

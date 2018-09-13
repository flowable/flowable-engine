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

import java.util.List;

import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public abstract class AbstractVariableComparatorExpressionFunction extends AbstractFlowableVariableExpressionFunction {
    
    protected static enum OPERATOR { LT, LTE, GT, GTE, EQ };

    public AbstractVariableComparatorExpressionFunction(String variableScopeName, List<String> functionNameOptions, String functionName) {
        super(variableScopeName, functionNameOptions, functionName);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static boolean compareVariableValue(VariableScope variableScope, String variableName, Object comparedValue, OPERATOR operator) {
        
        Object variableValue = getVariableValue(variableScope, variableName);
        if (comparedValue != null && variableValue != null) {

            // See equals method for an explanation why Number instances are handled specifically
            if (valuesAreNumbers(comparedValue, variableValue)) {
                if (variableValue instanceof Long) {
                    
                    long longVariableValue = ((Number) variableValue).longValue();
                    long longComparedValue =((Number) comparedValue).longValue();
                    if (operator == OPERATOR.LT) {
                        return longVariableValue < longComparedValue;                        
                    } else if (operator == OPERATOR.LTE) {
                        return longVariableValue <= longComparedValue;
                    } else if (operator == OPERATOR.GT) {
                        return longVariableValue > longComparedValue;
                    } else if (operator == OPERATOR.GTE) {
                        return longVariableValue >= longComparedValue;
                    }

                } else if(variableValue instanceof Integer) {
                    
                    int intVariableValue = ((Number) variableValue).intValue();
                    int intComparedValue =((Number) comparedValue).intValue();
                    if (operator == OPERATOR.LT) {
                        return intVariableValue < intComparedValue;                        
                    } else if (operator == OPERATOR.LTE) {
                        return intVariableValue <= intComparedValue;
                    } else if (operator == OPERATOR.GT) {
                        return intVariableValue > intComparedValue;
                    } else if (operator == OPERATOR.GTE) {
                        return intVariableValue >= intComparedValue;
                    }
                    
                } else if(variableValue instanceof Double) {
                    
                    double doubleVariableValue = ((Number) variableValue).doubleValue();
                    double doubleComparedValue =((Number) comparedValue).doubleValue();
                    if (operator == OPERATOR.LT) {
                        return doubleVariableValue < doubleComparedValue;                        
                    } else if (operator == OPERATOR.LTE) {
                        return doubleVariableValue <= doubleComparedValue;
                    } else if (operator == OPERATOR.GT) {
                        return doubleVariableValue > doubleComparedValue;
                    } else if (operator == OPERATOR.GTE) {
                        return doubleVariableValue >= doubleComparedValue;
                    }
                    
                } else if(variableValue instanceof Float) {
                    
                    float floatVariableValue = ((Number) variableValue).floatValue();
                    float floatComparedValue =((Number) comparedValue).floatValue();
                    if (operator == OPERATOR.LT) {
                        return floatVariableValue < floatComparedValue;                        
                    } else if (operator == OPERATOR.LTE) {
                        return floatVariableValue <= floatComparedValue;
                    } else if (operator == OPERATOR.GT) {
                        return floatVariableValue > floatComparedValue;
                    } else if (operator == OPERATOR.GTE) {
                        return floatVariableValue >= floatComparedValue;
                    }
                    
                } else if(variableValue instanceof Short) {
                    
                    short shortVariableValue = ((Number) variableValue).shortValue();
                    short shortComparedValue =((Number) comparedValue).shortValue();
                    if (operator == OPERATOR.LT) {
                        return shortVariableValue < shortComparedValue;                        
                    } else if (operator == OPERATOR.LTE) {
                        return shortVariableValue <= shortComparedValue;
                    } else if (operator == OPERATOR.GT) {
                        return shortVariableValue > shortComparedValue;
                    } else if (operator == OPERATOR.GTE) {
                        return shortVariableValue >= shortComparedValue;
                    }
                    
                } // Other subtypes possible (e.g. BigDecimal, AtomicInteger, etc.), will fall back to default comparison
                
            }
            
            if (variableValue instanceof Comparable && comparedValue instanceof Comparable) {
                if (operator == OPERATOR.LT) {
                    return ((Comparable) variableValue).compareTo((Comparable) comparedValue) < 0;                      
                } else if (operator == OPERATOR.LTE) {
                    return ((Comparable) variableValue).compareTo((Comparable) comparedValue) <= 0;
                } else if (operator == OPERATOR.GT) {
                    return ((Comparable) variableValue).compareTo((Comparable) comparedValue) > 0;
                } else if (operator == OPERATOR.GTE) {
                    return ((Comparable) variableValue).compareTo((Comparable) comparedValue) >= 0;
                }
            }
            
        }
        
        // if any of the values is null, return false;
        return false;
    }

}

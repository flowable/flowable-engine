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
package org.flowable.cmmn.engine.impl.el;

import java.util.Objects;

import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.delegate.FlowableExpressionEnhancer;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * A class containing the static functions that can be used in an {@link Expression}.
 * 
 * To register a custom function:
 * 
 * - Create the appropriate {@link FlowableFunctionDelegate} and register it with the {@link ExpressionManager}. 
 *   The function delegate will link to this class thtrough reflection.
 * - Create the appropriate {@link FlowableExpressionEnhancer} that enhances function shorthand to their real function call (if needed).  
 * 
 * @author Joram Barrez
 */
public class VariableExpressionFunctionsUtil {
    
    private static enum OPERATOR { LT, LTE, GT, GTE, EQ };
    
    /**
     * Compares the value of a variable (fetched using the variableName through the {@link PlanItemInstance})
     * with a value on equality. If the variable value is null, false is returned (unless compared to null). 
     */
    public static boolean equals(PlanItemInstance planItemInstance, String variableName, Object comparedValue) {
        
        Object variableValue = getVariableValue(planItemInstance, variableName);
        if (comparedValue != null && variableValue != null) {
            
            // Numbers are not necessarily of the expected type due to coming from JUEL, 
            // (eg. variable can be an Integer but JUEL passes a Long). 
            // As such, a specific check for the supported Number variable types of Flowable is done.
            
            if (valuesAreNumbers(comparedValue, variableValue)) {
                if (variableValue instanceof Long) {
                    return ((Number) variableValue).longValue() == ((Number) comparedValue).longValue();

                } else if(variableValue instanceof Integer) {
                    return ((Number) variableValue).intValue() == ((Number) comparedValue).intValue();
                    
                } else if(variableValue instanceof Double) {
                    return ((Number) variableValue).doubleValue() == ((Number) comparedValue).doubleValue();
                    
                } else if(variableValue instanceof Float) {
                    return ((Number) variableValue).floatValue() == ((Number) comparedValue).floatValue();
                    
                } else if(variableValue instanceof Short) {
                    return ((Number) variableValue).shortValue() == ((Number) comparedValue).shortValue();
                    
                } // Other subtypes possible (e.g. BigDecimal, AtomicInteger, etc.), will fall back to default comparison
                
            }
            
        }
        
        return defaultEquals(comparedValue, variableValue);
    }

    protected static boolean defaultEquals(Object variableValue, Object actualValue) {
        return Objects.equals(actualValue, variableValue);
    }
    
    /**
     * Compares the value of a variable (fetched using the variableName through the {@link PlanItemInstance})
     * with a value on inequality. If the variable value is null, false is returned (unless compared to null). 
     */
    public static boolean notEquals(PlanItemInstance planItemInstance, String variableName, Object variableValue) {
        
        // Special handling for null: when the variable is null, false is returned.
        // This is similar to equals, where a null variable value will always return false 
        // (it's effectively ignored) - unless it's compared to null itself)
        
        Object actualValue = getVariableValue(planItemInstance, variableName);
        if (actualValue != null) {
            return !equals(planItemInstance, variableName, variableValue);
        }
        
        return false;
    }
    
    /**
     * Returns whether or not a variable with the given name exists when fetched through the provided {@link PlanItemInstance}.
     */
    public static boolean exists(PlanItemInstance planItemInstance, String variableName) {
        return getVariableValue(planItemInstance, variableName) != null;
    }
    
    public static boolean lowerThan(PlanItemInstance planItemInstance, String variableName, Object comparedValue) {
        return compareVariableValue(planItemInstance, variableName, comparedValue, OPERATOR.LT);
    }
    
    public static boolean lowerThanOrEquals(PlanItemInstance planItemInstance, String variableName, Object comparedValue) {
        return compareVariableValue(planItemInstance, variableName, comparedValue, OPERATOR.LTE);
    }
    
    public static boolean greaterThan(PlanItemInstance planItemInstance, String variableName, Object comparedValue) {
        return compareVariableValue(planItemInstance, variableName, comparedValue, OPERATOR.GT);
    }
    
    public static boolean greaterThanOrEquals(PlanItemInstance planItemInstance, String variableName, Object comparedValue) {
        return compareVariableValue(planItemInstance, variableName, comparedValue, OPERATOR.GTE);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static boolean compareVariableValue(PlanItemInstance planItemInstance, String variableName, Object comparedValue, OPERATOR operator) {
        
        Object variableValue = getVariableValue(planItemInstance, variableName);
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
    
    protected static Object getVariableValue(PlanItemInstance planItemInstance, String variableName) {
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("Variable name passed is null");
        }
        return ((VariableScope) planItemInstance).getVariable((String) variableName);
    }

    protected static boolean valuesAreNumbers(Object variableValue, Object actualValue) {
        return actualValue instanceof Number && variableValue instanceof Number;
    }

}

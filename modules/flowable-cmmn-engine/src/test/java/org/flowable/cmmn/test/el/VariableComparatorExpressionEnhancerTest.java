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
package org.flowable.cmmn.test.el;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.flowable.common.engine.api.delegate.FlowableExpressionEnhancer;
import org.flowable.common.engine.impl.el.function.VariableGreaterThanExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableGreaterThanOrEqualsExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableLowerThanExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableLowerThanOrEqualsExpressionFunction;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class VariableComparatorExpressionEnhancerTest {
    
    private VariableLowerThanExpressionFunction variableLowerThanExpressionFunction = new VariableLowerThanExpressionFunction("planItemInstance");
    private VariableLowerThanOrEqualsExpressionFunction variableLowerThanOrEqualsExpressionFunction = new VariableLowerThanOrEqualsExpressionFunction("planItemInstance");
    private VariableGreaterThanExpressionFunction variableGreaterThanExpressionFunction = new VariableGreaterThanExpressionFunction("planItemInstance"); 
    private VariableGreaterThanOrEqualsExpressionFunction variableGreaterThanOrEqualsExpressionFunction = new  VariableGreaterThanOrEqualsExpressionFunction("planItemInstance");  
    
    @Test
    public void testRegexNameReplacement() {
        assertRegexCorrect(variableLowerThanExpressionFunction, "${variables:lowerThan(myVar,123)}", "${variables:lowerThan(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableLowerThanExpressionFunction, "${variables:lessThan(myVar,123)}", "${variables:lowerThan(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableLowerThanExpressionFunction, "${variables:lt(myVar,123)}", "${variables:lowerThan(planItemInstance,'myVar',123)}");
        
        assertRegexCorrect(variableLowerThanOrEqualsExpressionFunction, "${variables:lowerThanOrEquals(myVar,123)}", "${variables:lowerThanOrEquals(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableLowerThanOrEqualsExpressionFunction, "${variables:lessThanOrEquals(myVar,123)}", "${variables:lowerThanOrEquals(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableLowerThanOrEqualsExpressionFunction,  "${variables:lte(myVar,123)}", "${variables:lowerThanOrEquals(planItemInstance,'myVar',123)}");
        
        assertRegexCorrect(variableGreaterThanExpressionFunction, "${variables:greaterThan(myVar,123)}", "${variables:greaterThan(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableGreaterThanExpressionFunction, "${variables:gt(myVar,123)}", "${variables:greaterThan(planItemInstance,'myVar',123)}");
        
        assertRegexCorrect(variableGreaterThanOrEqualsExpressionFunction,"${variables:greaterThanOrEquals(myVar,123)}", "${variables:greaterThanOrEquals(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableGreaterThanOrEqualsExpressionFunction, "${variables:gte(myVar,123)}", "${variables:greaterThanOrEquals(planItemInstance,'myVar',123)}");
    }
        
    public void assertRegexCorrect(FlowableExpressionEnhancer expressionEnhancer, String in, String out) {
        assertEquals(out, expressionEnhancer.enhance(in));
    }

}

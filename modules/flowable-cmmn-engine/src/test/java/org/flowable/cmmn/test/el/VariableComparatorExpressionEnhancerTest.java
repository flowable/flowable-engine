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

import org.flowable.cmmn.engine.impl.el.VariableGreaterThanExpressionEnhancer;
import org.flowable.cmmn.engine.impl.el.VariableGreaterThanOrEqualsExpressionEnhancer;
import org.flowable.cmmn.engine.impl.el.VariableLowerThanExpressionEnhancer;
import org.flowable.cmmn.engine.impl.el.VariableLowerThanOrEqualsExpressionEnhancer;
import org.flowable.common.engine.api.delegate.FlowableExpressionEnhancer;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class VariableComparatorExpressionEnhancerTest {
    
    private VariableLowerThanExpressionEnhancer variableLowerThanExpressionEnhancer = new VariableLowerThanExpressionEnhancer();
    private VariableLowerThanOrEqualsExpressionEnhancer variableLowerThanOrEqualsExpressionEnhancer = new VariableLowerThanOrEqualsExpressionEnhancer();
    private VariableGreaterThanExpressionEnhancer variableGreaterThanExpressionEnhancer = new VariableGreaterThanExpressionEnhancer(); 
    private VariableGreaterThanOrEqualsExpressionEnhancer variableGreaterThanOrEqualsExpressionEnhancer = new  VariableGreaterThanOrEqualsExpressionEnhancer();  
    
    @Test
    public void testRegexNameReplacement() {
        assertRegexCorrect(variableLowerThanExpressionEnhancer, "${variables:lowerThan(myVar,123)}", "${variables:lowerThan(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableLowerThanExpressionEnhancer, "${variables:lt(myVar,123)}", "${variables:lowerThan(planItemInstance,'myVar',123)}");
        
        assertRegexCorrect(variableLowerThanOrEqualsExpressionEnhancer, "${variables:lowerThanOrEquals(myVar,123)}", "${variables:lowerThanOrEquals(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableLowerThanOrEqualsExpressionEnhancer,  "${variables:lte(myVar,123)}", "${variables:lowerThanOrEquals(planItemInstance,'myVar',123)}");
        
        assertRegexCorrect(variableGreaterThanExpressionEnhancer, "${variables:greaterThan(myVar,123)}", "${variables:greaterThan(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableGreaterThanExpressionEnhancer, "${variables:gt(myVar,123)}", "${variables:greaterThan(planItemInstance,'myVar',123)}");
        
        assertRegexCorrect(variableGreaterThanOrEqualsExpressionEnhancer,"${variables:greaterThanOrEquals(myVar,123)}", "${variables:greaterThanOrEquals(planItemInstance,'myVar',123)}");
        assertRegexCorrect(variableGreaterThanOrEqualsExpressionEnhancer, "${variables:gte(myVar,123)}", "${variables:greaterThanOrEquals(planItemInstance,'myVar',123)}");
    }
        
    public void assertRegexCorrect(FlowableExpressionEnhancer expressionEnhancer, String in, String out) {
        assertEquals(out, expressionEnhancer.enhance(in));
    }

}

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

import org.flowable.common.engine.impl.el.function.VariableExistsExpressionFunction;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class VariableExistsExpressionEnhancerTest {
    
    private VariableExistsExpressionFunction expressionFunction = new VariableExistsExpressionFunction("planItemInstance");
    
    @Test
    public void testRegexNameReplacement() {
        assertRegexCorrect("${variables:exists(myVar)}", "${variables:exists(planItemInstance,'myVar')}");
        assertRegexCorrect("${variables:exist(myVar)}", "${variables:exists(planItemInstance,'myVar')}");
        
        assertRegexCorrect("${variables:exists('myVar')}", "${variables:exists(planItemInstance,'myVar')}");
    }
        
    public void assertRegexCorrect(String in, String out) {
        assertEquals(out, expressionFunction.enhance(in));
    }

}

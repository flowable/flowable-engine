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

import org.flowable.common.engine.impl.el.function.VariableGetExpressionFunction;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class VariableGetExpressionEnhancerTest {
    
    private VariableGetExpressionFunction expressionFunction = new VariableGetExpressionFunction("planItemInstance");
    
    @Test
    public void testRegexDefault() {
        assertRegexCorrect("${variables:get(myVar)}", "${variables:get(planItemInstance,'myVar')}");
        assertRegexCorrect("${var:get('form_someForm_outcome') == 'close'}", "${variables:get(planItemInstance,'form_someForm_outcome') == 'close'}");
    }

    public void assertRegexCorrect(String in, String out) {
        assertEquals(out, expressionFunction.enhance(in));
    }

}

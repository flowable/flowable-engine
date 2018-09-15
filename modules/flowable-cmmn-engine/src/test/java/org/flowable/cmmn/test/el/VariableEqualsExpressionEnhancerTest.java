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

import org.flowable.common.engine.impl.el.function.VariableEqualsExpressionFunction;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class VariableEqualsExpressionEnhancerTest {
    
    private VariableEqualsExpressionFunction expressionFunction = new VariableEqualsExpressionFunction("planItemInstance");
    
    @Test
    public void testRegexDefault() {
        assertRegexCorrect("${variables:equals(myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
    }
        
    @Test
    public void testRegexAlternativeNameHandling() {
        assertRegexCorrect("${variables:eq(myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${vars:equals(myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${vars:eq(myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${var:equals(myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${var:eq(myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
    }
    
    @Test
    public void testRegexQuoteHandling() {
        assertRegexCorrect("${variables:equals('myVar',123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals(\"myVar\",123)}", "${variables:equals(planItemInstance,'myVar',123)}");
    }
    
    @Test
    public void testRegexSpaceHandling() {
        assertRegexCorrect("${variables:equals (myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals    (myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals( myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals(      myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals(myVar ,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals(myVar     ,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals ( myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals    (     myVar,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals    (     myVar   ,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        
        // Spaces and quotes
        assertRegexCorrect("${variables:equals    (     'myVar'   ,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
        assertRegexCorrect("${variables:equals    (     \"myVar\"   ,123)}", "${variables:equals(planItemInstance,'myVar',123)}");
    }
    
    @Test
    public void testRegexMultipleUsages() {
        assertRegexCorrect("${variables:equals(myVar,123) && var:eq ( otherVar , 456)}", "${variables:equals(planItemInstance,'myVar',123) && variables:equals(planItemInstance,'otherVar', 456)}");
        assertRegexCorrect("${(var:eq(myVar,123) && var:eq(otherVar,456)) || var:eq(myVar,789)}", 
                "${(variables:equals(planItemInstance,'myVar',123) && variables:equals(planItemInstance,'otherVar',456)) || variables:equals(planItemInstance,'myVar',789)}");
    }
    
    public void assertRegexCorrect(String in, String out) {
        assertEquals(out, expressionFunction.enhance(in));
    }

}

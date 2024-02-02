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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class VariableEqualsFunctionTest extends FlowableCmmnTestCase {

    @Test
    public void testAlternativeNameHandling() {
        Map<String, Object> variables = Collections.singletonMap("myVar", 123);
        VariableContainer variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:equals(myVar,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:eq(myVar,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${vars:equals(myVar,124)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:eq(myVar,100)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:equals(myVar,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:eq(myVar,123)}", variableContainer)).isEqualTo(true);

        variables = Collections.emptyMap();
        variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:eq(myVar,123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:equals(myVar,123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:eq(myVar,123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:equals(myVar,123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:eq(myVar,123)}", variableContainer)).isEqualTo(false);

    }

    @Test
    public void testQuoteHandling() {
        Map<String, Object> variables = Collections.singletonMap("myVar", 123);
        VariableContainer variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:equals('myVar',123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals(\"myVar\",123)}", variableContainer)).isEqualTo(true);

        variables = Collections.emptyMap();
        variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:equals('myVar',123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${variables:equals(\"myVar\",123)}", variableContainer)).isEqualTo(false);
    }

    @Test
    public void testSpaceHandling() {
        Map<String, Object> variables = Collections.singletonMap("myVar", 123);
        VariableContainer variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:equals (myVar,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals    (myVar,100)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${variables:equals( myVar,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals(      myVar,100)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${variables:equals(myVar ,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals(myVar     ,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals ( myVar,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals    (     myVar,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals    (     myVar   ,100)}", variableContainer)).isEqualTo(false);

        // Spaces and quotes
        assertThat(executeExpression("${variables:equals    (     'myVar'   ,123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:equals    (     \"myVar\"   ,123)}", variableContainer)).isEqualTo(true);
    }

    @Test
    public void testMultipleUsages() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVar", 123);
        variables.put("otherVar", 456);
        VariableContainer variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${variables:equals(myVar,123) && var:eq ( otherVar , 456)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${(var:eq(myVar,123) && var:eq(otherVar,456)) || var:eq(myVar,789)}", variableContainer)).isEqualTo(true);
    }

    protected Object executeExpression(String expression, VariableContainer variableContainer) {
        return cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> cmmnEngineConfiguration.getExpressionManager()
                        .createExpression(expression)
                        .getValue(variableContainer));
    }

}

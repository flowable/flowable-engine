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
public class VariableGetFunctionTest extends FlowableCmmnTestCase {

    @Test
    public void testGetDefault() {
        Map<String, Object> variables = Collections.singletonMap("myVar", "close");
        VariableContainer variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${variables:get(myVar)}", variableContainer)).isEqualTo("close");
        assertThat(executeExpression("${vars:get(myVar)}", variableContainer)).isEqualTo("close");
        assertThat(executeExpression("${var:get(myVar)}", variableContainer)).isEqualTo("close");
        assertThat(executeExpression("${var:get('form_someForm_outcome') == 'close'}", variableContainer)).isEqualTo(false);

        variableContainer = new VariableContainerWrapper(Collections.singletonMap("form_someForm_outcome", "close"));

        assertThat(executeExpression("${variables:get(myVar)}", variableContainer)).isNull();
        assertThat(executeExpression("${vars:get(myVar)}", variableContainer)).isNull();
        assertThat(executeExpression("${var:get(myVar)}", variableContainer)).isNull();
        assertThat(executeExpression("${var:get('form_someForm_outcome') == 'close'}", variableContainer)).isEqualTo(true);
    }

    @Test
    public void testGetNested() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("variableName", "myVar");
        variables.put("myVar", "some value");
        VariableContainer variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${variables:get(var:get(variableName))}", variableContainer)).isEqualTo("some value");
    }

    protected Object executeExpression(String expression, VariableContainer variableContainer) {
        return cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> cmmnEngineConfiguration.getExpressionManager()
                        .createExpression(expression)
                        .getValue(variableContainer));
    }

}

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
import java.util.Map;

import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class VariableExistsFunctionTest extends FlowableCmmnTestCase {

    @Test
    public void testExists() {
        Map<String, Object> variables = Collections.singletonMap("myVar", 123);
        VariableContainer variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:exists(myVar)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:exist(myVar)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${vars:exists(myVar)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${vars:exist(myVar)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:exists(myVar)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:exist(myVar)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${variables:exists('myVar')}", variableContainer)).isEqualTo(true);

        variableContainer = new VariableContainerWrapper(Collections.singletonMap("myVar", null));
        assertThat(executeExpression("${variables:exists(myVar)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:exist(myVar)}", variableContainer)).isEqualTo(false);

        variableContainer = new VariableContainerWrapper(Collections.emptyMap());
        assertThat(executeExpression("${variables:exist(myVar)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:exists(myVar)}", variableContainer)).isEqualTo(false);
    }

    protected Object executeExpression(String expression, VariableContainer variableContainer) {
        return cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> cmmnEngineConfiguration.getExpressionManager()
                        .createExpression(expression)
                        .getValue(variableContainer));
    }

}

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
public class VariableComparatorFunctionTest extends FlowableCmmnTestCase {

    @Test
    public void testLowerThan() {

        Map<String, Object> variables = Collections.singletonMap("myVar", 100);
        VariableContainerWrapper variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:lowerThan(myVar, 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:lessThan(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${variables:lt(myVar, 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${vars:lowerThan(myVar, 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${vars:lessThan(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:lt(myVar, 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:lowerThan(myVar, 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThan(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lt(myVar, 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:lowerThan('myVar', 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThan('myVar', 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lt('myVar', 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:lowerThan(\"myVar\", 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThan(\"myVar\", 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lt(\"myVar\", 123)}", variableContainer)).isEqualTo(true);

        variableContainer = new VariableContainerWrapper(Collections.emptyMap());

        assertThat(executeExpression("${var:lowerThan(myVar, 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lessThan(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lt(myVar, 123)}", variableContainer)).isEqualTo(false);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.singletonMap("myVar", 100)));
        variables.put("myVar", 500);
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:lowerThan(container, 'myVar', 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThan(container, 'myVar', 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lt(container, 'myVar', 100)}", variableContainer)).isEqualTo(false);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.emptyMap()));
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:lowerThan(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lessThan(container, 'myVar', 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lt(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
    }

    @Test
    public void testLowerThanOrEquals() {

        Map<String, Object> variables = Collections.singletonMap("myVar", 123);
        VariableContainerWrapper variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:lowerThanOrEquals(myVar, 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:lessThanOrEquals(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${variables:lte(myVar, 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${vars:lowerThanOrEquals(myVar, 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${vars:lessThanOrEquals(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:lte(myVar, 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:lowerThanOrEquals(myVar, 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThanOrEquals(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lte(myVar, 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:lowerThanOrEquals('myVar', 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThanOrEquals('myVar', 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lte('myVar', 123)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:lowerThanOrEquals(\"myVar\", 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThanOrEquals(\"myVar\", 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lte(\"myVar\", 123)}", variableContainer)).isEqualTo(true);

        variableContainer = new VariableContainerWrapper(Collections.emptyMap());

        assertThat(executeExpression("${var:lowerThanOrEquals(myVar, 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lessThanOrEquals(myVar, 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lte(myVar, 123)}", variableContainer)).isEqualTo(false);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.singletonMap("myVar", 100)));
        variables.put("myVar", 500);
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:lowerThanOrEquals(container, 'myVar', 100)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:lessThanOrEquals(container, 'myVar', 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lte(container, 'myVar', 123)}", variableContainer)).isEqualTo(true);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.emptyMap()));
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:lowerThanOrEquals(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lessThanOrEquals(container, 'myVar', 50)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:lte(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
    }

    @Test
    public void testGreaterThan() {

        Map<String, Object> variables = Collections.singletonMap("myVar", 100);
        VariableContainerWrapper variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:greaterThan(myVar, 50)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:gt(myVar, 100)}", variableContainer)).isEqualTo(false);

        assertThat(executeExpression("${vars:greaterThan(myVar, 50)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${vars:gt(myVar, 90)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:greaterThan(myVar, 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gt(myVar, 50)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:greaterThan('myVar', 50)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:gt('myVar', 80)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:greaterThan(\"myVar\", 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gt(\"myVar\", 50)}", variableContainer)).isEqualTo(true);

        variableContainer = new VariableContainerWrapper(Collections.emptyMap());

        assertThat(executeExpression("${var:greaterThan(myVar, 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gt(myVar, 50)}", variableContainer)).isEqualTo(false);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.singletonMap("myVar", 500)));
        variables.put("myVar", 100);
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:greaterThan(container, 'myVar', 123)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${var:gt(container, 'myVar', 123)}", variableContainer)).isEqualTo(true);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.emptyMap()));
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:greaterThan(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gt(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
    }

    @Test
    public void testGreaterThanOrEquals() {

        Map<String, Object> variables = Collections.singletonMap("myVar", 100);
        VariableContainerWrapper variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("${variables:greaterThanOrEquals(myVar, 50)}", variableContainer)).isEqualTo(true);
        assertThat(executeExpression("${variables:gte(myVar, 100)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${vars:greaterThanOrEquals(myVar, 150)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${vars:gte(myVar, 100)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:greaterThanOrEquals(myVar, 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gte(myVar, 100)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:greaterThanOrEquals('myVar', 150)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gte('myVar', 100)}", variableContainer)).isEqualTo(true);

        assertThat(executeExpression("${var:greaterThanOrEquals(\"myVar\", 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gte(\"myVar\", 100)}", variableContainer)).isEqualTo(true);

        variableContainer = new VariableContainerWrapper(Collections.emptyMap());

        assertThat(executeExpression("${var:greaterThanOrEquals(myVar, 100)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gte(myVar, 150)}", variableContainer)).isEqualTo(false);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.singletonMap("myVar", 100)));
        variables.put("myVar", 500);
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:greaterThanOrEquals(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gte(container, 'myVar', 100)}", variableContainer)).isEqualTo(true);

        variables = new HashMap<>();
        variables.put("container", new VariableContainerWrapper(Collections.emptyMap()));
        variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${var:greaterThanOrEquals(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
        assertThat(executeExpression("${var:gte(container, 'myVar', 123)}", variableContainer)).isEqualTo(false);
    }

    protected Object executeExpression(String expression, VariableContainer variableContainer) {
        return cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> cmmnEngineConfiguration.getExpressionManager()
                        .createExpression(expression)
                        .getValue(variableContainer));
    }

}

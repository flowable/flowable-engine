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
 * @author Filip Hrisafov
 */
public class VariableGetOrDefaultFunctionTest extends FlowableCmmnTestCase {

    @Test
    public void testGetOrDefaultDefault() {
        Map<String, Object> variables = Collections.singletonMap("myVar", "close");
        VariableContainer variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${variables:getOrDefault(myVar, 'test')}", variableContainer)).isEqualTo("close");
        assertThat(executeExpression("${vars:getOrDefault(myVar, 'test')}", variableContainer)).isEqualTo("close");
        assertThat(executeExpression("${var:getOrDefault(myVar, 'test')}", variableContainer)).isEqualTo("close");
        assertThat(executeExpression("${var:getOrDefault('form_someForm_outcome', 'test') == 'close'}", variableContainer)).isEqualTo(false);

        variableContainer = new VariableContainerWrapper(Collections.singletonMap("form_someForm_outcome", "close"));

        assertThat(executeExpression("${variables:getOrDefault(myVar, 'test')}", variableContainer)).isEqualTo("test");
        assertThat(executeExpression("${vars:getOrDefault(myVar, 'test')}", variableContainer)).isEqualTo("test");
        assertThat(executeExpression("${var:getOrDefault(myVar, 'test')}", variableContainer)).isEqualTo("test");
        assertThat(executeExpression("${var:getOrDefault('form_someForm_outcome', 'test') == 'close'}", variableContainer)).isEqualTo(true);
    }

    @Test
    public void testGetOrDefaultMultiExpressions() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("loadRef", null);
        VariableContainer variableContainer = new VariableContainerWrapper(variables);
        assertThat(executeExpression("<ul>\n"
                + "        <li><b>Reference:</b>${vars:getOrDefault(loadRef, 'unknown')}</li>\n"
                + "        <li><b>Currency:</b>${vars:getOrDefault(currencyRef, 'unknown')} (looked up from first loads LoadCostCurrency.)</ls>\n"
                + "        <li><b>Customer:</b>${vars:getOrDefault(custRef, 'unknown')} (looked up from load.CustomerId)</li>\n"
                + "        <li><b>Vendor:</b>${vars:getOrDefault(vendorRef, 'unknown')} (looked up from load.HaulierId)</li>\n"
                + "        <li><b>Item:</b>${vars:getOrDefault(itemRef, 'unknown')} (ref data based on companyId)</li>\n"
                + "        </ul>", variableContainer))
                .isEqualTo("<ul>\n"
                        + "        <li><b>Reference:</b>unknown</li>\n"
                        + "        <li><b>Currency:</b>unknown (looked up from first loads LoadCostCurrency.)</ls>\n"
                        + "        <li><b>Customer:</b>unknown (looked up from load.CustomerId)</li>\n"
                        + "        <li><b>Vendor:</b>unknown (looked up from load.HaulierId)</li>\n"
                        + "        <li><b>Item:</b>unknown (ref data based on companyId)</li>\n"
                        + "        </ul>");
    }

    @Test
    public void testGetOrDefaultNested() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVar", "some value");
        VariableContainer variableContainer = new VariableContainerWrapper(variables);

        assertThat(executeExpression("${variables:getOrDefault(var:getOrDefault(variableName, 'myVar'), 'test')}", variableContainer))
                .isEqualTo("some value");
    }

    protected Object executeExpression(String expression, VariableContainer variableContainer) {
        return cmmnEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> cmmnEngineConfiguration.getExpressionManager()
                        .createExpression(expression)
                        .getValue(variableContainer));
    }

}

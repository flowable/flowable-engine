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
package org.flowable.engine.test.el;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.VariableContainerWrapper;
import org.flowable.common.engine.impl.variable.MapDelegateVariableContainer;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonVariableContainerExpressionTest extends PluggableFlowableTestCase {

    @Test
    public void setNestedJsonVariableValueWithVariableContainerWrapper() {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();

        Map<String, Object> variables = new HashMap<>();
        variables.put("muppetshow", objectNode);

        VariableContainerWrapper simpleVariableContainer = new VariableContainerWrapper(variables);
        assertThatJson(executeSetValueExpression("${muppetshow.characters.frog.name}", "Kermit", simpleVariableContainer)
                .getVariable("muppetshow"))
                .isEqualTo("{characters:{frog:{'name':'Kermit'}}}");

        assertThatJson(executeSetValueExpression("${muppetshow.startingYear}", 1976, simpleVariableContainer)
                .getVariable("muppetshow"))
                .isEqualTo("{characters:{frog:{name:'Kermit'}},startingYear:1976}");
    }

    @Test
    public void setNestedJsonVariableValueWithMapDelegateVariableContainer() {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        MapDelegateVariableContainer simpleVariableContainer = new MapDelegateVariableContainer().addTransientVariable("muppetshow", objectNode);
        assertThatJson(executeSetValueExpression("${muppetshow.characters.frog.name}", "Kermit", simpleVariableContainer)
                .getVariable("muppetshow"))
                .isEqualTo("{characters:{frog:{'name':'Kermit'}}}");

        assertThatJson(executeSetValueExpression("${muppetshow.startingYear}", 1976, simpleVariableContainer)
                .getVariable("muppetshow"))
                .isEqualTo("{characters:{frog:{name:'Kermit'}},startingYear:1976}");
    }

    protected VariableContainer executeSetValueExpression(String expression, Object value, VariableContainer variableContainer) {
        processEngineConfiguration.getCommandExecutor()
                .execute(commandContext -> {
                    processEngineConfiguration.getExpressionManager()
                            .createExpression(expression)
                            .setValue(value, variableContainer);
                    return null;
                });
        return variableContainer;
    }

}

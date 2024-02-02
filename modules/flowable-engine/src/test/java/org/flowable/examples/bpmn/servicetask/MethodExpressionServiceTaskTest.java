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
package org.flowable.examples.bpmn.servicetask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Christian Stettler
 */
public class MethodExpressionServiceTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSetServiceResultToProcessVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("okReturningService", new OkReturningService());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setServiceResultToProcessVariables", variables);

        assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo("ok");
    }

    @Test
    @Deployment
    public void testSetServiceResultToProcessVariablesWithSkipExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("okReturningService", new OkReturningService());
        variables.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", false);

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setServiceResultToProcessVariablesWithSkipExpression", variables);

        assertThat(runtimeService.getVariable(pi.getId(), "result")).isEqualTo("ok");

        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("okReturningService", new OkReturningService());
        variables2.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables2.put("skip", true);

        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("setServiceResultToProcessVariablesWithSkipExpression", variables2);

        assertThat(runtimeService.getVariable(pi2.getId(), "result")).isNull();

    }
}

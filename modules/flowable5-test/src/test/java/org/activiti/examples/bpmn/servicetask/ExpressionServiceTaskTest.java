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
package org.activiti.examples.bpmn.servicetask;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * @author Christian Stettler
 */
public class ExpressionServiceTaskTest extends PluggableFlowableTestCase {

    @Deployment
    public void testSetServiceResultToProcessVariables() {
        ProcessInstance pi = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("setServiceResultToProcessVariables")
                .transientVariable("bean", new ValueBean("ok"))
                .start();
        assertEquals("ok", runtimeService.getVariable(pi.getId(), "result"));
    }

    @Deployment
    public void testSetServiceResultToProcessVariablesWithSkipExpression() {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables.put("skip", false);
        ProcessInstance pi = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("setServiceResultToProcessVariablesWithSkipExpression")
                .variables(variables)
                .transientVariable("bean", new ValueBean("ok"))
                .start();
        assertEquals("ok", runtimeService.getVariable(pi.getId(), "result"));

        Map<String, Object> variables2 = new HashMap<String, Object>();
        variables2.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables2.put("skip", true);
        ProcessInstance pi2 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("setServiceResultToProcessVariablesWithSkipExpression")
                .variables(variables2)
                .transientVariable("bean", new ValueBean("ok"))
                .start();
        assertNull(runtimeService.getVariable(pi2.getId(), "result"));

        Map<String, Object> variables3 = new HashMap<String, Object>();
        variables3.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", false);
        ProcessInstance pi3 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("setServiceResultToProcessVariablesWithSkipExpression")
                .variables(variables3)
                .transientVariable("bean", new ValueBean("okBean"))
                .start();
        assertEquals("okBean", runtimeService.getVariable(pi3.getId(), "result"));
    }

    @Deployment
    public void testBackwardsCompatibleExpression() {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("var", "---");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("BackwardsCompatibleExpressionProcess", variables);
        assertEquals("...---...", runtimeService.getVariable(pi.getId(), "result"));
    }
}

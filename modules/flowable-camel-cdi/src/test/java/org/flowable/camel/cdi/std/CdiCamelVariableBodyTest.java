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
package org.flowable.camel.cdi.std;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.flowable.camel.cdi.impl.CdiCamelBehaviorCamelBodyImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Test;

/**
 * Adapted from {@link CamelVariableBodyTest} to test {@link CdiCamelBehaviorCamelBodyImpl}.
 * 
 * @author Zach Visagie
 */
public class CdiCamelVariableBodyTest extends StdCamelCdiFlowableTestCase {

    @Inject
    protected CamelContext camelContext;

    protected MockEndpoint service1;

    @Override
    public void setUp() throws Exception {
    	super.setUp();
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("flowable:HelloCamel:serviceTask1").log(LoggingLevel.INFO, "Received message on service task").to("mock:serviceBehavior");
            }
        });
        service1 = (MockEndpoint) camelContext.getEndpoint("mock:serviceBehavior");
        service1.reset();
    }

    @After
    public void tearDown() throws Exception {
        List<Route> routes = camelContext.getRoutes();
        for (Route r : routes) {
            camelContext.stopRoute(r.getId());
            camelContext.removeRoute(r.getId());
        }
    }

    @Test
    @Deployment(resources = {"process/HelloCamelCdiBody.bpmn20.xml"})
    public void testCamelBody() throws Exception {
        service1.expectedBodiesReceived("hello world");
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("camelBody", "hello world");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HelloCamel", varMap);
        // Ensure that the variable is equal to the expected value.
        assertThat(runtimeService.getVariable(processInstance.getId(), "camelBody")).isEqualTo("hello world");
        service1.assertIsSatisfied();

        Task task = taskService.createTaskQuery().singleResult();

        // Ensure that the name of the task is correct.
        assertThat(task.getName()).isEqualTo("Hello Task");

        // Complete the task.
        taskService.complete(task.getId());
    }

}

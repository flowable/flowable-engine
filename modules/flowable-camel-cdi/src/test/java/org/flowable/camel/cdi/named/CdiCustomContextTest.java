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
package org.flowable.camel.cdi.named;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.flowable.camel.FlowableProducer;
import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.After;
import org.junit.Test;

/**
 * Adapted from {@link CustomContextTest}.
 * 
 * @author Zach Visagie
 */
public class CdiCustomContextTest extends NamedCamelCdiFlowableTestCase {

    @Inject
    @Named("camelContext")
    protected CamelContext camelContext;

    protected MockEndpoint service1;

    protected MockEndpoint service2;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:start").to("flowable:camelProcess");
                from("flowable:camelProcess:serviceTask1").setBody().exchangeProperty("var1").to("mock:service1").setProperty("var2").constant("var2").setBody()
                                .exchangeProperties();
                from("flowable:camelProcess:serviceTask2?copyVariablesToBodyAsMap=true").to("mock:service2");
                from("direct:receive").to("flowable:camelProcess:receive");
            }
        });

        service1 = (MockEndpoint) camelContext.getEndpoint("mock:service1");
        service1.reset();
        service2 = (MockEndpoint) camelContext.getEndpoint("mock:service2");
        service2.reset();
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
    @Deployment(resources = { "process/custom.bpmn20.xml" })
    public void testRunProcess() throws Exception {
        CamelContext ctx = (CamelContext) ProgrammaticBeanLookup.lookup("camelContext");
        ProducerTemplate tpl = ctx.createProducerTemplate();
        service1.expectedBodiesReceived("ala");

        Exchange exchange = ctx.getEndpoint("direct:start").createExchange();
        exchange.getIn().setBody(Collections.singletonMap("var1", "ala"));
        tpl.send("direct:start", exchange);

        String instanceId = (String) exchange.getProperty("PROCESS_ID_PROPERTY");
        List<ProcessInstance> processInstances = processEngine.getRuntimeService().createProcessInstanceQuery().list();
        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        assertThat(processInstance.isEnded()).isFalse();

        tpl.sendBodyAndProperty("direct:receive", null, FlowableProducer.PROCESS_ID_PROPERTY, instanceId);

        // check process ended
        processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
        assertThat(processInstance).isNull();

        service1.assertIsSatisfied();

        @SuppressWarnings("rawtypes")
        Map m = service2.getExchanges().get(0).getIn().getBody(Map.class);
        assertThat(m.get("var1")).isEqualTo("ala");
        assertThat(m.get("var2")).isEqualTo("var2");
    }
}

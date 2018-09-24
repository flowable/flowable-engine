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

package org.flowable.camel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@Tag("camel")
@ContextConfiguration("classpath:generic-camel-flowable-context.xml")
public class SimpleProcessTest extends SpringFlowableTestCase {

    @Autowired
    protected CamelContext camelContext;

    protected MockEndpoint service1;

    protected MockEndpoint service2;

    @BeforeEach
    public void setUp() throws Exception {
        service1 = (MockEndpoint) camelContext.getEndpoint("mock:service1");
        service1.reset();
        service2 = (MockEndpoint) camelContext.getEndpoint("mock:service2");
        service2.reset();
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:start").to("flowable:camelProcess");
                from("flowable:camelProcess:serviceTask1").setBody().exchangeProperty("var1").to("mock:service1").setProperty("var2").constant("var2").setBody().exchangeProperties();
                from("direct:receive").to("flowable:camelProcess:receive");
                from("flowable:camelProcess:serviceTask2?copyVariablesToBodyAsMap=true").to("mock:service2");
            }
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        List<Route> routes = camelContext.getRoutes();
        for (Route r : routes) {
            camelContext.stopRoute(r.getId());
            camelContext.removeRoute(r.getId());
        }
    }

    @Test
    @Deployment(resources = { "process/example.bpmn20.xml" })
    public void testRunProcess() throws Exception {
        CamelContext ctx = applicationContext.getBean(CamelContext.class);
        ProducerTemplate tpl = ctx.createProducerTemplate();
        service1.expectedBodiesReceived("ala");

        Exchange exchange = ctx.getEndpoint("direct:start").createExchange();
        exchange.getIn().setBody(Collections.singletonMap("var1", "ala"));
        tpl.send("direct:start", exchange);
        String instanceId = (String) exchange.getProperty("PROCESS_ID_PROPERTY");

        tpl.sendBodyAndProperty("direct:receive", null, FlowableProducer.PROCESS_ID_PROPERTY, instanceId);

        assertProcessEnded(instanceId);

        service1.assertIsSatisfied();
        Map<?, ?> m = service2.getExchanges().get(0).getIn().getBody(Map.class);
        assertEquals("ala", m.get("var1"));
        assertEquals("var2", m.get("var2"));
    }

    @Test
    @Deployment(resources = { "process/example.bpmn20.xml" })
    public void testRunProcessByKey() throws Exception {
        CamelContext ctx = applicationContext.getBean(CamelContext.class);
        ProducerTemplate tpl = ctx.createProducerTemplate();
        MockEndpoint me = (MockEndpoint) ctx.getEndpoint("mock:service1");
        me.expectedBodiesReceived("ala");

        tpl.sendBodyAndProperty("direct:start", Collections.singletonMap("var1", "ala"), FlowableProducer.PROCESS_KEY_PROPERTY, "key1");

        String instanceId = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("key1").singleResult().getProcessInstanceId();
        tpl.sendBodyAndProperty("direct:receive", null, FlowableProducer.PROCESS_KEY_PROPERTY, "key1");

        assertProcessEnded(instanceId);

        me.assertIsSatisfied();
    }

}

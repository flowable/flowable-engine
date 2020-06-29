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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@Tag("camel")
@ContextConfiguration("classpath:generic-camel-flowable-context.xml")
public class EmptyProcessTest extends SpringFlowableTestCase {

    @Autowired
    protected CamelContext camelContext;

    @BeforeEach
    public void setUp() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:startEmpty").to("flowable:emptyProcess");
                from("direct:startEmptyWithHeader").setHeader("MyVar", constant("Foo")).to("flowable:emptyProcess?copyVariablesFromHeader=true");
                from("direct:startEmptyBodyAsString").to("flowable:emptyProcess?copyBodyToCamelBodyAsString=true");
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
    @Deployment(resources = { "process/empty.bpmn20.xml" })
    public void testRunProcessWithHeader() throws Exception {
        CamelContext ctx = applicationContext.getBean(CamelContext.class);
        ProducerTemplate tpl = camelContext.createProducerTemplate();
        String body = "body text";
        Exchange exchange = ctx.getEndpoint("direct:startEmptyWithHeader").createExchange();
        exchange.getIn().setBody(body);
        tpl.send("direct:startEmptyWithHeader", exchange);

        String instanceId = (String) exchange.getProperty("PROCESS_ID_PROPERTY");
        assertProcessEnded(instanceId);
        HistoricVariableInstance var = processEngine.getHistoryService().createHistoricVariableInstanceQuery().variableName("camelBody").singleResult();
        assertThat(var).isNotNull();
        assertThat(var.getValue()).isEqualTo(body);
        var = processEngine.getHistoryService().createHistoricVariableInstanceQuery().variableName("MyVar").singleResult();
        assertThat(var).isNotNull();
        assertThat(var.getValue()).isEqualTo("Foo");
    }

    @Test
    @Deployment(resources = { "process/empty.bpmn20.xml" })
    public void testObjectAsVariable() throws Exception {
        CamelContext ctx = applicationContext.getBean(CamelContext.class);
        ProducerTemplate tpl = ctx.createProducerTemplate();
        Object expectedObj = 99L;
        Exchange exchange = ctx.getEndpoint("direct:startEmpty").createExchange();
        exchange.getIn().setBody(expectedObj);
        tpl.send("direct:startEmpty", exchange);
        String instanceId = (String) exchange.getProperty("PROCESS_ID_PROPERTY");
        assertProcessEnded(instanceId);
        HistoricVariableInstance var = processEngine.getHistoryService().createHistoricVariableInstanceQuery().variableName("camelBody").singleResult();
        assertThat(var).isNotNull();
        assertThat(var.getValue()).isEqualTo(expectedObj);
    }

    @Test
    @Deployment(resources = { "process/empty.bpmn20.xml" })
    public void testObjectAsStringVariable() throws Exception {
        CamelContext ctx = applicationContext.getBean(CamelContext.class);
        ProducerTemplate tpl = ctx.createProducerTemplate();
        Object expectedObj = 99L;

        Exchange exchange = ctx.getEndpoint("direct:startEmptyBodyAsString").createExchange();
        exchange.getIn().setBody(expectedObj);
        tpl.send("direct:startEmptyBodyAsString", exchange);

        String instanceId = (String) exchange.getProperty("PROCESS_ID_PROPERTY");

        assertProcessEnded(instanceId);
        HistoricVariableInstance var = processEngine.getHistoryService().createHistoricVariableInstanceQuery().variableName("camelBody").singleResult();
        assertThat(var).isNotNull();
        assertThat(var.getValue()).hasToString(expectedObj.toString());
    }
}

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

import java.util.List;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Adapted from {@link AsyncPingTest}.
 * 
 * @author Zach Visagie
 */
public class CdiAsyncPingTest extends StdCamelCdiFlowableTestCase {

    @Inject
    protected CamelContext camelContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("flowable:asyncPingProcess:serviceAsyncPing").to("seda:continueAsync");
                from("seda:continueAsync").to("flowable:asyncPingProcess:receiveAsyncPing");
            }
        });
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
    @Deployment(resources = { "process/asyncPing.bpmn20.xml" })
    public void testRunProcess() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncPingProcess");

        List<Execution> executionList = runtimeService.createExecutionQuery().list();
        Assert.assertEquals(2, executionList.size());

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        Thread.sleep(1500);

        executionList = runtimeService.createExecutionQuery().list();
        Assert.assertEquals(0, executionList.size());

        Assert.assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
    }

}

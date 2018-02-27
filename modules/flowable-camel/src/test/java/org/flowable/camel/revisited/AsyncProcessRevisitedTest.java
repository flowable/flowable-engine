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

package org.flowable.camel.revisited;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Saeid Mirzaei
 */

@ContextConfiguration("classpath:generic-camel-flowable-context.xml")
public class AsyncProcessRevisitedTest extends SpringFlowableTestCase {

    @Autowired
    protected CamelContext camelContext;

    @Override
    public void setUp() throws Exception {
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("flowable:asyncCamelProcessRevisited:serviceTaskAsync1").to("bean:sleepBean?method=sleep").to("seda:continueAsync1");
                from("seda:continueAsync1").to("flowable:asyncCamelProcessRevisited:receive1");

                from("flowable:asyncCamelProcessRevisited:serviceTaskAsync2").to("bean:sleepBean?method=sleep").to("seda:continueAsync2");
                from("seda:continueAsync2").to("flowable:asyncCamelProcessRevisited:receive2");
            }
        });
    }

    @Deployment(resources = { "process/revisited/async-revisited.bpmn20.xml" })
    public void testRunProcess() throws Exception {
        NotifyBuilder oneExchangeSendToFlowableReceive1 = new NotifyBuilder(camelContext).from("seda:continueAsync1").whenExactlyCompleted(1).create();
        NotifyBuilder oneExchangeSendToFlowableReceive2 = new NotifyBuilder(camelContext).from("seda:continueAsync2").whenExactlyCompleted(1).create();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncCamelProcessRevisited");

        List<Execution> executionList = runtimeService.createExecutionQuery().list();
        assertEquals(3, executionList.size());
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(5000, 200);

        assertTrue(oneExchangeSendToFlowableReceive1.matchesMockWaitTime());
        assertTrue(oneExchangeSendToFlowableReceive2.matchesMockWaitTime());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
    }
}

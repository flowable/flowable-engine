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

package org.flowable.camel.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.flowable.camel.exception.tools.ExceptionServiceMock;
import org.flowable.camel.exception.tools.NoExceptionServiceMock;
import org.flowable.camel.exception.tools.ThrowBpmnExceptionBean;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Saeid Mirzaei
 */
@Tag("camel")
@ContextConfiguration("classpath:generic-camel-flowable-context.xml")
public class CamelExceptionTest extends SpringFlowableTestCase {

    @Autowired
    protected CamelContext camelContext;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected ManagementService managementService;

    @BeforeEach
    public void setUp() throws Exception {
        ExceptionServiceMock.reset();
        NoExceptionServiceMock.reset();

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("flowable:exceptionInRouteSynchron:errorCamelTask").to("log:helloWorld").bean(ThrowBpmnExceptionBean.class);
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

    // check happy path in synchronous camel call
    @Test
    @Deployment(resources = { "org/flowable/camel/exception/bpmnExceptionInRouteSynchronous.bpmn20.xml" })
    public void testHappyPathSynchronous() {
        // Signal ThrowBpmnExceptionBean to throw no exception
        ThrowBpmnExceptionBean.setExceptionType(ThrowBpmnExceptionBean.ExceptionType.NO_EXCEPTION);
        runtimeService.startProcessInstanceByKey("exceptionInRouteSynchron");

        assertThat(ExceptionServiceMock.isCalled()).isFalse();
        assertThat(NoExceptionServiceMock.isCalled()).isTrue();
    }

    // Check Non BPMN error in synchronous camel call
    @Test
    @Deployment(resources = { "org/flowable/camel/exception/bpmnExceptionInRouteSynchronous.bpmn20.xml" })
    public void testNonBpmnExceptionInCamel() {
        // Signal ThrowBpmnExceptionBean to throw a non BPMN Exception
        ThrowBpmnExceptionBean.setExceptionType(ThrowBpmnExceptionBean.ExceptionType.NON_BPMN_EXCEPTION);
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("exceptionInRouteSynchron"))
                .isInstanceOf(FlowableException.class)
                .hasCauseInstanceOf(Exception.class)
                .hasRootCauseMessage("arbitrary non bpmn exception");
        assertThat(ExceptionServiceMock.isCalled()).isFalse();
        assertThat(NoExceptionServiceMock.isCalled()).isFalse();
    }

    // check Bpmn Exception in synchronous camel call
    @Test
    @Deployment(resources = { "org/flowable/camel/exception/bpmnExceptionInRouteSynchronous.bpmn20.xml" })
    public void testBpmnExceptionInCamel() {
        // Signal ThrowBpmnExceptionBean to throw a BPMN Exception
        ThrowBpmnExceptionBean.setExceptionType(ThrowBpmnExceptionBean.ExceptionType.BPMN_EXCEPTION);

        // The exception should be handled by camel. No exception expected.
        assertThatCode(() -> {
            runtimeService.startProcessInstanceByKey("exceptionInRouteSynchron");
        })
                .doesNotThrowAnyException();

        assertThat(ExceptionServiceMock.isCalled()).isTrue();
        assertThat(NoExceptionServiceMock.isCalled()).isFalse();
    }

    // check happy path in asynchronous camel call
    @Test
    @Deployment(resources = { "org/flowable/camel/exception/bpmnExceptionInRouteAsynchronous.bpmn20.xml" })
    public void testHappyPathAsynchronous() {

        // Signal ThrowBpmnExceptionBean to throw no exception
        ThrowBpmnExceptionBean.setExceptionType(ThrowBpmnExceptionBean.ExceptionType.NO_EXCEPTION);
        runtimeService.startProcessInstanceByKey("exceptionInRouteSynchron");

        Job job = managementService.createJobQuery().singleResult();

        managementService.executeJob(job.getId());

        assertThat(JobTestHelper.areJobsAvailable(managementService)).isFalse();
        assertThat(ExceptionServiceMock.isCalled()).isFalse();
        assertThat(NoExceptionServiceMock.isCalled()).isTrue();
    }

    // check non bpmn exception in asynchronous camel call
    @Test
    @Deployment(resources = { "org/flowable/camel/exception/bpmnExceptionInRouteAsynchronous.bpmn20.xml" })
    public void testNonBpmnPathAsynchronous() {

        // Signal ThrowBpmnExceptionBean to throw non bpmn exception
        ThrowBpmnExceptionBean.setExceptionType(ThrowBpmnExceptionBean.ExceptionType.NON_BPMN_EXCEPTION);
        runtimeService.startProcessInstanceByKey("exceptionInRouteSynchron");
        assertThat(JobTestHelper.areJobsAvailable(managementService)).isTrue();

        Job job = managementService.createJobQuery().singleResult();
        String jobId = job.getId();
        assertThatThrownBy(() -> managementService.executeJob(jobId))
                .isInstanceOf(Exception.class);

        // The job is now a timer job, to be retried later
        job = managementService.createTimerJobQuery().singleResult();
        assertThat(job.getExceptionMessage()).isEqualTo("Unhandled exception on camel route");

        assertThat(ExceptionServiceMock.isCalled()).isFalse();
        assertThat(NoExceptionServiceMock.isCalled()).isFalse();
    }
}

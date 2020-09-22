package org.flowable.engine.test.assertions;

import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class JobAssertHasActivityIdTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/JobAssertHasActivityIdTest.bpmn20.xml")
    public void success() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("JobAssert-hasActivityId");
        assertThat(processInstance).isNotNull();
//        assertThat(job()).isNotNull();
//        assertThat(job()).hasActivityId("ServiceTask_1");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/JobAssertHasActivityIdTest.bpmn20.xml")
    public void failure() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("JobAssert-hasActivityId");
        assertThat(processInstance).isNotNull();
//        assertThat(job()).isNotNull();
//        assertThatThrownBy(() -> assertThat(job()).hasActivityId("otherDeploymentId"))
//                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/JobAssertHasActivityIdTest.bpmn20.xml")
    public void testHasActivityId_Error_Null() {
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("JobAssert-hasActivityId");
        assertThat(processInstance).isNotNull();
//        assertThat(job()).isNotNull();
//        assertThatThrownBy(() -> assertThat(job()).hasActivityId(null))
//                .isInstanceOf(AssertionError.class);
    }
}

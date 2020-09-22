package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.assertThat;
import static org.flowable.engine.assertions.BpmnAwareTests.jobQuery;
import static org.flowable.engine.assertions.BpmnAwareTests.runtimeService;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class JobAssertHasIdTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/JobAssertHasIdTest.bpmn20.xml")
    public void success() {
        runtimeService().startProcessInstanceByKey("JobAssert-hasId");
        assertThat(jobQuery().singleResult()).isNotNull();
        assertThat(jobQuery().singleResult()).hasId(jobQuery().singleResult().getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/JobAssertHasIdTest.bpmn20.xml")
    public void failure() {
        runtimeService().startProcessInstanceByKey("JobAssert-hasId");
        assertThat(jobQuery().singleResult()).isNotNull();
        assertThatThrownBy(() -> assertThat(jobQuery().singleResult()).hasId("otherId"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/assertions/JobAssertHasIdTest.bpmn20.xml")
    public void errorNull() {
        runtimeService().startProcessInstanceByKey("JobAssert-hasId");
        assertThat(jobQuery().singleResult()).isNotNull();
        assertThatThrownBy(() -> assertThat(jobQuery().singleResult()).hasId(null))
                .isInstanceOf(AssertionError.class);
    }
}

package org.flowable.engine.test.bpmn.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

class NoneStartEventActivityBehaviorTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void asyncStartEvent() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("asyncNoneStartEvent");

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count()).isEqualTo(1l);
        Job job = managementService.createJobQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).onlyChildExecutions().singleResult().getActivityId()).isEqualTo("theStart");

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 1000, 200);
        assertProcessEnded(pi.getProcessInstanceId());
    }

}
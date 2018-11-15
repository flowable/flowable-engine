package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.test.AbstractTestCase;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.Test;

public class RecordRuntimeActivitiesTest extends AbstractTestCase {

    @Test
    public void mandatoryRecordRuntimeActivities() {
        ProcessEngineConfiguration processEngineConfiguration = new StandaloneInMemProcessEngineConfiguration()
            .setJdbcUrl("jdbc:h2:mem:RecordRuntimeActivitiesTest");
        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

        Deployment deployment = processEngine.getRepositoryService().createDeployment()
            .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
            .deploy();

        try {
            processEngine.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");

            assertThat(processEngine.getRuntimeService().createActivityInstanceQuery().count()).isGreaterThan(0L);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }

    }

}

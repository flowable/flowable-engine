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
package org.flowable.engine.data.inmemory.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Calendar;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.data.inmemory.MemoryDataManagerFlowableTestCase;
import org.flowable.engine.data.inmemory.impl.job.MemoryDeadLetterJobDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryDeadLetterJobDataManagerTest extends MemoryDataManagerFlowableTestCase {

    @Test
    public void testFindDeadLetterJobs() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/timerExecution.bpmn20.xml").deploy();

        MemoryDeadLetterJobDataManager jobManager = getDeadLetterJobDataManager();

        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("timerExecution");
            fireTimer(instance);
            assertThat(processEngine.getManagementService().createDeadLetterJobQuery().processInstanceId(instance.getProcessInstanceId()).list()).hasSize(1);
            assertThat(jobManager.findJobsByProcessInstanceId(instance.getProcessInstanceId())).hasSize(1);

        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryDeadLetterJobs() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/timerExecution.bpmn20.xml").deploy();

        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("timerExecution");
            fireTimer(instance);
            DeadLetterJobQueryImpl query = Mockito.spy(query());
            assertThat(query.processInstanceId(instance.getProcessInstanceId()).list()).hasSize(1);
            assertQueryMethods(DeadLetterJobQueryImpl.class, query);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    private void fireTimer(ProcessInstance instance) {
        List<Job> jobs = processEngine.getManagementService().createTimerJobQuery().processInstanceId(instance.getId()).list();
        assertThat(jobs).hasSize(1);
        Job job = jobs.get(0);
        getConfig().getManagementService().setTimerJobRetries(job.getId(), 1);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        getConfig().getClock().setCurrentTime(tomorrow.getTime());

        Job executableJob = getConfig().getManagementService().moveTimerToExecutableJob(job.getId());
        assertThatThrownBy(() -> getConfig().getManagementService().executeJob(executableJob.getId())).isInstanceOf(FlowableException.class);
    }

    private DeadLetterJobQueryImpl query() {
        return (DeadLetterJobQueryImpl) getConfig().getJobServiceConfiguration().getJobService().createDeadLetterJobQuery();
    }

}

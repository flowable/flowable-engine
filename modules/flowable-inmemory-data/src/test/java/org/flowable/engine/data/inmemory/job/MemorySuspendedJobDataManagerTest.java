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

import org.flowable.engine.data.inmemory.MemoryDataManagerFlowableTestCase;
import org.flowable.engine.data.inmemory.impl.job.MemorySuspendedJobDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.impl.SuspendedJobQueryImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemorySuspendedJobDataManagerTest extends MemoryDataManagerFlowableTestCase {

    @Test
    public void testFindSuspendedJobs() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/timerExecution.bpmn20.xml").deploy();

        MemorySuspendedJobDataManager jobManager = getSuspendedJobDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("timerExecution");
            processEngine.getRuntimeService().suspendProcessInstanceById(instance.getId());
            assertThat(jobManager.findJobsByProcessInstanceId(instance.getProcessInstanceId())).hasSize(1);
            processEngine.getRuntimeService().activateProcessInstanceById(instance.getId());
            assertThat(jobManager.findJobsByProcessInstanceId(instance.getProcessInstanceId())).isEmpty();
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQuerySuspendedJobs() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/timerExecution.bpmn20.xml").deploy();

        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("timerExecution");
            processEngine.getRuntimeService().suspendProcessInstanceById(instance.getId());

            SuspendedJobQueryImpl query = Mockito.spy(query());
            assertThat(query.processInstanceId(instance.getProcessInstanceId()).list()).hasSize(1);
            assertQueryMethods(SuspendedJobQueryImpl.class, query,
                            // not used in query
                            "getNow", "getLockOwner", "isExecutable");
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    private SuspendedJobQueryImpl query() {
        return (SuspendedJobQueryImpl) getConfig().getManagementService().createSuspendedJobQuery();
    }

}

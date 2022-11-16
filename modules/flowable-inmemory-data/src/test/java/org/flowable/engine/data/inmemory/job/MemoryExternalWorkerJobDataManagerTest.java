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
import org.flowable.engine.data.inmemory.impl.job.MemoryExternalWorkerJobDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryExternalWorkerJobDataManagerTest extends MemoryDataManagerFlowableTestCase {

    @Test
    public void testFindExternalWorkerJobs() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/externalWorkerExecution.bpmn20.xml").deploy();

        MemoryExternalWorkerJobDataManager jobManager = getExternalWorkerJobDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("externalWorkerExecution");
            assertThat(jobManager.findJobsByProcessInstanceId(instance.getProcessInstanceId())).hasSize(2);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryExternalWorkerJobs() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/externalWorkerExecution.bpmn20.xml").deploy();

        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("externalWorkerExecution");
            ExternalWorkerJobQueryImpl query = Mockito.spy(query());

            assertThat(query.processInstanceId(instance.getProcessInstanceId()).list()).hasSize(2);
            assertQueryMethods(ExternalWorkerJobQueryImpl.class, query,
                            // not used in query
                            "getNow", "getLockOwner");
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    private ExternalWorkerJobQueryImpl query() {
        return (ExternalWorkerJobQueryImpl) getConfig().getManagementService().createExternalWorkerJobQuery();
    }

}
